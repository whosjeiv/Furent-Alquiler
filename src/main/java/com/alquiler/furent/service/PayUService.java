package com.alquiler.furent.service;

import com.alquiler.furent.config.PayUProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PayUService {

    private final PayUProperties payUProperties;

    public PayUService(PayUProperties payUProperties) {
        this.payUProperties = payUProperties;
    }

    public String generateSignature(String referenceCode, BigDecimal amount, String currency) {
        // En PayU, la firma es MD5(ApiKey~merchantId~referenceCode~tx_value~currency)
        // tx_value: It is recommended to include the first decimal if ending in zero (e.g. 100.0) but formatting must match the HTML form value EXACTLY.
        // We will format amount without decimals or with standard format.
        String amountFormatted = amount.setScale(0, RoundingMode.HALF_UP).toString();
        
        String input = String.format("%s~%s~%s~%s~%s", 
                payUProperties.getApiKey(), 
                payUProperties.getMerchantId(), 
                referenceCode, 
                amountFormatted, 
                currency);
        
        return md5(input);
    }
    
    public String generateConfirmationSignature(String referenceCode, BigDecimal amount, String currency, String statePol) {
        // Firma de confirmación Webhook: MD5(ApiKey~merchant_id~reference_sale~new_value~currency~state_pol)
        // El new_value debe tener las posiciones decimales de acuerdo a lo reportado en tv_value de confirmación,
        // pero vamos a usar un truncamiento seguro o el amount tal cual viene en la solicitud para validarlo.
        return null; // Will implement properly in webhook check
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 no soportado", e);
        }
    }
    
    public String getAmountFormatted(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toString();
    }
}
