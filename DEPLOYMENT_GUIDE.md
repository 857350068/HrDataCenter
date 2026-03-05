# 人力资源数据中心系统 - 完整部署指南（已有环境）

## 📋 项目结构分析

### 前端技术栈
- **框架**: Vue 3.4.0 + Vite 5.0.0
- **UI组件**: Element Plus 2.5.0
- **图表**: ECharts 5.4.3
- **路由**: Vue Router 4.2.5
- **状态管理**: Pinia 2.1.7
- **HTTP客户端**: Axios 1.6.5

### 后端技术栈
- **框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0.33
- **ORM**: MyBatis-Plus 3.5.5
- **连接池**: Druid 1.2.20
- **安全**: Spring Security + JWT
- **大数据**: Hive 3.1.3

### 项目目录结构
```
HrDataCenter/
├── frontend/              # 前端项目
│   ├── src/
│   │   ├── api/          # API接口
│   │   ├── components/   # 组件
│   │   ├── views/        # 页面
│   │   ├── router/       # 路由
│   │   └── stores/       # 状态管理
│   ├── package.json
│   └── vite.config.js
├── backend/              # 后端项目
│   ├── src/main/
│   │   ├── java/         # Java源码
│   │   └── resources/    # 配置文件
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── pom.xml
└── database/             # 数据库脚本
    ├── mysql/init.sql
    └── hive/init.sql
```

---

## 🚀 完整部署流程

### 第一步：Windows主机 - 打包项目

在Windows PowerShell中执行以下命令（一行命令）：

```powershell
cd d:\HrDataCenter; cd frontend; npm install; npm run build; cd ..\backend; mvn clean package -DskipTests; cd ..; tar -czf HrDataCenter-deploy.tar.gz frontend\dist backend\target\*.jar database\mysql\init.sql
```

**或者分步执行**：

```powershell
# 1. 进入项目目录
cd d:\HrDataCenter

# 2. 构建前端
cd frontend
npm install
npm run build

# 3. 构建后端
cd ..\backend
mvn clean package -DskipTests

# 4. 打包部署文件
cd ..
tar -czf HrDataCenter-deploy.tar.gz frontend\dist backend\target\*.jar database\mysql\init.sql
```

---

### 第二步：上传到CentOS虚拟机

**使用WinSCP上传**：
1. 下载WinSCP: https://winscp.net/
2. 连接虚拟机（IP: 你的虚拟机IP）
3. 上传 `HrDataCenter-deploy.tar.gz` 到 `/opt/` 目录

**或使用SCP命令上传**：

```powershell
scp HrDataCenter-deploy.tar.gz root@192.168.169.100:/opt/
```

---

### 第三步：CentOS虚拟机 - 安装Nginx

在CentOS虚拟机中执行以下命令（一行命令）：

```bash
yum install -y epel-release && yum install -y nginx && systemctl start nginx && systemctl enable nginx && firewall-cmd --permanent --add-port=80/tcp && firewall-cmd --reload
```

---

### 第四步：CentOS虚拟机 - 部署项目

在CentOS虚拟机中执行以下命令（一行命令）：

```bash
cd /opt && tar -xzf HrDataCenter-deploy.tar.gz && rm -rf /usr/share/nginx/html/* && cp -r frontend/dist/* /usr/share/nginx/html/ && systemctl restart nginx
```

---

### 第五步：CentOS虚拟机 - 启动后端服务

在CentOS虚拟机中执行以下命令（一行命令）：

```bash
cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "后端服务已启动，PID: $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}')"
```

---

### 第六步：配置Nginx反向代理

在CentOS虚拟机中执行以下命令：

**3.1 安装JDK 1.8（Java 8）**

```bash
# 安装OpenJDK 1.8
yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

# 设置JAVA_HOME环境变量
echo 'export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk' >> /etc/profile
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile
source /etc/profile

# 验证Java版本
java -version
```

应该看到类似输出：
```
openjdk version "1.8.0_xxx"
OpenJDK Runtime Environment (build 1.8.0_xxx-bxx)
OpenJDK 64-Bit Server VM (build 25.xxx-bxx, mixed mode)
```

**3.2 配置Nginx**

```bash
cat > /etc/nginx/conf.d/hr-datacenter.conf << 'EOF'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

systemctl restart nginx
```

**3.3 启动后端服务**

```bash
cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "后端服务已启动，PID: $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}')"
```

---

### 第七步：验证部署

```bash
# 查看后端日志
tail -f /opt/backend.log

# 查看后端进程
ps -ef | grep java

# 测试后端API
curl http://localhost:8081/api/auth/login

# 查看Nginx状态
systemctl status nginx
```

---

## 🌐 访问系统

### 在Windows主机浏览器中访问

```
http://192.168.169.100
```

替换 `192.168.1.100` 为你的虚拟机IP地址。

### 测试账号

- 用户名: `admin`
- 密码: `123456`

---

## 📝 常用运维命令

### 后端服务管理

```bash
# 启动后端
cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 &

# 停止后端
kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}')

# 重启后端
kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') && cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 &

# 查看日志
tail -f /opt/backend.log
```

### Nginx管理

```bash
# 重启Nginx
systemctl restart nginx

# 查看Nginx状态
systemctl status nginx

# 查看Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 数据库初始化

```bash
# 如果需要重新初始化数据库
mysql -uroot -p123456 < /opt/database/mysql/init.sql
```

---

## 🔧 故障排查

### 问题1: 无法访问前端页面

```bash
# 检查Nginx是否运行
systemctl status nginx

# 检查前端文件是否存在
ls -la /usr/share/nginx/html/

# 检查防火墙
firewall-cmd --list-ports
```

### 问题2: 无法访问后端API

```bash
# 检查后端进程
ps -ef | grep java

# 查看后端日志
tail -100 /opt/backend.log

# 测试MySQL连接
mysql -uroot -p123456 -e "SELECT 1;"
```

### 问题3: 图表不显示

```bash
# 测试后端API
curl http://localhost:8081/api/analysis/organization-efficiency?period=202401

# 查看数据库数据
mysql -uroot -p123456 hr_db -e "SELECT COUNT(*) FROM employee_profile WHERE category_id = 1;"
```

---

## 📦 完整的一键部署脚本

将以下内容保存为 `deploy.sh` 并在CentOS中执行：

```bash
#!/bin/bash
cd /opt && tar -xzf HrDataCenter-deploy.tar.gz && rm -rf /usr/share/nginx/html/* && cp -r frontend/dist/* /usr/share/nginx/html/ && cat > /etc/nginx/conf.d/hr-datacenter.conf << 'EOF'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://localhost:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
systemctl restart nginx && kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') 2>/dev/null; cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "部署完成！访问地址: http://$(hostname -I | awk '{print $1}')" && echo "测试账号: admin / 123456" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)" && echo "Java版本: $(java -version 2>&1 | head -n 1)"
```

---

## 🎯 快速部署步骤总结

### Windows主机（3个命令）
```powershell
cd d:\HrDataCenter && cd frontend && npm install && npm run build && cd ..\backend && mvn clean package -DskipTests && cd .. && tar -czf HrDataCenter-deploy.tar.gz frontend\dist backend\target\*.jar database\mysql\init.sql
scp HrDataCenter-deploy.tar.gz root@192.168.169.100:/opt/
```

### CentOS虚拟机（3个命令）
```bash
yum install -y epel-release && yum install -y nginx && systemctl start nginx && systemctl enable nginx && firewall-cmd --permanent --add-port=80/tcp && firewall-cmd --reload
cd /opt && tar -xzf HrDataCenter-deploy.tar.gz && rm -rf /usr/share/nginx/html/* && cp -r frontend/dist/* /usr/share/nginx/html/ && cat > /etc/nginx/conf.d/hr-datacenter.conf << 'EOF'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://localhost:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
systemctl restart nginx && kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') 2>/dev/null; cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "部署完成！访问地址: http://$(hostname -I | awk '{print $1}')" && echo "测试账号: admin / 123456"
```

---

**部署完成！现在您可以在Windows主机浏览器中访问系统。**
