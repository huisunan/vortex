package com.vortex.node.transform;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.NodeExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 转换节点执行器 - 支持 JavaScript 脚本转换 (使用 GraalVM)
 * 
 * 脚本中可通过 record 变量访问当前记录，返回修改后的记录
 * 示例：record.label = record.status == 1 ? '正常' : '禁用'; return record;
 */
@Component
public class TransformNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "transform";
    }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        String script = (String) config.get("script");
        
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script is required for transform node");
        }

        List<Map<String, Object>> transformedRecords = context.getRecords().stream()
            .map(record -> transformRecord(record, script))
            .collect(Collectors.toList());
        
        context.setRecords(transformedRecords);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> transformRecord(Map<String, Object> record, String script) {
        try (Context graalContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> true)
                .build()) {
            
            // 将 Java Map 包装为 GraalVM Value
            Value jsRecord = Value.asValue(record);
            
            // 设置全局变量 record
            graalContext.getBindings("js").putMember("record", jsRecord);
            
            // 执行脚本
            graalContext.eval("js", script);
            
            // 从 Value 读取结果回 Map
            Map<String, Object> resultMap = new HashMap<>();
            if (jsRecord.hasMembers()) {
                for (String key : jsRecord.getMemberKeys()) {
                    Value member = jsRecord.getMember(key);
                    if (member != null && !member.isNull()) {
                        resultMap.put(key, member.as(Object.class));
                    }
                }
            }
            
            return resultMap;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute transform script: " + script, e);
        }
    }
}
