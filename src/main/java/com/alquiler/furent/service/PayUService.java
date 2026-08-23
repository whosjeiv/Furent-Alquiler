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
    
    public String generateConfirmationSignature(String referenceCode, BigDecimal amount, String currency, int statePol) {
        // Firma de confirmación Webhook: MD5(ApiKey~merchant_id~reference_sale~new_value~currency~state_pol)
        // El new_value debe tener las posiciones decimales de acuerdo a lo reportado en tv_value de confirmación.
        // PayU recomienda usar un formato específico para el monto.
        
        // Formatear monto: si es entero (como 150000), dejar como 150000.0 si PayU lo manda así, 
        // pero usualmente PayU manda el valor exacto con un decimal si es .0
        String amountFormatted = amount.setScale(1, RoundingMode.HALF_UP).toString();
        if (amountFormatted.endsWith(".0")) {
            // A veces PayU lo manda sin el .0 en la firma v4.0, pero en v2 es con .1
            // Intentaremos coincidir con el valor que viene en el request.
        }

        String input = String.format("%s~%s~%s~%s~%s~%d", 
                payUProperties.getApiKey(), 
                payUProperties.getMerchantId(), 
                referenceCode, 
                amount.setScale(1, RoundingMode.HALF_UP).toString(), 
                currency,
                statePol);
        
        return md5(input);
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
