package com.java3y.austin.web.controller.sentinel.tms;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.config.AuthInterceptor;
import com.java3y.austin.web.service.sentinel.tms.ApiKeyService;
import com.java3y.austin.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 开放 API 应用凭证接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/api-key")
@Api(tags = "开放 API")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping("/list")
    @ApiOperation("凭证列表")
    @RequireRole("ADMIN")
    public BasicResultVO list() {
        return BasicResultVO.success(apiKeyService.list());
    }

    @PostMapping("/create")
    @ApiOperation("创建应用凭证")
    @RequireRole("ADMIN")
    public BasicResultVO create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String createdBy = null;
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO) {
            createdBy = ((CurrentUserVO) attr).getUsername();
        }
        return BasicResultVO.success(apiKeyService.create(
                str(body.get("appName")),
                str(body.get("company")),
                str(body.get("contactName")),
                str(body.get("contactPhone")),
                str(body.get("contactEmail")),
                body.get("scope") == null ? null : String.valueOf(body.get("scope")),
                str(body.get("remark")),
                createdBy));
    }

    @PostMapping("/{id}/toggle")
    @ApiOperation("启用/停用")
    @RequireRole("ADMIN")
    public BasicResultVO toggle(@PathVariable Long id) {
        return BasicResultVO.success(apiKeyService.toggle(id));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("吊销凭证")
    @RequireRole("ADMIN")
    public BasicResultVO delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return BasicResultVO.success();
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o);
        return s.isEmpty() ? null : s;
    }
}