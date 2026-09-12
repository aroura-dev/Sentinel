package com.aroura.sentinel.web.controller.sentinel;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.TemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模板管理接口（薄控制器，业务逻辑在 {@link TemplateService}）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/template")
@Api(tags = "Sentinel 模板接口（sentinel 别名）")
@RequireRole({"ADMIN"})
public class SentinelTemplateController {

    private final TemplateService templateService;

    public SentinelTemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/list")
    @ApiOperation("模板分页")
    public BasicResultVO list(@RequestParam(required = false) String keywords,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(templateService.list(keywords, page, perPage));
    }

    @PostMapping("/save")
    @ApiOperation("保存模板（新增或更新）")
    public BasicResultVO save(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(templateService.save(body));
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除模板（逻辑删除）")
    public BasicResultVO delete(@PathVariable Long id) {
        templateService.delete(id);
        return BasicResultVO.success(true);
    }
}
