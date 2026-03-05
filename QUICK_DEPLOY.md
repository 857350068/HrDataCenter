# 人力资源数据中心系统 - 超简单部署指南

## 📦 第一步：Windows主机 - 打包（一行命令）

在Windows PowerShell中执行：

```powershell
cd d:\HrDataCenter; cd frontend; npm install; npm run build; cd ..\backend; mvn clean package -DskipTests; cd ..; tar -czf HrDataCenter-deploy.tar.gz frontend\dist backend\target\*.jar database\mysql\init.sql
```

**或者双击运行**：`package.bat`

---

## 📤 第二步：上传到CentOS虚拟机

**使用WinSCP上传**：
1. 下载WinSCP: https://winscp.net/
2. 连接虚拟机（IP: 192.168.169.100）
3. 上传 `HrDataCenter-deploy.tar.gz` 到 `/opt/` 目录

**或使用SCP命令**：

```powershell
scp HrDataCenter-deploy.tar.gz root@192.168.169.100:/opt/
```

---

## 🔧 第三步：CentOS虚拟机 - 安装Nginx和JDK 1.8（一行命令）

```bash
yum install -y epel-release && yum install -y nginx java-1.8.0-openjdk java-1.8.0-openjdk-devel && echo 'export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk' >> /etc/profile && echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile && source /etc/profile && systemctl start nginx && systemctl enable nginx && firewall-cmd --permanent --add-port=80/tcp && firewall-cmd --reload && java -version
```

---

## 🚀 第四步：CentOS虚拟机 - 部署项目（一行命令）

```bash
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
systemctl restart nginx
```

---

## ▶️ 第五步：CentOS虚拟机 - 启动后端（一行命令）

```bash
kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') 2>/dev/null; cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "后端服务已启动，PID: $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}')" && echo "Java版本: $(java -version 2>&1 | head -n 1)"
```

---

## 🌐 第六步：访问系统

在Windows主机浏览器中访问：

```
http://192.168.169.100
```

### 测试账号

- 用户名: `admin`
- 密码: `123456`

---

## 📝 常用运维命令

### 后端服务

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

# 查看状态
systemctl status nginx

# 查看日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

---

## 🔍 验证部署

```bash
# 检查后端进程
ps -ef | grep java

# 查看后端日志
tail -100 /opt/backend.log

# 测试后端API
curl http://localhost:8081/api/auth/login

# 测试前端访问
curl http://localhost
```

---

## ⚡ 完整的一键部署脚本

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
systemctl restart nginx && kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') 2>/dev/null; cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "========================================="
echo "部署完成！"
echo "========================================="
echo "访问地址: http://$(hostname -I | awk '{print $1}')"
echo "测试账号: admin / 123456"
echo "Java版本: $(java -version 2>&1 | head -n 1)"
echo "========================================="
```

---

## 🎯 部署完成！

现在您可以在Windows主机浏览器中访问人力资源数据中心系统了！

**访问地址**: http://192.168.1.100
**测试账号**: admin / 123456
