package com.java3y.austin.web.controller.sentinel;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.logistics.dao.KnowledgeDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常知识库接口（PRD 8.6）
 * <p>
 * 修复：原接口仅存在于前端 Mock，后端补齐并落库 anomaly_knowledge。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/knowledge")
@Api(tags = "Sentinel 知识库接口")
@RequireRole({"ADMIN"})
public class SentinelKnowledgeController {

    @Autowired
    private KnowledgeDao knowledgeDao;

    @GetMapping("/list")
    @ApiOperation("知识库分页")
    public BasicResultVO list(@RequestParam(required = false) String type,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(knowledgeDao.queryPage(type, page, perPage));
    }

    @PostMapping("/save")
    @ApiOperation("保存知识库（新增或更新）")
    public BasicResultVO save(@RequestParam(required = false) Long id,
                              @RequestParam String statusCode,
                              @RequestParam String type,
                              @RequestParam String description,
                              @RequestParam(required = false) Integer avgDurationHours,
                              @RequestParam(required = false) String suggestion) {
        knowledgeDao.save(id, statusCode, type, description, avgDurationHours, suggestion);
        return BasicResultVO.success(true);
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除知识库（逻辑删除）")
    public BasicResultVO delete(@PathVariable Long id) {
        knowledgeDao.deleteById(id);
        return BasicResultVO.success(true);
    }
}