package com.alquiler.furent.repository;

import com.alquiler.furent.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUserIdAndUsedFalse(String userId);
    List<PasswordResetToken> findByUserId(String userId);
}
