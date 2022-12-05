package com.totoropcbeta.studentmanagesystem.service.impl;

import com.totoropcbeta.studentmanagesystem.bo.AccessToken;
import com.totoropcbeta.studentmanagesystem.bo.UserDetail;
import com.totoropcbeta.studentmanagesystem.cache.Cache;
import com.totoropcbeta.studentmanagesystem.enums.CacheName;
import com.totoropcbeta.studentmanagesystem.provider.AuthProvider;
import com.totoropcbeta.studentmanagesystem.provider.JwtProvider;
import com.totoropcbeta.studentmanagesystem.service.StudentAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yuanhang08
 * @date 2022年12月04日
 */
@Service
@Slf4j
@Transactional
public class StudentAuthServiceImpl implements StudentAuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private Cache caffeineCache;


    @Override
    public AccessToken login(String studentId, String passWord) {
        log.info("进入login方法...");
        // 1 创建UsernamePasswordAuthenticationToken
        UsernamePasswordAuthenticationToken usernameAuthentication = new UsernamePasswordAuthenticationToken(studentId, passWord);
        log.info("创建UsernamePasswordAuthenticationToken: {}", usernameAuthentication);
        // 2 认证
        Authentication authentication = this.authenticationManager.authenticate(usernameAuthentication);
        log.info("认证 authentication: {}", authentication.getPrincipal().toString());
        // 3 保存认证信息
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 4 生成自定义token
        AccessToken accessToken = jwtProvider.createToken((UserDetails) authentication.getPrincipal());
        log.info("生成自定义token: {}", accessToken);
        UserDetail userDetail = (UserDetail) authentication.getPrincipal();
        // 5 放入缓存
        caffeineCache.put(CacheName.USER, userDetail.getUsername(), userDetail);
        return accessToken;
    }

    @Override
    public void logout() {
        caffeineCache.remove(CacheName.USER, AuthProvider.getLoginAccount());
        SecurityContextHolder.clearContext();
    }

    @Override
    public AccessToken refreshToken(String token) {
        AccessToken accessToken = jwtProvider.refreshToken(token);
        UserDetail userDetail = caffeineCache.get(CacheName.USER, accessToken.getLoginAccount(), UserDetail.class);
        caffeineCache.put(CacheName.USER, accessToken.getLoginAccount(), userDetail);
        return accessToken;
    }
}