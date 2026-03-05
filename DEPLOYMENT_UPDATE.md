# 部署文档更新说明

## 更新内容

### 1. 虚拟机IP地址更新
- 原IP: `192.168.1.100`
- 新IP: `192.168.169.100`

### 2. JDK版本要求
- 要求: JDK 1.8 (Java 8)
- 安装包: OpenJDK 1.8.0-openjdk

### 3. 更新的文件

#### QUICK_DEPLOY.md
- 更新上传命令中的IP地址
- 更新访问地址
- 添加JDK 1.8安装到第三步
- 在第五步启动后端时显示Java版本

#### DEPLOYMENT_GUIDE.md
- 更新所有IP地址引用
- 添加JDK 1.8安装说明（第三步第3.1小节）
- 添加启动后端服务的独立步骤（第三步第3.3小节）
- 在完整部署命令中显示Java版本

## 快速部署命令（已更新）

### Windows主机 - 打包
```powershell
cd d:\HrDataCenter; cd frontend; npm install; npm run build; cd ..\backend; mvn clean package -DskipTests; cd ..; tar -czf HrDataCenter-deploy.tar.gz frontend\dist backend\target\*.jar database\mysql\init.sql
```

### 上传到虚拟机
```powershell
scp HrDataCenter-deploy.tar.gz root@192.168.169.100:/opt/
```

### CentOS虚拟机 - 安装Nginx和JDK 1.8
```bash
yum install -y epel-release && yum install -y nginx java-1.8.0-openjdk java-1.8.0-openjdk-devel && echo 'export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk' >> /etc/profile && echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile && source /etc/profile && systemctl start nginx && systemctl enable nginx && firewall-cmd --permanent --add-port=80/tcp && firewall-cmd --reload && java -version
```

### CentOS虚拟机 - 部署项目
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

### CentOS虚拟机 - 启动后端
```bash
kill -9 $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}') 2>/dev/null; cd /opt && nohup java -jar backend/*.jar > backend.log 2>&1 & sleep 3 && echo "后端服务已启动，PID: $(ps -ef | grep 'backend.*jar' | grep -v grep | awk '{print $2}')" && echo "Java版本: $(java -version 2>&1 | head -n 1)"
```

### 访问系统
```
http://192.168.169.100
```

测试账号: `admin` / `123456`

## 验证Java版本

执行以下命令验证Java版本是否正确：

```bash
java -version
```

应该看到类似输出：
```
openjdk version "1.8.0_xxx"
OpenJDK Runtime Environment (build 1.8.0_xxx-bxx)
OpenJDK 64-Bit Server VM (build 25.xxx-bxx, mixed mode)
```
