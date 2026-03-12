package com.alquiler.furent.repository;

import com.alquiler.furent.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);
    void deleteByUserId(String userId);
}
