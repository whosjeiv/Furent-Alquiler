package com.alquiler.furent.repository;

import com.alquiler.furent.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String> {
}
