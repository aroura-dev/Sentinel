package com.java3y.austin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 依赖本地 MySQL/Redis 的全上下文测试，CI 无基建，禁用（保持 mvn test 可跑）
 */
@Disabled("requires local MySQL/Redis")
class MiniProgramTestController extends BaseTestController {

    private static final String BASE_URL = "/miniProgram";

    /**
     * 根据账号Id获取模板列表
     *
     * @throws Exception
     */
    @Test
    void queryList() throws Exception {
        //doRequest
        resultActions = mvc.perform(
                MockMvcRequestBuilders
                        .get(BASE_URL + "/template/list")
                        .headers(headers)
                        .param("id", "1")
        );
    }

}
