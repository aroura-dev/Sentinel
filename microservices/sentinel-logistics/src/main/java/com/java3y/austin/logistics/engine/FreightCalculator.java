package com.java3y.austin.logistics.engine;

import com.java3y.austin.logistics.dao.tms.CarrierChannelDao;
import com.java3y.austin.logistics.dao.tms.CarrierRateDao;
import com.java3y.austin.logistics.model.tms.FreightQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

/**
 * 运费核算引擎
 * <p>
 * 计费重量 = max(实重, 体积重=体积L / 渠道体积系数)；低于渠道最小计费重按最小计费重。
 * 计费方式：PER_KG 单价 | FIRST_CONTINUED 首续重。
 *
 * @author sentinel
 */
@Component
public class FreightCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CarrierChannelDao channelDao;
    private final CarrierRateDao rateDao;

    public FreightCalculator(CarrierChannelDao channelDao, CarrierRateDao rateDao) {
        this.channelDao = channelDao;
        this.rateDao = rateDao;
    }

    /**
     * 计算运费（不落库）。无价卡抛 {@link FreightNoRateException}
     */
    public FreightQuote quote(Long channelId, BigDecimal weightKg, BigDecimal volumeL, String destCountry) {
        Map<String, Object> channel = channelDao.findById(channelId);
        if (channel == null) {
            throw new FreightNoRateException("渠道不存在: id=" + channelId);
        }
        Integer volDivisor = toInt(channel.get("vol_divisor"), 5000);
        BigDecimal minBillable = toDecimal(channel.get("min_billable_weight_kg"), ZERO);

        BigDecimal weight = weightKg == null ? ZERO : weightKg;
        BigDecimal volumeWeight = volumeL == null ? ZERO
                : volumeL.divide(BigDecimal.valueOf(volDivisor), 3, RoundingMode.HALF_UP);
        BigDecimal billable = weight.compareTo(volumeWeight) > 0 ? weight : volumeWeight;
        if (billable.compareTo(minBillable) < 0) {
            billable = minBillable;
        }

        Map<String, Object> rate = rateDao.selectRate(channelId, destCountry, billable, new Date());
        if (rate == null) {
            throw new FreightNoRateException("无价卡: 渠道=" + channelId + " 区域=" + destCountry + " 计费重=" + billable + "kg");
        }

        String mode = String.valueOf(rate.get("mode"));
        BigDecimal freight;
        if ("FIRST_CONTINUED".equals(mode)) {
            BigDecimal firstW = toDecimal(rate.get("first_weight_kg"), ZERO);
            BigDecimal firstP = toDecimal(rate.get("first_price"), ZERO);
            BigDecimal contW = toDecimal(rate.get("continued_weight_kg"), ONE);
            BigDecimal contP = toDecimal(rate.get("continued_price"), ZERO);
            if (billable.compareTo(firstW) <= 0) {
                freight = firstP;
            } else {
                BigDecimal units = billable.subtract(firstW)
                        .divide(contW, 0, RoundingMode.CEILING);
                freight = firstP.add(units.multiply(contP));
            }
        } else {
            BigDecimal price = toDecimal(rate.get("price"), ZERO);
            freight = billable.multiply(price);
        }

        return FreightQuote.builder()
                .billableWeight(billable)
                .zone(String.valueOf(rate.get("zone")))
                .mode(mode)
                .price("FIRST_CONTINUED".equals(mode)
                        ? toDecimal(rate.get("first_price"), ZERO)
                        : toDecimal(rate.get("price"), ZERO))
                .currency(String.valueOf(rate.get("currency")))
                .freight(freight.setScale(2, RoundingMode.HALF_UP))
                .rateId(toLong(rate.get("id")))
                .channelCode(String.valueOf(channel.get("channel_code")))
                .channelName(String.valueOf(channel.get("channel_name")))
                .build();
    }

    private static Integer toInt(Object v, Integer def) {
        return v == null ? def : Integer.valueOf(String.valueOf(v));
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static BigDecimal toDecimal(Object v, BigDecimal def) {
        return v == null ? def : new BigDecimal(String.valueOf(v));
    }
}
