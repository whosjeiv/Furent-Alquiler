package com.alquiler.furent.repository;

import com.alquiler.furent.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByNombre(String nombre);
}
