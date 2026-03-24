/**
 * ================================================================================
 * 人力资源数据中心系统 - Spring Boot 应用程序启动类
 * ================================================================================
 * 项目名称：HrDataCenter（人力资源数据中心系统）
 * 项目描述：基于Spring Boot + Vue 3的前后端分离系统，用于企业人力资源数据管理和分析
 *
 * 技术栈：
 * - 后端框架：Spring Boot 2.7.18
 * - 持久层：MyBatis Plus 3.5.5
 * - 安全框架：Spring Security + JWT
 * - 数据库：MySQL 8.0（业务数据）+ Hive 3.1.3（大数据分析）
 * - 前端框架：Vue 3.4.0 + Element Plus
 *
 * 核心功能：
 * 1. 用户认证与授权（登录、注册、权限管理）
 * 2. 员工档案管理（增删改查、批量导入导出）
 * 3. 数据分类管理（数据目录、元数据管理）
 * 4. 数据同步管理（MySQL到Hive的数据同步）
 * 5. 数据分析功能（薪酬福利、员工流失、绩效管理等）
 * 6. 报表模板管理（自定义报表模板）
 * 7. 预警规则管理（数据异常预警）
 * 8. 收藏夹功能（常用数据收藏）
 * 9. 管理员功能（用户管理、数据同步控制）
 *
 * 系统架构：
 * - 采用标准的分层架构：Controller → Service → Mapper → Database
 * - 使用JWT进行无状态认证
 * - 支持MySQL和Hive双数据源
 * - 提供RESTful API接口
 *
 * 作者：HrDataCenter开发团队
 * 版本：1.0.0
 * ================================================================================
 */
package com.hr.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 临时测试类（随便放在项目里，执行一次即可）
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring Boot应用程序启动类
 * <p>
 * 功能说明：
 * 1. 作为整个Spring Boot应用的入口点
 * 2. 启用Spring Boot自动配置
 * 3. 扫描并注册所有Spring组件（Controller、Service、Component等）
 * 4. 配置MyBatis Mapper接口扫描路径
 *
 * @MapperScan注解说明：
 * - 指定Mapper接口的扫描包路径为"com.hr.backend.mapper"
 * - Spring会自动扫描该包下的所有Mapper接口，并将其注册为Bean
 * - MyBatis Plus会自动为这些Mapper接口生成实现类
 */
@SpringBootApplication
@MapperScan("com.hr.backend.mapper")
public class HrDataCenterApplication {

    /**
     * 应用程序主入口方法
     * <p>
     * 执行流程：
     * 1. 初始化Spring Application上下文
     * 2. 加载所有配置文件（application.yml等）
     * 3. 扫描并注册所有Bean
     * 4. 启动内嵌的Tomcat服务器（默认端口8081）
     * 5. 应用启动完成，开始接收HTTP请求
     *
     * @param args 命令行参数，可用于覆盖配置文件中的配置项
     */
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String correctPwd = encoder.encode("123456"); // 生成123456的合法加密串
        System.out.println("正确的BCrypt加密串：" + correctPwd);

        SpringApplication.run(HrDataCenterApplication.class, args);
        System.out.println("========================================");
        System.out.println("人力资源数据中心系统启动成功！");
        System.out.println("访问地址: http://localhost:8081");
        System.out.println("========================================");
    }



    public class GenerateBCryptPwd {
        public  void main(String[] args) {

        }
    }
}
