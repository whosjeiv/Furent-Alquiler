package com.alquiler.furent.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpService.class);
    private static final String ISSUER = "Furent";

    /** Genera una nueva clave secreta TOTP de base32 */
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    /**
     * Genera la URI de datos (data:image/png;base64,...) del QR para registrar
     * la cuenta en Google Authenticator.
     */
    public String generateQrDataUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData;
        try {
            imageData = generator.generate(data);
        } catch (QrGenerationException e) {
            log.error("Error generando QR TOTP: {}", e.getMessage());
            throw new RuntimeException("No se pudo generar el código QR");
        }
        return getDataUriForImage(imageData, generator.getImageMimeType());
    }

    /**
     * Verifica que el código de 6 dígitos ingresado por el usuario coincida
     * con el secreto almacenado. Acepta el código actual y los dos
     * adyacentes (±30 s) para tolerar desfases de reloj.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) return false;
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }
}
