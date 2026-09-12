package com.aroura.sentinel.service.api.impl.service;

import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.pipeline.ProcessController;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.service.api.domain.MessageParam;
import com.aroura.sentinel.service.api.domain.SendRequest;
import com.aroura.sentinel.service.api.domain.SendResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SendServiceImpl：messageParam 缺失时返回明确错误而非 NPE；正常参数走责任链
 */
@ExtendWith(MockitoExtension.class)
public class SendServiceImplTest {

    @Mock
    private ProcessController processController;
    @InjectMocks
    private SendServiceImpl sendService;

    @Test
    void nullMessageParamReturnsBadParameters() {
        SendRequest req = SendRequest.builder().code("send").messageTemplateId(1L).build();

        SendResponse resp = sendService.send(req);

        assertNotNull(resp);
        assertEquals(RespStatusEnum.CLIENT_BAD_PARAMETERS.getCode(), resp.getCode());
        verify(processController, never()).process(any());
    }

    @Test
    void validRequestInvokesPipeline() {
        SendRequest req = SendRequest.builder().code("send").messageTemplateId(1L)
                .messageParam(MessageParam.builder().receiver("test@example.com").build())
                .build();
        when(processController.process(any())).thenAnswer(inv -> {
            ProcessContext ctx = inv.getArgument(0);
            ctx.setResponse(BasicResultVO.success());
            return ctx;
        });

        SendResponse resp = sendService.send(req);

        assertNotNull(resp);
        assertEquals(RespStatusEnum.SUCCESS.getCode(), resp.getCode());
        verify(processController, times(1)).process(any());
    }
}