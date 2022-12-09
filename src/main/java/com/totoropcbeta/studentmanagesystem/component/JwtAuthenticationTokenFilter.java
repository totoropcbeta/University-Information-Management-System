package com.totoropcbeta.studentmanagesystem.component;

import cn.hutool.core.util.StrUtil;
import com.totoropcbeta.studentmanagesystem.bo.UserDetail;
import com.totoropcbeta.studentmanagesystem.cache.Cache;
import com.totoropcbeta.studentmanagesystem.enums.CacheName;
import com.totoropcbeta.studentmanagesystem.properties.JwtProperties;
import com.totoropcbeta.studentmanagesystem.provider.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT登录过滤器
 * 拿到请求头中的token解析出其中的用户信息,
 * 将用户信息传给下一条过滤器,
 * 拿到上下文对象赋值到上下文。
 */
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private Cache caffeineCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        log.info("JWT过滤器通过校验请求头token进行自动登录...");

        // 拿到access-token请求头内的信息
        String authToken = jwtProvider.getToken(request);

        // 判断一下内容是否为空
        if (StrUtil.isNotEmpty(authToken) && authToken.startsWith(jwtProperties.getTokenPrefix())) {
            // 去掉token前缀(Bearer ), 拿到真实token
            authToken = authToken.substring(jwtProperties.getTokenPrefix().length());

            // 拿到token里面的登录账号
            String userId = jwtProvider.getSubjectFromToken(authToken);
            log.info("token里面的登录账号: {}", userId);
            if (StrUtil.isNotEmpty(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 查询用户
                UserDetail userDetail = caffeineCache.get(CacheName.USER, userId, UserDetail.class);
                log.info("查询缓存获取到的userDetail: {}", userDetail);
                // 拿到用户信息后验证用户信息与token
                if (userDetail != null && jwtProvider.validateToken(authToken, userDetail)) {

                    // 组装authentication对象, 构造参数是Principal Credentials 与 Authorities
                    // 后面的拦截器里面会用到 grantedAuthorities 方法
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetail, userDetail.getPassword(), userDetail.getAuthorities());

                    // 将authentication信息放入到上下文对象中
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("JWT过滤器通过校验请求头token自动登录成功, userId: {}", userDetail.getUserId());
                }
            }
        }

        chain.doFilter(request, response);
    }
}
