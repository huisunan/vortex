package com.vortex.engine;

import com.vortex.flow.domain.EdgeDefinition;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.domain.NodeDefinition;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flow 执行引擎
 * 
 * 负责：
 * 1. 加载 FlowDefinition
 * 2. 对节点进行拓扑排序
 * 3. 调用 NodeExecutor 执行每个节点
 * 4. 传递 ExecutionContext
 * 5. 持久化执行状态到 trace_record
 */
@Component
public class FlowExecutionEngine {

    private final Map<String, NodeExecutor> nodeExecutors;
    private final ExpressionParser expressionParser;
    private final ExecutionTracer tracer;

    public FlowExecutionEngine(List<NodeExecutor> nodeExecutors, ExecutionTracer tracer) {
        this.nodeExecutors = new HashMap<>();
        for (NodeExecutor executor : nodeExecutors) {
            this.nodeExecutors.put(executor.nodeType(), executor);
        }
        this.expressionParser = new SpelExpressionParser();
        this.tracer = tracer;
    }

    /**
     * 执行流程
     * 
     * @param flow 流程定义
     * @param context 执行上下文
     */
    public void execute(FlowDefinition flow, ExecutionContext context) {
        // 1. 拓扑排序节点
        List<String> sortedNodeIds = topologicalSort(flow.getNodes(), flow.getEdges());
        
        // 2. 按顺序执行每个节点
        for (String nodeId : sortedNodeIds) {
            NodeDefinition node = findNodeById(flow.getNodes(), nodeId);
            if (node == null) {
                throw new IllegalStateException("Node not found: " + nodeId);
            }
            
            executeNode(node, context);
        }
    }

    /**
     * 对节点进行拓扑排序
     * 
     * 使用 Kahn 算法实现拓扑排序
     * 
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 排序后的节点 ID 列表
     * @throws IllegalStateException 如果存在循环依赖
     */
    private List<String> topologicalSort(List<NodeDefinition> nodes, List<EdgeDefinition> edges) {
        // 构建入度表和邻接表
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();
        
        // 初始化
        for (NodeDefinition node : nodes) {
            inDegree.put(node.getNodeId(), 0);
            adjacencyList.put(node.getNodeId(), new ArrayList<>());
        }
        
        // 计算入度和构建邻接表
        for (EdgeDefinition edge : edges) {
            inDegree.put(edge.getToNodeId(), inDegree.get(edge.getToNodeId()) + 1);
            adjacencyList.get(edge.getFromNodeId()).add(edge.getToNodeId());
        }
        
        // Kahn 算法
        Queue<String> queue = new LinkedList<>();
        for (String nodeId : inDegree.keySet()) {
            if (inDegree.get(nodeId) == 0) {
                queue.offer(nodeId);
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            for (String neighbor : adjacencyList.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // 检查是否存在循环依赖
        if (result.size() != nodes.size()) {
            throw new IllegalStateException("Circular dependency detected in flow");
        }
        
        return result;
    }

    /**
     * 执行单个节点
     * 
     * @param node 节点定义
     * @param context 执行上下文
     */
    private void executeNode(NodeDefinition node, ExecutionContext context) {
        NodeExecutor executor = nodeExecutors.get(node.getNodeType());
        if (executor == null) {
            throw new IllegalStateException("No executor found for node type: " + node.getNodeType());
        }
        
        // 记录执行开始
        tracer.recordStart(context.getRunId(), getFirstBusinessId(context), node.getNodeId());
        
        try {
            long startTime = System.currentTimeMillis();
            executor.execute(context, node.getConfig());
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录执行成功
            tracer.recordSuccess(
                context.getRunId(), 
                getFirstBusinessId(context), 
                node.getNodeId(),
                "Executed in " + duration + "ms"
            );
        } catch (Exception e) {
            // 记录执行失败
            tracer.recordFailure(
                context.getRunId(), 
                getFirstBusinessId(context), 
                node.getNodeId(),
                e.getMessage()
            );
            throw e;
        }
    }

    /**
     * 获取第一个业务 ID（用于追踪）
     * 
     * @param context 执行上下文
     * @return 第一个业务 ID，如果没有则返回 runId
     */
    private String getFirstBusinessId(ExecutionContext context) {
        if (context.getBusinessIds() != null && !context.getBusinessIds().isEmpty()) {
            return context.getBusinessIds().get(0);
        }
        return context.getRunId();
    }

    /**
     * 根据 ID 查找节点
     * 
     * @param nodes 节点列表
     * @param nodeId 节点 ID
     * @return 找到的节点，未找到返回 null
     */
    private NodeDefinition findNodeById(List<NodeDefinition> nodes, String nodeId) {
        for (NodeDefinition node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 使用 Spring EL 评估表达式
     * 
     * @param expression SpEL 表达式
     * @param context 执行上下文
     * @param record 当前记录
     * @return 评估结果
     */
    public Object evaluateExpression(String expression, ExecutionContext context, Map<String, Object> record) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("record", record);
        evalContext.setVariable("context", context);
        
        // 将 record 的字段也暴露为变量
        if (record != null) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                evalContext.setVariable(entry.getKey(), entry.getValue());
            }
        }
        
        Expression expr = expressionParser.parseExpression(expression);
        return expr.getValue(evalContext);
    }
}
