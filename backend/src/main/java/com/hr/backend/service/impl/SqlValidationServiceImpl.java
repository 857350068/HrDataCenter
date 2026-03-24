package com.hr.backend.service.impl;

import com.hr.backend.service.SqlValidationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL安全校验服务实现
 */
@Service
public class SqlValidationServiceImpl implements SqlValidationService {

    // 危险关键词列表（不允许的操作）
    private static final String[] DANGEROUS_KEYWORDS = {
        "DROP", "DELETE", "UPDATE", "INSERT", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE",
        "EXEC", "EXECUTE", "CALL", "DECLARE", "CURSOR", "UNION ALL", "INTO OUTFILE", "LOAD_FILE"
    };

    // 必须包含的关键词（只允许SELECT查询）
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);

    // 允许的表模式（白名单）
    private static final Pattern ALLOWED_TABLE_PATTERN = Pattern.compile(
        "\\b(employee_profile|hr_department|hr_data_category|warning_rule|report_template|sys_user|data_sync_log)\\b",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Map<String, Object> validateSql(String sql) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("message", "");

        if (sql == null || sql.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "SQL语句不能为空");
            return result;
        }

        // 检查是否为SELECT语句
        if (!SELECT_PATTERN.matcher(sql).find()) {
            result.put("valid", false);
            result.put("message", "只允许SELECT查询语句");
            return result;
        }

        // 检查危险关键词
        if (containsDangerousKeywords(sql)) {
            result.put("valid", false);
            result.put("message", "SQL语句包含危险操作，不允许使用DROP、DELETE、UPDATE、INSERT等操作");
            return result;
        }

        // 检查表名白名单
        if (!ALLOWED_TABLE_PATTERN.matcher(sql).find()) {
            result.put("valid", false);
            result.put("message", "SQL语句只能查询系统允许的表：employee_profile、hr_department、hr_data_category、warning_rule、report_template、sys_user、data_sync_log");
            return result;
        }

        // 检查是否包含注释（可能包含恶意代码）
        if (sql.contains("--") || sql.contains("/*") || sql.contains("*/")) {
            result.put("valid", false);
            result.put("message", "SQL语句不允许包含注释");
            return result;
        }

        // 检查是否包含分号（防止多语句执行）
        if (sql.contains(";")) {
            result.put("valid", false);
            result.put("message", "SQL语句不允许包含分号");
            return result;
        }

        // 检查是否包含系统函数
        if (sql.matches("(?i).*(benchmark|sleep|waitfor|delay|pg_sleep).*")) {
            result.put("valid", false);
            result.put("message", "SQL语句包含危险系统函数");
            return result;
        }

        // 检查LIMIT限制
        if (!sql.matches("(?i).*LIMIT\\s+\\d+.*")) {
            result.put("warning", "建议添加LIMIT限制返回结果数量");
        } else {
            // 提取LIMIT值并检查
            Pattern limitPattern = Pattern.compile("(?i)LIMIT\\s+(\\d+)");
            java.util.regex.Matcher matcher = limitPattern.matcher(sql);
            if (matcher.find()) {
                int limit = Integer.parseInt(matcher.group(1));
                if (limit > 10000) {
                    result.put("warning", "LIMIT值建议不超过10000");
                }
            }
        }

        return result;
    }

    @Override
    public String sanitizeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        // 移除危险关键词（虽然validate已经检查，但作为双重保险）
        String sanitized = sql;

        // 移除注释
        sanitized = sanitized.replaceAll("--.*", "");
        sanitized = sanitized.replaceAll("/\\*.*?\\*/", "");

        // 移除多余的分号
        sanitized = sanitized.replaceAll(";+$", "");

        // 移除多余的空格
        sanitized = sanitized.trim();

        return sanitized;
    }

    @Override
    public boolean containsDangerousKeywords(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String upperSql = sql.toUpperCase();

        for (String keyword : DANGEROUS_KEYWORDS) {
            // 使用单词边界匹配，避免误判
            if (upperSql.matches(".*\\b" + keyword + "\\b.*")) {
                return true;
            }
        }

        return false;
    }
}
