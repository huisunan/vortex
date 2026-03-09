package com.vortex.node.filter;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.NodeExecutor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 过滤节点执行器 - 使用 Spring EL (SpEL) 表达式引擎进行数据过滤
 * 
 * 支持表达式：#record.price > 100 and #record.status == 'active'
 * 支持操作符：==, !=, >, >=, <, <=, and, or, not
 * 通过 #record.fieldName 访问字段
 */
@Component
public class FilterNodeExecutor implements NodeExecutor {

    private final ExpressionParser expressionParser;

    public FilterNodeExecutor() {
        this.expressionParser = new SpelExpressionParser();
    }

    @Override
    public String nodeType() {
        return "filter";
    }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        String expression = (String) config.get("expression");
        
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression is required for filter node");
        }

        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        
        for (Map<String, Object> record : context.getRecords()) {
            try {
                Object result = evaluateExpression(expression, record);
                
                if (result instanceof Boolean && (Boolean) result) {
                    filteredRecords.add(record);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to evaluate filter expression: " + expression, e);
            }
        }
        
        context.setRecords(filteredRecords);
    }

    /**
     * 使用 Spring EL 评估表达式
     * 
     * @param expression SpEL 表达式（使用 #record.fieldName 或 #record['fieldName'] 语法）
     * @param record 当前记录
     * @return 评估结果
     */
    private Object evaluateExpression(String expression, Map<String, Object> record) {
        // 使用 SimpleEvaluationContext 支持 Map 数据绑定
        var context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        context.setVariable("record", record);
        
        Expression expr = expressionParser.parseExpression(expression);
        return expr.getValue(context);
    }
}
