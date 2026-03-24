<template>
  <div class="template-selector">
    <h3>选择预设报表模板</h3>
    <el-input
      v-model="searchKeyword"
      placeholder="搜索模板..."
      prefix-icon="Search"
      style="margin-bottom: 20px"
      clearable
    />
    <div class="template-grid">
      <div
        v-for="template in filteredTemplates"
        :key="template.id"
        class="template-card"
        @click="selectTemplate(template)"
      >
        <div class="template-icon">
          <el-icon :size="32">
            <component :is="template.icon" />
          </el-icon>
        </div>
        <div class="template-info">
          <h4>{{ template.name }}</h4>
          <p class="template-desc">{{ template.description }}</p>
          <el-tag size="small" type="info">{{ template.category }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User, DataAnalysis, Money, TrendCharts, Warning, Document,
  Calendar, PieChart, Finished
} from '@element-plus/icons-vue'

const emit = defineEmits(['select'])

const searchKeyword = ref('')

// 预设查询模板
const templates = ref([
  {
    id: 1,
    name: '员工基本信息表',
    description: '查询所有员工的基本信息，包括姓名、部门、职位等',
    category: '人员报表',
    icon: User,
    sql: `SELECT
  e.id,
  e.employee_no AS 员工编号,
  e.name AS 姓名,
  e.gender AS 性别,
  e.age AS 年龄,
  e.dept_name AS 部门,
  e.position AS 职位,
  e.level AS 职级
FROM employee_profile e
WHERE e.is_deleted = 0
ORDER BY e.id
LIMIT 1000`
  },
  {
    id: 2,
    name: '部门绩效对比表',
    description: '各部门的平均绩效得分对比分析',
    category: '绩效报表',
    icon: DataAnalysis,
    sql: `SELECT
  dept_id AS 部门ID,
  dept_name AS 部门,
  AVG(CASE WHEN category_id = 1 THEN value END) AS 组织效能,
  AVG(CASE WHEN category_id = 2 THEN value END) AS 人才建设,
  AVG(CASE WHEN category_id = 3 THEN value END) AS 薪酬福利,
  AVG(CASE WHEN category_id = 4 THEN value END) AS 绩效管理
FROM employee_profile
WHERE is_deleted = 0
GROUP BY dept_id, dept_name
ORDER BY 组织效能 DESC
LIMIT 1000`
  },
  {
    id: 3,
    name: '薪酬成本分析表',
    description: '各部门薪酬成本统计和对比分析',
    category: '薪酬报表',
    icon: Money,
    sql: `SELECT
  dept_id AS 部门ID,
  dept_name AS 部门,
  COUNT(DISTINCT employee_no) AS 人数,
  SUM(CASE WHEN category_id = 3 THEN value ELSE 0 END) AS 薪酬总额,
  AVG(CASE WHEN category_id = 3 THEN value END) AS 人均薪酬
FROM employee_profile
WHERE is_deleted = 0
GROUP BY dept_id, dept_name
ORDER BY 薪酬总额 DESC
LIMIT 1000`
  },
  {
    id: 4,
    name: '员工流失分析表',
    description: '员工流失率统计和流失原因分析',
    category: '流失报表',
    icon: TrendCharts,
    sql: `SELECT
  period AS 月份,
  dept_name AS 部门,
  COUNT(DISTINCT employee_no) AS 总人数,
  SUM(CASE WHEN category_id = 5 AND value < 60 THEN 1 ELSE 0 END) AS 高风险人数,
  ROUND(SUM(CASE WHEN category_id = 5 AND value < 60 THEN 1 ELSE 0 END) * 100.0 / COUNT(DISTINCT employee_no), 2) AS 流失率
FROM employee_profile
WHERE is_deleted = 0
GROUP BY period, dept_name
ORDER BY period DESC, 流失率 DESC
LIMIT 1000`
  },
  {
    id: 5,
    name: '人才梯队建设表',
    description: '各部门人才梯队建设情况分析',
    category: '人才报表',
    icon: Finished,
    sql: `SELECT
  dept_name AS 部门,
  COUNT(DISTINCT employee_no) AS 总人数,
  SUM(CASE WHEN category_id = 2 AND value >= 80 THEN 1 ELSE 0 END) AS 高潜人才,
  SUM(CASE WHEN category_id = 2 AND value >= 60 AND value < 80 THEN 1 ELSE 0 END) AS 中坚力量,
  SUM(CASE WHEN category_id = 2 AND value < 60 THEN 1 ELSE 0 END) AS 待提升
FROM employee_profile
WHERE is_deleted = 0
GROUP BY dept_name
ORDER BY 高潜人才 DESC
LIMIT 1000`
  },
  {
    id: 6,
    name: '培训效果评估表',
    description: '培训效果和员工技能提升情况分析',
    category: '培训报表',
    icon: Document,
    sql: `SELECT
  period AS 月份,
  dept_name AS 部门,
  AVG(CASE WHEN category_id = 6 THEN value END) AS 培训效果,
  COUNT(DISTINCT employee_no) AS 参训人数
FROM employee_profile
WHERE is_deleted = 0 AND category_id = 6
GROUP BY period, dept_name
ORDER BY period DESC, 培训效果 DESC
LIMIT 1000`
  },
  {
    id: 7,
    name: '人力成本优化表',
    description: '人力成本结构和优化建议分析',
    category: '成本报表',
    icon: PieChart,
    sql: `SELECT
  dept_name AS 部门,
  COUNT(DISTINCT employee_no) AS 人数,
  SUM(CASE WHEN category_id = 3 THEN value ELSE 0 END) AS 薪酬成本,
  SUM(CASE WHEN category_id = 7 THEN value ELSE 0 END) AS 管理成本,
  SUM(CASE WHEN category_id = 3 THEN value ELSE 0 END) + SUM(CASE WHEN category_id = 7 THEN value ELSE 0 END) AS 总成本,
  ROUND((SUM(CASE WHEN category_id = 3 THEN value ELSE 0 END) + SUM(CASE WHEN category_id = 7 THEN value ELSE 0 END)) / COUNT(DISTINCT employee_no), 2) AS 人均成本
FROM employee_profile
WHERE is_deleted = 0
GROUP BY dept_name
ORDER BY 总成本 DESC
LIMIT 1000`
  },
  {
    id: 8,
    name: '人才发展预测表',
    description: '基于历史数据的人才发展预测分析',
    category: '发展报表',
    icon: Calendar,
    sql: `SELECT
  period AS 月份,
  dept_name AS 部门,
  AVG(CASE WHEN category_id = 8 THEN value END) AS 发展潜力,
  AVG(CASE WHEN category_id = 1 THEN value END) AS 当前绩效,
  ROUND(AVG(CASE WHEN category_id = 8 THEN value END) - AVG(CASE WHEN category_id = 1 THEN value END), 2) AS 提升空间
FROM employee_profile
WHERE is_deleted = 0
GROUP BY period, dept_name
ORDER BY period DESC, 发展潜力 DESC
LIMIT 1000`
  },
  {
    id: 9,
    name: '预警规则统计表',
    description: '预警规则配置和触发情况统计',
    category: '效能报表',
    icon: Warning,
    sql: `SELECT
  r.id AS 规则ID,
  r.rule_name AS 规则名称,
  c.category_name AS 分类,
  r.threshold_type AS 阈值类型,
  r.threshold_value AS 阈值,
  CASE WHEN r.enabled = 1 THEN '启用' ELSE '禁用' END AS 状态
FROM warning_rule r
LEFT JOIN hr_data_category c ON r.category_id = c.id
ORDER BY r.id
LIMIT 1000`
  },
  {
    id: 10,
    name: '综合效能分析表',
    description: '多维度综合效能分析报表',
    category: '综合报表',
    icon: DataAnalysis,
    sql: `SELECT
  dept_name AS 部门,
  COUNT(DISTINCT employee_no) AS 人数,
  AVG(CASE WHEN category_id = 1 THEN value END) AS 组织效能,
  AVG(CASE WHEN category_id = 2 THEN value END) AS 人才建设,
  AVG(CASE WHEN category_id = 3 THEN value END) AS 薪酬福利,
  AVG(CASE WHEN category_id = 4 THEN value END) AS 绩效管理,
  AVG(CASE WHEN category_id = 5 THEN value END) AS 流失风险,
  AVG(CASE WHEN category_id = 6 THEN value END) AS 培训效果
FROM employee_profile
WHERE is_deleted = 0
GROUP BY dept_name
ORDER BY 组织效能 DESC
LIMIT 1000`
  }
])

// 过滤模板
const filteredTemplates = computed(() => {
  if (!searchKeyword.value) {
    return templates.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return templates.value.filter(t =>
    t.name.toLowerCase().includes(keyword) ||
    t.description.toLowerCase().includes(keyword) ||
    t.category.toLowerCase().includes(keyword)
  )
})

// 选择模板
const selectTemplate = (template) => {
  emit('select', template)
}
</script>

<style scoped>
.template-selector {
  padding: 20px;
}

.template-selector h3 {
  margin-bottom: 20px;
  color: #303133;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.template-card {
  display: flex;
  padding: 20px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.template-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.template-icon {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
  margin-right: 15px;
}

.template-info {
  flex: 1;
}

.template-info h4 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 16px;
}

.template-desc {
  margin: 0 0 10px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
