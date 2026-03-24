<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">数据管理（员工档案）</h2>
      <el-button type="primary" @click="openDialog()">新增员工</el-button>
    </div>
    <el-card>
      <el-form inline class="mb-4">
        <el-form-item label="员工编号">
          <el-input v-model="query.employeeNo" placeholder="员工编号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptName" placeholder="请选择部门" clearable style="width: 150px">
            <el-option v-for="dept in departmentList" :key="dept.name" :label="dept.name" :value="dept.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据分类">
          <el-select v-model="query.categoryId" placeholder="请选择分类" clearable style="width: 150px">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期">
          <el-input v-model="query.period" placeholder="如202601" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="employeeNo" label="员工编号" width="100" />
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="deptName" label="部门" width="100" />
        <el-table-column prop="job" label="岗位" width="100" />
        <el-table-column prop="categoryId" label="分类ID" width="80">
          <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="value" label="指标值" width="100" />
        <el-table-column prop="period" label="周期" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, prev, pager, next"
        @current-change="load"
        style="margin-top: 16px"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="员工编号" prop="employeeNo">
          <el-input v-model="form.employeeNo" placeholder="请输入员工编号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="deptName">
          <el-select v-model="form.deptName" placeholder="请选择部门" clearable style="width: 100%">
            <el-option v-for="dept in departmentList" :key="dept.name" :label="dept.name" :value="dept.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="job">
          <el-input v-model="form.job" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="数据分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择数据分类" clearable>
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="指标值" prop="value">
          <el-input-number v-model="form.value" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="统计周期" prop="period">
          <el-input v-model="form.period" placeholder="如: 202401" maxlength="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getEmployeePage, addEmployee, updateEmployee, deleteEmployee } from '@/api/data'
import { getCategoryList } from '@/api/category'
import { getDepartmentList } from '@/api/department'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = reactive({ employeeNo: '', deptName: '', categoryId: null, period: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const categoryList = ref([])
const departmentList = ref([])

const form = reactive({
  id: null,
  employeeNo: '',
  name: '',
  deptName: '',
  job: '',
  categoryId: null,
  value: 0,
  period: ''
})

const rules = {
  employeeNo: [{ required: true, message: '请输入员工编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptName: [{ required: true, message: '请选择部门', trigger: 'change' }],
  job: [{ required: true, message: '请输入岗位', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择数据分类', trigger: 'change' }],
  value: [{ required: true, message: '请输入指标值', trigger: 'blur' }],
  period: [{ required: true, message: '请输入统计周期', trigger: 'blur' }]
}

const loadCategoryList = async () => {
  try {
    const res = await getCategoryList()
    categoryList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载分类列表失败')
  }
}

const loadDepartmentList = async () => {
  try {
    const res = await getDepartmentList()
    departmentList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载部门列表失败')
  }
}

const getCategoryName = (categoryId) => {
  const category = categoryList.value.find(item => item.id === categoryId)
  return category ? category.name : ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getEmployeePage({
      current: page.current,
      size: page.size,
      employeeNo: query.employeeNo,
      deptName: query.deptName,
      categoryId: query.categoryId,
      period: query.period
    })
    tableData.value = res.data.records || []
    page.total = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.employeeNo = ''
  query.deptName = ''
  query.categoryId = null
  query.period = ''
  page.current = 1
  load()
}

const openDialog = (row = null) => {
  if (row) {
    dialogTitle.value = '编辑员工档案'
    form.id = row.id
    form.employeeNo = row.employeeNo
    form.name = row.name
    form.deptName = row.deptName
    form.job = row.job
    form.categoryId = row.categoryId
    form.value = row.value
    form.period = row.period
  } else {
    dialogTitle.value = '新增员工档案'
    resetForm()
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.id = null
  form.employeeNo = ''
  form.name = ''
  form.deptName = ''
  form.job = ''
  form.categoryId = null
  form.value = 0
  form.period = ''
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateEmployee(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await addEmployee(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该员工档案吗？', '提示', { type: 'warning' })
    await deleteEmployee(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadCategoryList()
  loadDepartmentList()
  load()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
