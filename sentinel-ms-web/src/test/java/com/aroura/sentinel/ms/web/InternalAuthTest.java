package com.aroura.sentinel.ms.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份签名原语测试。
 * <p>
 * 这是整套服务间鉴权的根 —— 验签一旦可被绕过（或算错），身份头就退回到「谁都能伪造」。
 * 所以这里覆盖的不只是正常路径，还有各类篡改与边界输入。
 */
class InternalAuthTest {

    private static final String SECRET = "test-secret";
    private static final long SKEW = 300L;

    private static String nowSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }

    @Test
    void 正确签名应通过校验() {
        String ts = nowSeconds();
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", Long.parseLong(ts));
        assertTrue(InternalAuth.verify(SECRET, "张伟", "ADMIN", ts, sig, SKEW));
    }

    @Test
    void 换一个密钥应不通过() {
        String ts = nowSeconds();
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", Long.parseLong(ts));
        assertFalse(InternalAuth.verify("other-secret", "张伟", "ADMIN", ts, sig, SKEW));
    }

    /** 改了身份就必须失效，否则「签一次、随便改角色」即可提权。 */
    @Test
    void 篡改用户名或角色应不通过() {
        String ts = nowSeconds();
        long t = Long.parseLong(ts);
        String sig = InternalAuth.sign(SECRET, "张伟", "MERCHANT", t);
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", ts, sig, SKEW), "角色被改仍需通过校验？");
        assertFalse(InternalAuth.verify(SECRET, "李四", "MERCHANT", ts, sig, SKEW), "用户名被改仍需通过校验？");
    }

    /** 改了时间戳同样要失效，否则同一签名的重放窗口可被无限拉长。 */
    @Test
    void 篡改时间戳应不通过() {
        String ts = nowSeconds();
        long t = Long.parseLong(ts);
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", t);
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", String.valueOf(t + 1), sig, SKEW));
    }

    @Test
    void 超时的时间戳应不通过() {
        long stale = System.currentTimeMillis() / 1000L - SKEW - 10;
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", stale);
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", String.valueOf(stale), sig, SKEW));
    }

    /** 未来时间同样算越界，避免有人把时间戳往前推来延长有效期。 */
    @Test
    void 未来时间戳应不通过() {
        long future = System.currentTimeMillis() / 1000L + SKEW + 10;
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", future);
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", String.valueOf(future), sig, SKEW));
    }

    @Test
    void 空值与坏格式应安全失败而不是抛异常() {
        String ts = nowSeconds();
        String sig = InternalAuth.sign(SECRET, "张伟", "ADMIN", Long.parseLong(ts));
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", null, sig, SKEW));
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", ts, null, SKEW));
        assertFalse(InternalAuth.verify(SECRET, "张伟", "ADMIN", "not-a-number", sig, SKEW));
        assertFalse(InternalAuth.verify("", "张伟", "ADMIN", ts, sig, SKEW), "空密钥必须拒绝，不能当成无校验");
        assertFalse(InternalAuth.verify(null, "张伟", "ADMIN", ts, sig, SKEW));
    }

    /**
     * 角色为 null 与空串必须等价 —— 网关与下游对「无角色」的表达可能不同，
     * 若一侧用 null、另一侧用 ""，签名会对不上，表现为随机 401。
     */
    @Test
    void 角色为空与null应等价() {
        String ts = nowSeconds();
        long t = Long.parseLong(ts);
        assertEquals(InternalAuth.sign(SECRET, "u", null, t), InternalAuth.sign(SECRET, "u", "", t));
        String sig = InternalAuth.sign(SECRET, "u", null, t);
        assertTrue(InternalAuth.verify(SECRET, "u", "", ts, sig, SKEW));
    }

    /** 签名是 URL 安全的 base64（要去掉填充），否则放进 HTTP 头会带 = 号。 */
    @Test
    void 签名应为URL安全且无填充() {
        String sig = InternalAuth.sign(SECRET, "u", "ADMIN", 1L);
        assertFalse(sig.contains("="), "签名不应包含 base64 填充");
        assertFalse(sig.contains("+"), "签名不应包含 +");
        assertFalse(sig.contains("/"), "签名不应包含 /");
    }

    @Test
    void 不同输入应产生不同签名() {
        assertNotEquals(InternalAuth.sign(SECRET, "a", "ADMIN", 1L),
                InternalAuth.sign(SECRET, "b", "ADMIN", 1L));
    }
}
