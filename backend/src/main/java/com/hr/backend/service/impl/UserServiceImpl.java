package com.hr.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hr.backend.common.BaseException;
import com.hr.backend.common.ErrorCode;
import com.hr.backend.mapper.UserMapper;
import com.hr.backend.model.dto.ChangePasswordRequest;
import com.hr.backend.model.dto.LoginRequest;
import com.hr.backend.model.dto.LoginResponse;
import com.hr.backend.model.dto.RegisterRequest;
import com.hr.backend.model.entity.User;
import com.hr.backend.security.JwtUtil;
import com.hr.backend.security.SecurityUtil;
import com.hr.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 用户服务实现类
 * 处理用户登录、注册、密码修改等核心业务逻辑
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 日志对象，用于定位登录问题（核心调试工具）
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    // 注入密码编码器（BCrypt）
    @Resource
    private PasswordEncoder passwordEncoder;

    // 注入JWT工具类
    @Resource
    private JwtUtil jwtUtil;

    /**
     * 用户登录核心方法
     * @param request 登录请求（用户名+密码）
     * @return 登录响应（Token+用户信息）
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 提取前端参数并打印（关键：定位传参问题）
        String username = request.getUsername();
        String inputPwd = request.getPassword();
        log.info("【登录校验】前端传入 → 用户名：{}，密码明文：{}，密码长度：{}",
                username, inputPwd, inputPwd == null ? 0 : inputPwd.length());

        // 2. 查询用户（已加is_deleted=0条件）
        User user = baseMapper.selectByUsername(username);
        if (user == null) {
            log.error("【登录失败】用户不存在：{}", username);
            throw new BaseException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        log.info("【登录校验】数据库查询 → 用户：{}，状态：{}，加密密码：{}",
                user.getUsername(), user.getStatus(), user.getPassword());

        // 3. 校验账号状态（是否禁用）
        if (user.getStatus() != null && user.getStatus() != 1) {
            log.error("【登录失败】账号已禁用：{}", username);
            throw new BaseException(ErrorCode.FORBIDDEN.getCode(), "账号已禁用");
        }

        // 4. 密码匹配校验（核心：明文在前，加密串在后）
        boolean isMatch = passwordEncoder.matches(inputPwd, user.getPassword());
        log.info("【登录校验】密码匹配结果：{}（输入明文：{}，数据库加密串：{}）",
                isMatch, inputPwd, user.getPassword());

        // 5. 密码不匹配则抛异常
        if (!isMatch) {
            log.error("【登录失败】密码不匹配：用户={}，输入密码={}", username, inputPwd);
            throw new BaseException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        // 6. 生成JWT Token并返回
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("【登录成功】用户={}，生成Token（部分）：{}", user.getUsername(), token.substring(0, 20) + "...");
        return LoginResponse.of(token, user);
    }

    /**
     * 用户注册
     * @param request 注册请求（工号、密码、姓名等）
     */
    @Override
    public void register(RegisterRequest request) {
        // 校验工号是否已存在
        if (baseMapper.selectByUsername(request.getUsername()) != null) {
            throw new BaseException(ErrorCode.USER_EXISTS.getCode(), "工号已存在");
        }
        // 构建用户对象（密码加密存储）
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setDeptId(request.getDeptId());
        user.setDeptName(request.getDeptName());
        user.setDeptScope(request.getDeptScope());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1); // 默认启用
        save(user);
    }

    /**
     * 修改密码
     * @param request 改密请求（原密码+新密码）
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        // 获取当前登录用户
        User current = SecurityUtil.getCurrentUser();
        if (current == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        // 校验原密码
        User user = getById(current.getId());
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.PARAM_ERROR.getCode(), "原密码错误");
        }
        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public User getById(Long id) {
        return super.getById(id);
    }

    /**
     * 分页查询用户
     * @param page 分页参数
     * @param username 用户名模糊查询
     * @param role 角色精准查询
     * @return 分页结果
     */
    @Override
    public IPage<User> page(Page<User> page, String username, String role) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 过滤已删除用户
        queryWrapper.eq(User::getIsDeleted, 0);
        // 用户名模糊查询
        if (StringUtils.hasText(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        // 角色精准查询（修复：原代码多了括号 User::getRole() → User::getRole）
        if (StringUtils.hasText(role)) {
            queryWrapper.eq(User::getRole, role);
        }
        // 按创建时间降序
        queryWrapper.orderByDesc(User::getCreateTime);
        return super.page(page, queryWrapper);
    }

    /**
     * 保存用户（新增/编辑）
     * @param user 用户信息
     */
    @Override
    public void saveUser(User user) {
        // 校验工号是否重复
        if (baseMapper.selectByUsername(user.getUsername()) != null) {
            throw new BaseException(ErrorCode.USER_EXISTS.getCode(), "工号已存在");
        }
        // 密码加密（编辑时若不传密码则不更新）
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // 默认启用状态
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        save(user);
    }
}