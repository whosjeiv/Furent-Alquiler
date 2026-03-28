package com.alquiler.furent.repository;

import com.alquiler.furent.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    
    // Métodos para filtrado y búsqueda de usuarios
    Page<User> findByRole(String role, Pageable pageable);
    
    Page<User> findByEstado(String estado, Pageable pageable);
    
    Page<User> findByRoleAndEstado(String role, String estado, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseOrNombreContainingIgnoreCase(
            String email, String nombre, Pageable pageable);
}
