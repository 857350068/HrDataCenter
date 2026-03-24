<template>
  <div class="query-builder">
    <!-- 步骤指示器 -->
    <el-steps :active="currentStep" finish-status="success" align-center>
      <el-step title="选择数据表" />
      <el-step title="选择字段" />
      <el-step title="设置条件" />
      <el-step title="预览SQL" />
    </el-steps>

    <!-- 步骤1: 选择数据表 -->
    <div v-show="currentStep === 0" class="step-content">
      <h3>选择数据表</h3>
      <el-checkbox-group v-model="selectedTables">
        <el-checkbox v-for="table in availableTables" :key="table.name" :label="table.name">
          <div class="table-item">
            <span class="table-name">{{ table.name }}</span>
            <span class="table-desc">{{ table.description }}</span>
          </div>
        </el-checkbox>
      </el-checkbox-group>
    </div>

    <!-- 步骤2: 选择字段 -->
    <div v-show="currentStep === 1" class="step-content">
      <h3>选择字段</h3>
      <div v-for="table in selectedTables" :key="table" class="field-group">
        <h4>{{ table }} 表的字段</h4>
        <el-checkbox-group v-model="selectedFields[table]">
          <el-checkbox v-for="field in getTableFields(table)" :key="field.name" :label="field.name">
            <span class="field-name">{{ field.name }}</span>
            <span class="field-type">{{ field.type }}</span>
            <span class="field-desc">{{ field.description }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>

    <!-- 步骤3: 设置条件 -->
    <div v-show="currentStep === 2" class="step-content">
      <h3>设置查询条件</h3>
      <div class="conditions-container">
        <div v-for="(condition, index) in conditions" :key="index" class="condition-item">
          <el-select v-model="condition.field" placeholder="选择字段" style="width: 200px">
            <el-option
              v-for="field in allSelectedFields"
              :key="field"
              :label="field"
              :value="field"
            />
          </el-select>
          <el-select v-model="condition.operator" placeholder="操作符" style="width: 120px">
            <el-option label="等于" value="=" />
            <el-option label="不等于" value="!=" />
            <el-option label="大于" value=">" />
            <el-option label="小于" value="<" />
            <el-option label="大于等于" value=">=" />
            <el-option label="小于等于" value="<=" />
            <el-option label="包含" value="LIKE" />
            <el-option label="不包含" value="NOT LIKE" />
            <el-option label="为空" value="IS NULL" />
            <el-option label="不为空" value="IS NOT NULL" />
          </el-select>
          <el-input
            v-model="condition.value"
            placeholder="值"
            style="width: 200px"
            :disabled="condition.operator === 'IS NULL' || condition.operator === 'IS NOT NULL'"
          />
          <el-button type="danger" icon="Delete" @click="removeCondition(index)" circle />
        </div>
        <el-button type="primary" icon="Plus" @click="addCondition">添加条件</el-button>
        <el-radio-group v-model="conditionLogic" style="margin-left: 20px">
          <el-radio label="AND">AND (并且)</el-radio>
          <el-radio label="OR">OR (或者)</el-radio>
        </el-radio-group>
      </div>

      <h3 style="margin-top: 30px">排序设置</h3>
      <div class="sort-container">
        <div v-for="(sort, index) in sorts" :key="index" class="sort-item">
          <el-select v-model="sort.field" placeholder="选择字段" style="width: 200px">
            <el-option
              v-for="field in allSelectedFields"
              :key="field"
              :label="field"
              :value="field"
            />
          </el-select>
          <el-select v-model="sort.order" placeholder="排序方式" style="width: 120px">
            <el-option label="升序" value="ASC" />
            <el-option label="降序" value="DESC" />
          </el-select>
          <el-button type="danger" icon="Delete" @click="removeSort(index)" circle />
        </div>
        <el-button type="primary" icon="Plus" @click="addSort">添加排序</el-button>
      </div>

      <h3 style="margin-top: 30px">分组设置</h3>
      <div class="group-container">
        <el-select v-model="groupBy" multiple placeholder="选择分组字段" style="width: 100%">
          <el-option
            v-for="field in allSelectedFields"
            :key="field"
            :label="field"
            :value="field"
          />
        </el-select>
      </div>
    </div>

    <!-- 步骤4: 预览SQL -->
    <div v-show="currentStep === 3" class="step-content">
      <h3>生成的SQL查询语句</h3>
      <el-input
        v-model="generatedSQL"
        type="textarea"
        :rows="10"
        readonly
        class="sql-preview"
      />
      <div class="sql-actions">
        <el-button type="success" icon="View" @click="previewData">预览数据</el-button>
        <el-button type="primary" icon="CopyDocument" @click="copySQL">复制SQL</el-button>
      </div>

      <!-- 数据预览 -->
      <div v-if="previewDataList.length > 0" class="data-preview">
        <h3>数据预览（前10条）</h3>
        <el-table :data="previewDataList" border stripe>
          <el-table-column
            v-for="column in previewColumns"
            :key="column"
            :prop="column"
            :label="column"
            min-width="120"
          />
        </el-table>
      </div>
    </div>

    <!-- 底部操作按钮 -->
    <div class="step-actions">
      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="currentStep === 3" type="success" @click="confirmSQL">确认使用</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['confirm'])

const currentStep = ref(0)
const selectedTables = ref([])
const selectedFields = ref({})
const conditions = ref([])
const conditionLogic = ref('AND')
const sorts = ref([])
const groupBy = ref([])
const generatedSQL = ref('')
const previewDataList = ref([])
const previewColumns = ref([])

// 可用的数据表定义
const availableTables = [
  { name: 'employee_profile', description: '员工档案表' },
  { name: 'hr_department', description: '部门表' },
  { name: 'hr_data_category', description: '数据分类表' },
  { name: 'warning_rule', description: '预警规则表' },
  { name: 'report_template', description: '报表模板表' }
]

// 表字段定义
const tableFields = {
  employee_profile: [
    { name: 'id', type: 'BIGINT', description: '主键ID' },
    { name: 'employee_no', type: 'VARCHAR', description: '员工编号' },
    { name: 'name', type: 'VARCHAR', description: '姓名' },
    { name: 'gender', type: 'VARCHAR', description: '性别' },
    { name: 'age', type: 'INT', description: '年龄' },
    { name: 'dept_id', type: 'BIGINT', description: '部门ID' },
    { name: 'dept_name', type: 'VARCHAR', description: '部门名称' },
    { name: 'position', type: 'VARCHAR', description: '职位' },
    { name: 'level', type: 'VARCHAR', description: '职级' },
    { name: 'category_id', type: 'INT', description: '数据分类ID' },
    { name: 'period', type: 'VARCHAR', description: '周期' },
    { name: 'value', type: 'DECIMAL', description: '数值' },
    { name: 'unit', type: 'VARCHAR', description: '单位' },
    { name: 'create_time', type: 'DATETIME', description: '创建时间' },
    { name: 'update_time', type: 'DATETIME', description: '更新时间' },
    { name: 'is_deleted', type: 'INT', description: '是否删除' }
  ],
  hr_department: [
    { name: 'id', type: 'BIGINT', description: '主键ID' },
    { name: 'dept_name', type: 'VARCHAR', description: '部门名称' },
    { name: 'dept_code', type: 'VARCHAR', description: '部门编码' },
    { name: 'parent_id', type: 'BIGINT', description: '上级部门ID' },
    { name: 'level', type: 'INT', description: '层级' },
    { name: 'manager', type: 'VARCHAR', description: '负责人' },
    { name: 'create_time', type: 'DATETIME', description: '创建时间' }
  ],
  hr_data_category: [
    { name: 'id', type: 'INT', description: '主键ID' },
    { name: 'category_name', type: 'VARCHAR', description: '分类名称' },
    { name: 'category_code', type: 'VARCHAR', description: '分类编码' },
    { name: 'description', type: 'VARCHAR', description: '描述' }
  ],
  warning_rule: [
    { name: 'id', type: 'BIGINT', description: '主键ID' },
    { name: 'rule_name', type: 'VARCHAR', description: '规则名称' },
    { name: 'category_id', type: 'INT', description: '分类ID' },
    { name: 'threshold_type', type: 'VARCHAR', description: '阈值类型' },
    { name: 'threshold_value', type: 'DECIMAL', description: '阈值' },
    { name: 'enabled', type: 'INT', description: '是否启用' }
  ],
  report_template: [
    { name: 'id', type: 'BIGINT', description: '主键ID' },
    { name: 'name', type: 'VARCHAR', description: '模板名称' },
    { name: 'category', type: 'VARCHAR', description: '分类' },
    { name: 'description', type: 'VARCHAR', description: '描述' },
    { name: 'query_sql', type: 'TEXT', description: '查询SQL' },
    { name: 'enabled', type: 'INT', description: '是否启用' }
  ]
}

// 获取表字段
const getTableFields = (tableName) => {
  return tableFields[tableName] || []
}

// 所有已选择的字段
const allSelectedFields = computed(() => {
  const fields = []
  Object.keys(selectedFields.value).forEach(table => {
    selectedFields.value[table].forEach(field => {
      fields.push(`${table}.${field}`)
    })
  })
  return fields
})

// 添加条件
const addCondition = () => {
  conditions.value.push({
    field: '',
    operator: '=',
    value: ''
  })
}

// 移除条件
const removeCondition = (index) => {
  conditions.value.splice(index, 1)
}

// 添加排序
const addSort = () => {
  sorts.value.push({
    field: '',
    order: 'ASC'
  })
}

// 移除排序
const removeSort = (index) => {
  sorts.value.splice(index, 1)
}

// 生成SQL
const generateSQL = () => {
  if (selectedTables.value.length === 0) {
    generatedSQL.value = '-- 请先选择数据表'
    return
  }

  let sql = 'SELECT\n'

  // SELECT 子句
  const selectFields = []
  Object.keys(selectedFields.value).forEach(table => {
    selectedFields.value[table].forEach(field => {
      selectFields.push(`  ${table}.${field}`)
    })
  })

  if (selectFields.length === 0) {
    selectFields.push('  *')
  }

  sql += selectFields.join(',\n')
  sql += '\nFROM\n'

  // FROM 子句
  sql += `  ${selectedTables.value[0]}`

  // JOIN 子句（如果有多个表，这里简化处理）
  for (let i = 1; i < selectedTables.value.length; i++) {
    sql += `\n  LEFT JOIN ${selectedTables.value[i]} ON ${selectedTables.value[0]}.id = ${selectedTables.value[i]}.id`
  }

  // WHERE 子句
  if (conditions.value.length > 0) {
    sql += '\nWHERE\n'
    const whereClauses = conditions.value.map((cond, index) => {
      let clause = `  ${cond.field} ${cond.operator}`
      if (cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL') {
        clause += ` '${cond.value}'`
      }
      if (index < conditions.value.length - 1) {
        clause += ` ${conditionLogic.value}`
      }
      return clause
    })
    sql += whereClauses.join('\n')
  }

  // GROUP BY 子句
  if (groupBy.value.length > 0) {
    sql += '\nGROUP BY\n'
    sql += `  ${groupBy.value.join(',\n  ')}`
  }

  // ORDER BY 子句
  if (sorts.value.length > 0) {
    sql += '\nORDER BY\n'
    const orderClauses = sorts.value.map(sort => `  ${sort.field} ${sort.order}`)
    sql += orderClauses.join(',\n')
  }

  // LIMIT 子句
  sql += '\nLIMIT 1000'

  generatedSQL.value = sql
}

// 监听变化自动生成SQL
watch([selectedTables, selectedFields, conditions, conditionLogic, sorts, groupBy], () => {
  generateSQL()
}, { deep: true })

// 上一步
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 下一步
const nextStep = () => {
  if (currentStep.value === 0 && selectedTables.value.length === 0) {
    ElMessage.warning('请至少选择一个数据表')
    return
  }
  if (currentStep.value === 1) {
    let hasFields = false
    Object.keys(selectedFields.value).forEach(table => {
      if (selectedFields.value[table].length > 0) {
        hasFields = true
      }
    })
    if (!hasFields) {
      ElMessage.warning('请至少选择一个字段')
      return
    }
  }
  if (currentStep.value < 3) {
    currentStep.value++
  }
}

// 预览数据
const previewData = async () => {
  // 这里应该调用后端API执行SQL预览
  // 暂时使用模拟数据
  ElMessage.info('SQL预览功能需要后端支持')
}

// 复制SQL
const copySQL = () => {
  navigator.clipboard.writeText(generatedSQL.value)
  ElMessage.success('SQL已复制到剪贴板')
}

// 确认使用
const confirmSQL = () => {
  emit('confirm', generatedSQL.value)
}
</script>

<style scoped>
.query-builder {
  padding: 20px;
}

.step-content {
  margin-top: 30px;
  min-height: 300px;
}

.step-content h3 {
  margin-bottom: 20px;
  color: #303133;
}

.step-content h4 {
  margin: 15px 0 10px;
  color: #606266;
}

.table-item {
  display: flex;
  flex-direction: column;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-right: 20px;
}

.table-name {
  font-weight: bold;
  color: #303133;
}

.table-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.field-group {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.field-name {
  font-weight: bold;
  color: #303133;
  margin-right: 10px;
}

.field-type {
  font-size: 12px;
  color: #409eff;
  margin-right: 10px;
}

.field-desc {
  font-size: 12px;
  color: #909399;
}

.conditions-container,
.sort-container,
.group-container {
  margin-bottom: 20px;
}

.condition-item,
.sort-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  align-items: center;
}

.sql-preview {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.sql-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.data-preview {
  margin-top: 30px;
}

.step-actions {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

:deep(.el-checkbox) {
  display: flex;
  margin-right: 20px;
  margin-bottom: 10px;
}

:deep(.el-checkbox__label) {
  white-space: normal;
}
</style>
