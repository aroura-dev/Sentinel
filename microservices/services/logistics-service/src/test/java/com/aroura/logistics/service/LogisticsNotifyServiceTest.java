package com.aroura.logistics.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.aroura.logistics.client.AgentClient;
import com.aroura.logistics.client.MsgClient;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.NotificationDao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知投递与补偿记账测试。
 * <p>
 * 这里盯的是<b>状态语义</b>：SENT / SKIPPED / FAILED 三者不能混。
 * 尤其是「没有接收方」必须落成 SKIPPED —— 若留在 PENDING，补偿任务会把它当成
 * 崩溃遗留的记录反复重投。
 */
@ExtendWith(MockitoExtension.class)
class LogisticsNotifyServiceTest {

    private static final String ORDER_NO = "OMT-TEST-0001";
    private static final long ROW_ID = 100L;

    @Mock
    private NotificationDao notificationDao;
    @Mock
    private LogisticsDao logisticsDao;
    @Mock
    private AgentClient agentClient;
    @Mock
    private MsgClient msgClient;

    @InjectMocks
    private LogisticsNotifyService notifyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifyService, "dispatchEnabled", true);
        ReflectionTestUtils.setField(notifyService, "dispatchTemplateId", 3L);
        ReflectionTestUtils.setField(notifyService, "dedupHours", 24);
        ReflectionTestUtils.setField(notifyService, "maxAttempts", 5);
        ReflectionTestUtils.setField(notifyService, "baseBackoffSeconds", 30L);
    }

    /** 订单存在且带合法手机号。 */
    private void givenOrderWithPhone(String phone) {
        Map<String, Object> order = new HashMap<>(4);
        order.put("buyer_phone", phone);
        order.put("buyer_id", "王芳");
        order.put("buyer_language", "zh");
        when(logisticsDao.findOrderByNo(ORDER_NO)).thenReturn(order);
        when(agentClient.generate(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("您的订单已更新");
        when(notificationDao.insert(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(ROW_ID);
    }

    @Test
    void 投递成功应标记SENT() {
        givenOrderWithPhone("13800000001");
        when(msgClient.send(anyLong(), anyString(), anyString(), anyString())).thenReturn(true);

        notifyService.send(ORDER_NO, "IN_TRANSIT", "buyer", "sms");

        verify(notificationDao).markSent(ROW_ID);
        verify(notificationDao, never()).markFailed(anyLong(), anyString(), any());
        verify(notificationDao, never()).markSkipped(anyLong(), anyString());
    }

    /** 失败要带上退避时间，否则补偿任务无从判断何时该重投。 */
    @Test
    void 投递失败应记录原因并安排退避() {
        givenOrderWithPhone("13800000001");
        when(msgClient.send(anyLong(), anyString(), anyString(), anyString())).thenReturn(false);
        when(notificationDao.queryById(ROW_ID)).thenReturn(row(0));

        notifyService.send(ORDER_NO, "IN_TRANSIT", "buyer", "sms");

        ArgumentCaptor<Timestamp> next = ArgumentCaptor.forClass(Timestamp.class);
        verify(notificationDao).markFailed(eq(ROW_ID), anyString(), next.capture());
        assertNotNull(next.getValue(), "首次失败应安排下一次重试时间");
        assertTrue(next.getValue().getTime() > System.currentTimeMillis(), "重试时间应在未来");
    }

    /** 到达上限后必须停止，否则一条坏消息会被永远重投。 */
    @Test
    void 达到重试上限应停止补偿() {
        givenOrderWithPhone("13800000001");
        when(msgClient.send(anyLong(), anyString(), anyString(), anyString())).thenReturn(false);
        when(notificationDao.queryById(ROW_ID)).thenReturn(row(4));  // 已失败 4 次，maxAttempts=5

        notifyService.send(ORDER_NO, "IN_TRANSIT", "buyer", "sms");

        ArgumentCaptor<Timestamp> next = ArgumentCaptor.forClass(Timestamp.class);
        verify(notificationDao).markFailed(eq(ROW_ID), anyString(), next.capture());
        assertNull(next.getValue(), "达到上限应清空重试时间，退出补偿循环");
    }

    /** 没有接收方是「本就不该发」，必须是 SKIPPED —— 留在 PENDING 会被补偿任务反复重投。 */
    @Test
    void 无接收方应标记SKIPPED而不是留PENDING() {
        Map<String, Object> order = new HashMap<>(2);
        order.put("buyer_phone", "");
        order.put("buyer_id", null);
        when(logisticsDao.findOrderByNo(ORDER_NO)).thenReturn(order);
        when(agentClient.generate(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("文案");
        when(notificationDao.insert(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(ROW_ID);

        notifyService.send(ORDER_NO, "IN_TRANSIT", "buyer", "sms");

        verify(notificationDao).markSkipped(eq(ROW_ID), anyString());
        verify(msgClient, never()).send(anyLong(), anyString(), anyString(), anyString());
    }

    /** 去重命中时不应再落一行 —— 否则每次调用都会新增记录。 */
    @Test
    void 去重命中应直接跳过() {
        when(notificationDao.existsRecent(ORDER_NO, "IN_TRANSIT", "buyer", 24)).thenReturn(true);

        assertNull(notifyService.send(ORDER_NO, "IN_TRANSIT", "buyer", "sms"));
        verify(notificationDao, never()).insert(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }

    /** 补偿重投走同一条投递路径；接收方按订单现况重新推导。 */
    @Test
    void 补偿重投应重新解析接收方() {
        Map<String, Object> order = new HashMap<>(2);
        order.put("buyer_phone", "13900000002");
        order.put("buyer_id", "李四");
        when(logisticsDao.findOrderByNo(ORDER_NO)).thenReturn(order);
        when(msgClient.send(eq(3L), anyString(), eq("13900000002"), eq(ORDER_NO))).thenReturn(true);

        Map<String, Object> failed = row(1);
        failed.put("order_no", ORDER_NO);
        failed.put("node", "IN_TRANSIT");
        failed.put("role", "buyer");
        failed.put("language", "zh");
        notifyService.retryOne(failed);

        verify(notificationDao).markSent(ROW_ID);
    }

    private Map<String, Object> row(int retryCount) {
        Map<String, Object> row = new HashMap<>(4);
        row.put("id", ROW_ID);
        row.put("retry_count", retryCount);
        return row;
    }
}
