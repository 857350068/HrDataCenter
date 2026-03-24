<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>
    <el-card>
      <el-form inline class="mb-4">
        <el-form-item label="工号">
          <el-input v-model="query.username" placeholder="工号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="角色" clearable style="width: 120px">
            <el-option label="HR管理员" value="HR_ADMIN" />
            <el-option label="部门主管" value="DEPT_HEAD" />
            <el-option label="管理层" value="MANAGEMENT" />
            <el-option label="普通员工" value="EMPLOYEE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="工号" width="100" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-form-item label="工号" prop="username">
          <el-input v-model="form.username" placeholder="请输入工号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" clearable>
            <el-option label="HR管理员" value="HR_ADMIN" />
            <el-option label="部门主管" value="DEPT_HEAD" />
            <el-option label="管理层" value="MANAGEMENT" />
            <el-option label="普通员工" value="EMPLOYEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="deptName">
          <el-select v-model="form.deptName" placeholder="请选择部门" clearable style="width: 100%">
            <el-option v-for="dept in departmentList" :key="dept.name" :label="dept.name" :value="dept.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { getUserPage, addUser, updateUser, deleteUser } from '@/api/admin'
import { getDepartmentList } from '@/api/department'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = reactive({ username: '', role: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const departmentList = ref([])

const form = reactive({
  id: null,
  username: '',
  name: '',
  password: '',
  role: '',
  deptName: '',
  phone: '',
  email: '',
  status: 1
})

const rules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[A-Z0-9]+$/, message: '工号只能包含大写字母和数字', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  deptName: [{ required: true, message: '请选择部门', trigger: 'change' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const loadDepartmentList = async () => {
  try {
    const res = await getDepartmentList()
    departmentList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载部门列表失败')
  }
}

const getRoleName = (role) => {
  const roleMap = {
    'HR_ADMIN': 'HR管理员',
    'DEPT_HEAD': '部门主管',
    'MANAGEMENT': '管理层',
    'EMPLOYEE': '普通员工'
  }
  return roleMap[role] || role
}

const load = async () => {
  loading.value = true
  try {
    const res = await getUserPage({
      current: page.current,
      size: page.size,
      username: query.username,
      role: query.role
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
  query.username = ''
  query.role = ''
  page.current = 1
  load()
}

const openDialog = (row = null) => {
  if (row) {
    dialogTitle.value = '编辑用户'
    form.id = row.id
    form.username = row.username
    form.name = row.name
    form.password = ''
    form.role = row.role
    form.deptName = row.deptName || ''
    form.phone = row.phone || ''
    form.email = row.email || ''
    form.status = row.status
  } else {
    dialogTitle.value = '新增用户'
    resetForm()
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.id = null
  form.username = ''
  form.name = ''
  form.password = ''
  form.role = ''
  form.deptName = ''
  form.phone = ''
  form.email = ''
  form.status = 1
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateUser(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await addUser(form)
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
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    await deleteUser(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
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
