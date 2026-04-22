package com.lingxi.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent调用上下文 - 使用ThreadLocal存储当前请求的检索结果
 */
public class AgentContext {

    private static final ThreadLocal<List<Map<String, Object>>> SOURCES_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 初始化上下文
     */
    public static void init() {
        SOURCES_HOLDER.set(new ArrayList<>());
    }

    /**
     * 初始化上下文并设置部门ID
     */
    public static void init(Long deptId) {
        SOURCES_HOLDER.set(new ArrayList<>());
        DEPT_ID_HOLDER.set(deptId);
    }

    /**
     * 添加检索来源
     */
    public static void addSource(Map<String, Object> source) {
        List<Map<String, Object>> sources = SOURCES_HOLDER.get();
        if (sources != null) {
            sources.add(source);
        }
    }

    /**
     * 获取所有检索来源
     */
    public static List<Map<String, Object>> getSources() {
        List<Map<String, Object>> sources = SOURCES_HOLDER.get();
        return sources != null ? sources : new ArrayList<>();
    }

    /**
     * 获取当前部门ID
     */
    public static Long getDeptId() {
        return DEPT_ID_HOLDER.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        SOURCES_HOLDER.remove();
        DEPT_ID_HOLDER.remove();
    }
}
