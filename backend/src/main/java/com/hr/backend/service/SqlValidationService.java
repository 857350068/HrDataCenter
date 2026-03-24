package com.hr.backend.service;

import java.util.Map;

/**
 * SQL安全校验服务
 */
public interface SqlValidationService {

    /**
     * 校验SQL语句安全性
     * @param sql SQL语句
     * @return 校验结果
     */
    Map<String, Object> validateSql(String sql);

    /**
     * 清理SQL语句，移除危险操作
     * @param sql 原始SQL
     * @return 清理后的SQL
     */
    String sanitizeSql(String sql);

    /**
     * 检查SQL是否包含危险关键词
     * @param sql SQL语句
     * @return 是否包含危险关键词
     */
    boolean containsDangerousKeywords(String sql);
}
