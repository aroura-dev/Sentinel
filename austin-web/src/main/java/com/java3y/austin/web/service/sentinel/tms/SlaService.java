package com.java3y.austin.web.service.sentinel.tms;

import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * 履约 SLA/ETA 服务
 *
 * @author sentinel
 */
@Service
public class SlaService {

    /**
     * 承诺妥投 ETA = base + transitDaysMax 天
     */
    public Date computeEta(int transitDaysMax, Date base) {
        Calendar c = Calendar.getInstance();
        c.setTime(base);
        c.add(Calendar.DAY_OF_YEAR, transitDaysMax);
        return c.getTime();
    }

    /**
     * 按渠道承诺时效评估 SLA 状态：
     * elapsed ≥ threshold → BREACHED；(0.8, 1.0)× → RISK；否则 NORMAL
     */
    public String evaluate(int transitDaysMax, long elapsedHours) {
        long threshold = (long) transitDaysMax * 24;
        if (elapsedHours >= threshold) {
            return "BREACHED";
        }
        if (elapsedHours >= threshold * 0.8) {
            return "RISK";
        }
        return "NORMAL";
    }
}
