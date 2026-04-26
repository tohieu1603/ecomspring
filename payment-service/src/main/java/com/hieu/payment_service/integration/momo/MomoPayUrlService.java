package com.hieu.payment_service.integration.momo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generates MoMo sandbox redirect URLs for e-wallet payments.
 */
@Service
public class MomoPayUrlService {

    @Value("${momo.partner-code:MOMO_DEMO}")
    private String partnerCode;

    @Value("${momo.sandbox-base-url:https://test-payment.momo.vn/v2/gateway/pay}")
    private String sandboxBaseUrl;

    @Value("${momo.return-url:http://localhost:3000/payment/momo/return}")
    private String returnUrl;

    public String generatePayUrl(String orderId, BigDecimal amount) {
        String requestId = UUID.randomUUID().toString();
        String raw = orderId + amount + requestId;
        String sig = HexFormat.of()
                .formatHex(digest(raw.getBytes(StandardCharsets.UTF_8)))
                .substring(0, 16);
        return sandboxBaseUrl
                + "?partnerCode=" + partnerCode
                + "&orderId=" + orderId
                + "&requestId=" + requestId
                + "&amount=" + amount
                + "&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8)
                + "&sig=" + sig;
    }

    private static byte[] digest(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String getPartnerCode() { return partnerCode; }
}
