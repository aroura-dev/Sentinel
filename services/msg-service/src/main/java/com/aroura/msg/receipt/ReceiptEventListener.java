package com.aroura.msg.receipt;

import com.aroura.sentinel.common.domain.NotificationReceipt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听发送链路发布的投递回执，转成对 logistics-service 的回调。
 * <p>
 * 分成「共享库发事件 + 本服务监听」两步，而不是在 SmsHandler 里直接调 logistics：
 * SmsHandler 在共享库里，不该依赖某个业务服务的客户端；而且回执只有本服务关心，
 * 别的 MQ 实现没有这条链路，事件没人监听时自然是个空操作。
 *
 * @author sentinel-ms
 */
@Component
public class ReceiptEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReceiptEventListener.class);

    @Autowired
    private LogisticsReceiptClient receiptClient;

    /**
     * 异步处理：事件是在发送线程里发布的，同步回调会把 logistics 的网络往返
     * 叠加到每一次短信发送上。回执是辅助信息，不该左右主链路的耗时。
     * <p>
     * 失败只记日志、不抛出：回执丢失会让该记录停在 SENT（即当前行为），
     * 而抛出异常会打断发送链路 —— 拿主线稳定换一条辅助信息不值得。
     */
    @Async
    @EventListener
    public void onReceipt(NotificationReceipt receipt) {
        if (receipt == null || receipt.getBizId() == null) {
            return;
        }
        try {
            receiptClient.report(new ReceiptRequest(
                    receipt.getBizId(), receipt.isAccepted(), receipt.getDetail()));
        } catch (Exception e) {
            // 启用 Feign 熔断且未配 fallback 时，任何失败都被包装成 NoFallbackAvailableException
            // （熔断打开时亦然，cause 才是 CallNotPermittedException）。
            // 对本方法无影响：一视同仁地只记日志 —— 回执丢失、记录停在 SENT，与不开熔断时一致。
            log.warn("[Receipt] 回执上报失败 bizId={} accepted={} err={}",
                    receipt.getBizId(), receipt.isAccepted(), e.getMessage());
        }
    }
}
