<template>
  <div class="register-container">
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="card-header">
          <span>用户注册</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="large">
        <el-form-item label="工号" prop="username">
          <el-input v-model="form.username" placeholder="工号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="部门主管" value="DEPT_HEAD" />
            <el-option label="管理层" value="MANAGEMENT" />
            <el-option label="普通员工" value="EMPLOYEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSubmit">注册</el-button>
          <el-button @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

// 注册时部门列表使用静态数据（注册接口无需登录）

const formRef = ref(null)
const loading = ref(false)
const departments = ref([])
const form = reactive({
  username: '',
  password: '',
  name: '',
  role: 'EMPLOYEE',
  deptId: null
})
const rules = {
  username: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => {
  departments.value = [
    { id: 1, name: '销售部' },
    { id: 2, name: '研发部' },
    { id: 3, name: '人事部' },
    { id: 4, name: '财务部' },
    { id: 5, name: '市场部' },
    { id: 6, name: '运营部' }
  ]
})

const onSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await request({ url: '/auth/register', method: 'post', data: form })
    ElMessage.success('注册成功，请登录')
    window.location.href = '/#/login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgb(251, 251, 251) 0%, #fefefe 100%);
}
.register-card {
  width: 480px;
}
</style>
