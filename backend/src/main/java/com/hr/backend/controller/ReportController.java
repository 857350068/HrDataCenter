package com.hr.backend.controller;

import com.hr.backend.common.Response;
import com.hr.backend.model.entity.ReportTemplate;
import com.hr.backend.service.ReportTemplateService;
import com.hr.backend.service.SqlValidationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 报表管理控制器
 * <p>
 * 提供报表模板的增删改查和SQL预览功能
 * 所有SQL查询都会经过安全校验，防止SQL注入攻击
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Resource
    private ReportTemplateService reportTemplateService;

    @Resource
    private SqlValidationService sqlValidationService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取报表模板列表
     *
     * @return 报表模板列表
     */
    @GetMapping("/list")
    public Response<List<ReportTemplate>> list() {
        try {
            return Response.success(reportTemplateService.list());
        } catch (Exception e) {
            return Response.error(500, "获取报表列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表模板
     *
     * @param id 报表模板ID
     * @return 报表模板详情
     */
    @GetMapping("/{id}")
    public Response<ReportTemplate> getById(@PathVariable Long id) {
        try {
            ReportTemplate template = reportTemplateService.getById(id);
            if (template == null) {
                return Response.error(404, "报表模板不存在");
            }
            return Response.success(template);
        } catch (Exception e) {
            return Response.error(500, "获取报表详情失败：" + e.getMessage());
        }
    }

    /**
     * 新增报表模板
     *
     * @param template 报表模板
     * @return 操作结果
     */
    @PostMapping
    public Response<Void> add(@RequestBody ReportTemplate template) {
        try {
            // 校验SQL安全性
            if (template.getQuerySql() != null && !template.getQuerySql().trim().isEmpty()) {
                Map<String, Object> validationResult = sqlValidationService.validateSql(template.getQuerySql());
                if (!(Boolean) validationResult.get("valid")) {
                    return Response.error(400, "SQL校验失败：" + validationResult.get("message"));
                }
            }
            reportTemplateService.save(template);
            return Response.success();
        } catch (Exception e) {
            return Response.error(500, "新增报表失败：" + e.getMessage());
        }
    }

    /**
     * 更新报表模板
     *
     * @param id       报表模板ID
     * @param template 报表模板
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Response<Void> update(@PathVariable Long id, @RequestBody ReportTemplate template) {
        try {
            // 检查报表是否存在
            ReportTemplate existing = reportTemplateService.getById(id);
            if (existing == null) {
                return Response.error(404, "报表模板不存在");
            }

            // 校验SQL安全性
            if (template.getQuerySql() != null && !template.getQuerySql().trim().isEmpty()) {
                Map<String, Object> validationResult = sqlValidationService.validateSql(template.getQuerySql());
                if (!(Boolean) validationResult.get("valid")) {
                    return Response.error(400, "SQL校验失败：" + validationResult.get("message"));
                }
            }
            template.setId(id);
            reportTemplateService.updateById(template);
            return Response.success();
        } catch (Exception e) {
            return Response.error(500, "更新报表失败：" + e.getMessage());
        }
    }

    /**
     * 删除报表模板
     *
     * @param id 报表模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        try {
            // 检查报表是否存在
            ReportTemplate existing = reportTemplateService.getById(id);
            if (existing == null) {
                return Response.error(404, "报表模板不存在");
            }
            reportTemplateService.removeById(id);
            return Response.success();
        } catch (Exception e) {
            return Response.error(500, "删除报表失败：" + e.getMessage());
        }
    }

    /**
     * 预览SQL查询结果
     * <p>
     * 执行用户输入的SQL并返回前10条结果，用于预览查询效果
     * 所有SQL都会经过安全校验，防止SQL注入攻击
     *
     * @param request 请求参数，包含sql字段
     * @return 查询结果，包含data、columns、count
     */
    @PostMapping("/preview")
    public Response<Map<String, Object>> preview(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");

            // 参数校验
            if (sql == null || sql.trim().isEmpty()) {
                return Response.error(400, "SQL语句不能为空");
            }

            // 校验SQL安全性
            Map<String, Object> validationResult = sqlValidationService.validateSql(sql);
            if (!(Boolean) validationResult.get("valid")) {
                return Response.error(400, "SQL校验失败：" + validationResult.get("message"));
            }

            // 清理SQL
            String sanitizedSql = sqlValidationService.sanitizeSql(sql);

            // 执行查询，限制返回10条记录
            String limitedSql = sanitizedSql.replaceAll("(?i)LIMIT\\s+\\d+", "LIMIT 10");
            if (!limitedSql.matches("(?i).*LIMIT.*")) {
                limitedSql += " LIMIT 10";
            }

            // 执行查询
            List<Map<String, Object>> data = jdbcTemplate.queryForList(limitedSql);

            // 获取列名
            Set<String> columns = new LinkedHashSet<>();
            if (!data.isEmpty()) {
                columns.addAll(data.get(0).keySet());
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("columns", columns);
            result.put("count", data.size());

            // 检查是否有警告信息
            if (validationResult.containsKey("warning")) {
                result.put("warning", validationResult.get("warning"));
            }

            return Response.success(result);
        } catch (Exception e) {
            return Response.error(500, "SQL执行失败：" + e.getMessage());
        }
    }
}
