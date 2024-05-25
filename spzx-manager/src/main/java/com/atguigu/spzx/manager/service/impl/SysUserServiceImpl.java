package com.atguigu.spzx.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.spzx.common.exception.GuiguException;
import com.atguigu.spzx.manager.mapper.SysUserMapper;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.model.dto.system.LoginDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.system.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Sunk
 * @version 1.0
 * @description:
 * @date 2024/5/24 15:26
 */
@Service
public class SysUserServiceImpl implements SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public LoginVo login(LoginDto loginDto) {
        //获取loginDto中的验证码key值，拿key去查询redis中的验证码值是否一致，一致再进行登录验证操作
        String codeKey = loginDto.getCodeKey();
        String codeValue = redisTemplate.opsForValue().get("user:login:validateCode" + codeKey);
        if (StrUtil.isEmpty(codeValue) || !StrUtil.equalsAnyIgnoreCase(codeValue, loginDto.getCaptcha())) {
            throw new GuiguException(ResultCodeEnum.VALIDATECODE_ERROR);
        }
        redisTemplate.delete("user:login:validateCode" + codeKey);

        //1 获取提交的用户名，loginDto中获取
        String userName = loginDto.getUserName();
        //2 根据用户名查询数据库表sys_User
        SysUser sysUser = sysUserMapper.selectUserInfoByUserName(userName);
        //3 如果根据用户名查不到用户信息：用户不存在，返回错误信息
        if (sysUser == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }
        //4 根据用户名查到用户信息，用户存在

        String database_password = sysUser.getPassword();
        String input_password = DigestUtils.md5DigestAsHex(loginDto.getPassword().getBytes());
        //5. 把输入的密码进行md5加密比较数据库中的密码
        if (!database_password.equals(input_password)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }
        //6 一致，登录成功，反之失败
        //7 登录成功生成token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //8 把token和用户信息放入redis中
        redisTemplate.opsForValue().set("user:login" + token, JSON.toJSONString(sysUser), 7, TimeUnit.DAYS);
        //9 返回loginVo对象
        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        return loginVo;
    }

    @Override
    public SysUser getUserInfo(String token) {
        String userJson = redisTemplate.opsForValue().get("user:login" + token);
        return JSON.parseObject(userJson, SysUser.class);
    }
}

