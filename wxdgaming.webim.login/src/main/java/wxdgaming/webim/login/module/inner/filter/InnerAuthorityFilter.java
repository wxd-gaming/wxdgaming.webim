package wxdgaming.webim.login.module.inner.filter;

import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;

import java.lang.reflect.Method;

/**
 * 权限
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 22:07
 **/
@Slf4j
@Singleton
public class InnerAuthorityFilter extends HoldRunApplication implements HttpFilter {

    @Value(path = "innerAuthorizationKey")
    private String innerAuthorizationKey;

    @Override public Object doFilter(HttpRequest httpRequest, Method method, HttpContext httpContext) {
        if (httpContext.getRequest().getUriPath().startsWith("/inner")) {
            String reqContent = httpContext.getRequest().getReqContent();
            String authorization = httpContext.getRequest().header(HttpHeaderNames.AUTHORIZATION);
            String digestEncode = Md5Util.md5DigestEncode(reqContent, innerAuthorizationKey);
            if (!Objects.equals(authorization, digestEncode)) {
                return RunResult.fail("授权失败");
            }
        }
        return null;
    }
}
