package com.java3y.austin.web.controller.sentinel;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.service.sentinel.ChannelService;
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
 * 渠道账号接口（薄控制器，业务逻辑在 {@link ChannelService}）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/channel")
@Api(tags = "Sentinel 渠道账号接口（austin 别名）")
@RequireRole({"ADMIN"})
public class SentinelChannelController {

    private final ChannelService channelService;

    public SentinelChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("/list")
    @ApiOperation("渠道账号分页")
    public BasicResultVO list(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(channelService.list(page, perPage));
    }

    @PostMapping("/save")
    @ApiOperation("保存渠道账号")
    public BasicResultVO save(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(channelService.save(body));
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除渠道账号（逻辑删除）")
    public BasicResultVO delete(@PathVariable Long id) {
        channelService.delete(id);
        return BasicResultVO.success(true);
    }
}
