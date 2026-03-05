@echo off
REM 人力资源数据中心系统 - 一键打包脚本

echo ========================================
echo 人力资源数据中心系统 - 开始打包
echo ========================================

REM 设置变量
set PROJECT_DIR=%~dp0
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0,8%

echo 项目目录: %PROJECT_DIR%
echo 时间戳: %TIMESTAMP%

REM 进入项目目录
cd /d "%PROJECT_DIR%"

echo.
echo [1/4] 开始构建前端...
cd frontend
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo 前端依赖安装失败！
    pause
    exit /b 1
)

call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo 前端构建失败！
    pause
    exit /b 1
)

echo ✓ 前端构建完成

echo.
echo [2/4] 开始构建后端...
cd ..\backend
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo 后端构建失败！
    pause
    exit /b 1
)

echo ✓ 后端构建完成

echo.
echo [3/4] 开始打包部署文件...
cd ..

REM 创建临时目录
if exist deploy_temp rmdir /s /q deploy_temp
mkdir deploy_temp

REM 复制前端文件
echo 复制前端文件...
xcopy frontend\dist deploy_temp\frontend\ /E /I /Y /Q >nul

REM 复制后端文件
echo 复制后端文件...
copy backend\target\*.jar deploy_temp\backend\ /Y >nul

REM 复制数据库脚本
echo 复制数据库脚本...
xcopy database\mysql\init.sql deploy_temp\database\ /Y >nul

echo ✓ 文件复制完成

echo.
echo [4/4] 压缩部署包...
echo 正在压缩...

REM 检查是否安装了tar命令
where tar >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    REM 使用tar命令压缩
    tar -czf HrDataCenter-deploy-%TIMESTAMP%.tar.gz -C deploy_temp .
    echo ✓ 压缩完成: HrDataCenter-deploy-%TIMESTAMP%.tar.gz
) else (
    REM 使用PowerShell压缩
    powershell -Command "Compress-Archive -Path deploy_temp\* -DestinationPath HrDataCenter-deploy-%TIMESTAMP%.zip -Force"
    echo ✓ 压缩完成: HrDataCenter-deploy-%TIMESTAMP%.zip
)

REM 清理临时目录
rmdir /s /q deploy_temp

echo.
echo ========================================
echo 打包完成！
echo ========================================
echo 部署包位置: %PROJECT_DIR%HrDataCenter-deploy-%TIMESTAMP%.tar.gz
echo.
echo 下一步操作:
echo 1. 上传 HrDataCenter-deploy-%TIMESTAMP%.tar.gz 到CentOS虚拟机
echo 2. 在CentOS中执行以下命令:
echo    cd /opt
echo    tar -xzf HrDataCenter-deploy-%TIMESTAMP%.tar.gz
echo    rm -rf /usr/share/nginx/html/*
echo    cp -r frontend/dist/* /usr/share/nginx/html/
echo    cat > /etc/nginx/conf.d/hr-datacenter.conf ^<^< 'EOF'
echo    server {
echo        listen 80;
echo        server_name _;
echo        root /usr/share/nginx/html;
echo        index index.html;
echo        location / {
echo            try_files $uri $uri/ /index.html;
echo        }
echo        location /api/ {
echo            proxy_pass http://localhost:8081/api/;
echo            proxy_set_header Host $host;
echo            proxy_set_header X-Real-IP $remote_addr;
echo            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
echo            proxy_set_header X-Forwarded-Proto $scheme;
echo        }
echo    }
echo    EOF
echo    systemctl restart nginx
echo    kill -9 $(ps -ef ^| grep 'backend.*jar' ^| grep -v grep ^| awk '{print $2}') 2^>/dev/null
echo    cd /opt
echo    nohup java -jar backend/*.jar ^> backend.log 2^>^&1 ^&
echo ========================================

pause
