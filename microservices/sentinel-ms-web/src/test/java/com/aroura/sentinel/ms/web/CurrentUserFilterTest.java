package com.aroura.sentinel.ms.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aroura.sentinel.web.vo.CurrentUserVO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身份头验签测试。
 * <p>
 * 关键性质：<b>带了身份头就必须带对签名</b>。特别覆盖「签名错时是拒绝而不是当作匿名放行」——
 * 后者会让没有 @RequireRole 的端点（/internal/**）在伪造请求下照常执行。
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserFilterTest {

    private static final String SECRET = "test-secret";
    private static final long SKEW = 300L;
    private static final String USERNAME = "张伟";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    private static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    /** 只 stub 身份相关头；未 stub 的 getRequestURI() 返回 null，走非内部接口分支。 */
    private void givenIdentity(String name, String role, String sig) {
        when(request.getHeader(CurrentUserFilter.HDR_USER_NAME)).thenReturn(name);
        when(request.getHeader(CurrentUserFilter.HDR_USER_ROLE)).thenReturn(role);
        when(request.getHeader(InternalAuth.HDR_SIGNATURE)).thenReturn(sig);
        when(request.getHeader(InternalAuth.HDR_TIMESTAMP)).thenReturn(String.valueOf(now()));
    }

    /**
     * 走一遍过滤器并断言被拒。
     * reject() 会往响应体写 JSON，所以要先 stub getWriter() —— 真实容器不会返回 null，
     * 这里只是补上 mock 的缺省值。
     */
    private void runExpectingRejection() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        new CurrentUserFilter(SECRET, SKEW).doFilterInternal(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verify(request, never()).setAttribute(any(), any());
    }

    @Test
    void 合法签名应放行并还原出中文用户名() throws Exception {
        String encoded = encode(USERNAME);
        givenIdentity(encoded, "ADMIN", InternalAuth.sign(SECRET, encoded, "ADMIN", now()));

        new CurrentUserFilter(SECRET, SKEW).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(request).setAttribute(eq(CurrentUserFilter.CURRENT_USER_ATTR), captor.capture());
        assertEquals(USERNAME, ((CurrentUserVO) captor.getValue()).getUsername(), "应还原成解码后的中文名");
    }

    /** 没有签名 —— 伪造身份头最直接的形式，必须拒绝而不是当匿名继续。 */
    @Test
    void 无签名应拒绝() throws Exception {
        givenIdentity(encode(USERNAME), "ADMIN", null);
        runExpectingRejection();
    }

    @Test
    void 签名错误应拒绝() throws Exception {
        givenIdentity(encode(USERNAME), "ADMIN", "bogus-signature");
        runExpectingRejection();
    }

    /** 用较低角色的签名套上高角色 —— 篡改角色即提权，必须失效。 */
    @Test
    void 用MERCHANT的签名冒充ADMIN应拒绝() throws Exception {
        String encoded = encode(USERNAME);
        givenIdentity(encoded, "ADMIN", InternalAuth.sign(SECRET, encoded, "MERCHANT", now()));
        runExpectingRejection();
    }

    /** 换成别人的签名（用户不匹配）同样要拒绝。 */
    @Test
    void 借用他人签名应拒绝() throws Exception {
        String encoded = encode(USERNAME);
        givenIdentity(encoded, "ADMIN", InternalAuth.sign(SECRET, encode("李四"), "ADMIN", now()));
        runExpectingRejection();
    }

    /** 完全没带身份头 → 匿名放行，是否拒绝交给 @RequireRole（默认拒绝）。 */
    @Test
    void 无身份头应匿名放行() throws Exception {
        new CurrentUserFilter(SECRET, SKEW).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(any(), any());
        verify(response, never()).setStatus(any(Integer.class));
    }

    @Test
    void 内部接口无签名应拒绝() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/agent/generate");
        runExpectingRejection();
    }

    @Test
    void 内部接口带伪造服务名应拒绝() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/agent/generate");
        when(request.getHeader(InternalAuth.HDR_SERVICE)).thenReturn("svc:logistics");
        when(request.getHeader(InternalAuth.HDR_TIMESTAMP)).thenReturn(String.valueOf(now()));
        when(request.getHeader(InternalAuth.HDR_SIGNATURE)).thenReturn("bogus");
        runExpectingRejection();
    }

    @Test
    void 内部接口带合法服务签名应放行() throws Exception {
        String svc = "svc:logistics";
        when(request.getRequestURI()).thenReturn("/internal/agent/generate");
        when(request.getHeader(InternalAuth.HDR_SERVICE)).thenReturn(svc);
        when(request.getHeader(InternalAuth.HDR_TIMESTAMP)).thenReturn(String.valueOf(now()));
        when(request.getHeader(InternalAuth.HDR_SIGNATURE))
                .thenReturn(InternalAuth.sign(SECRET, svc, InternalAuth.SERVICE_ROLE, now()));

        new CurrentUserFilter(SECRET, SKEW).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(any(), any());
    }

    /** 未配置密钥时跳过校验（本地无网关联调），不应抛异常，也不应误拒。 */
    @Test
    void 未配置密钥时应跳过校验() throws Exception {
        new CurrentUserFilter("", SKEW).doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
