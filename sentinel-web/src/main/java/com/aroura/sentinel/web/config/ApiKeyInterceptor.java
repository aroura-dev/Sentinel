package com.aroura.sentinel.web.config;

import com.aroura.sentinel.web.service.sentinel.tms.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 对外开放接口的 API Key 认证（当前挂载于 {@code /api/omnimerchant/**}）。
 *
 * <h3>为什么不能走会话认证</h3>
 * 这些接口是给外部系统（OmniMerchant、承运商、对账系统）调用的，对方没有、也不该有
 * 本站的登录会话；此前它们既不在 {@code /api/**} 的拦截范围内、也没有任何角色注解，
 * 于是任何已登录用户都能查任意订单轨迹、把任意工单改到任意状态。
 *
 * <h3>认证方式</h3>
 * 请求头 {@code X-Api-Key: <api_key>}，取自 {@code api_key} 表且要求
 * {@code status = 1 AND is_deleted = 0}。校验通过后把 {@code app_name} 放入 request attribute，
 * 便于审计与排障。
 *
 * <h3>权限范围</h3>
 * 按请求路径要求对应的 {@code scope}（逗号分隔多值）：
 * <ul>
 *   <li>{@code GET /track/{orderNo}} → {@code track:read}</li>
 *   <li>{@code POST /workorder/callback} → {@code workorder:write}</li>
 * </ul>
 * 未在 {@link #requiredScope} 中声明的子路径不校验 scope，只要求凭证有效 —— 新增对外接口时
 * 应同步补上映射，否则等于默认放行。
 *
 * @author sentinel
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String API_APP_ATTR = "apiClientApp";

    private final ApiKeyService apiKeyService;

    @Autowired
    public ApiKeyInterceptor(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Map<String, Object> apiKey = apiKeyService.findActiveByKey(request.getHeader(API_KEY_HEADER));
        if (apiKey == null) {
            return reject(response, 401, "缺少或无效的 API Key（请求头 " + API_KEY_HEADER + "）");
        }
        String required = requiredScope(request);
        if (required != null && !ApiKeyService.hasScope(apiKey, required)) {
            return reject(response, 403, "该 API Key 未被授予 " + required + " 权限");
        }
        request.setAttribute(API_APP_ATTR, apiKey.get("app_name"));
        return true;
    }

    /** 路径 → 所需 scope；返回 null 表示该路径不校验 scope。 */
    private static String requiredScope(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        if (uri.contains("/track/")) {
            return "track:read";
        }
        if (uri.contains("/workorder/callback")) {
            return "workorder:write";
        }
        return null;
    }

    private static boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"msg\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
