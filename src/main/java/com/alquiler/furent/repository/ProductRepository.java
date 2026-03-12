package com.alquiler.furent.repository;

import com.alquiler.furent.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoriaNombre(String categoriaNombre);

    Page<Product> findByCategoriaNombre(String categoriaNombre, Pageable pageable);

    List<Product> findByDisponibleTrue();

    Page<Product> findAll(Pageable pageable);

    Page<Product> findByDisponibleTrue(Pageable pageable);

    @Query("{ $or: [ " +
           "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
           "{ 'descripcion': { $regex: ?0, $options: 'i' } }, " +
           "{ 'categoriaNombre': { $regex: ?0, $options: 'i' } }, " +
           "{ 'material': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Product> searchProducts(String keyword);

    List<Product> findByIdIn(List<String> ids);
}
