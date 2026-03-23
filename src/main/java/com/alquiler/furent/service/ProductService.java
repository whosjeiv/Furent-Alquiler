package com.alquiler.furent.service;

import com.alquiler.furent.model.Category;
import com.alquiler.furent.model.Combo;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.ComboRepository;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Servicio de gestión de productos (mobiliarios), categorías y combos.
 * Incluye caching Redis para consultas frecuentes y métricas de rendimiento.
 *
 * Caches utilizados:
 * - "products": lista completa de productos
 * - "product-detail": detalle individual por ID
 * - "featured-products": productos destacados
 * - "categories": lista de categorías
 * - "product-count": conteo de productos
 * - "combos": lista completa de combos
 * - "active-combos": combos activos (públicos)
 *
 * @author Furent Team
 * @since 2.0
 */
@Service
public class ProductService {

        private static final Logger log = LoggerFactory.getLogger(ProductService.class);

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final ComboRepository comboRepository;

        public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                        ComboRepository comboRepository) {
                this.productRepository = productRepository;
                this.categoryRepository = categoryRepository;
                this.comboRepository = comboRepository;
        }

        // ═══════════════════════════════════════════════════════
        // PRODUCTS - con Cache Redis
        // ═══════════════════════════════════════════════════════

        @Cacheable(value = "products", unless = "#result.isEmpty()")
        public List<Product> getAllProducts() {
                log.debug("Cache MISS: cargando todos los productos desde MongoDB");
                return productRepository.findAll();
        }

        @Cacheable(value = "featured-products", unless = "#result.isEmpty()")
        public List<Product> getFeaturedProducts() {
                log.debug("Cache MISS: cargando productos destacados desde MongoDB");
                return productRepository.findByDisponibleTrue()
                                .stream()
                                .sorted(java.util.Comparator.comparingDouble(Product::getCalificacion).reversed())
                                .limit(6)
                                .collect(Collectors.toList());
        }

        @Cacheable(value = "product-detail", key = "#id")
        public Optional<Product> getProductById(String id) {
                log.debug("Cache MISS: cargando producto {} desde MongoDB", id);
                return productRepository.findById(id);
        }

        public Product getProductByIdOrThrow(String id) {
                return productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        }

        public List<Product> getProductsByCategory(String category) {
                return productRepository.findByCategoriaNombre(category);
        }

        public Page<Product> getProductsPaginated(String category, int page, int size) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
                if (category != null && !category.isBlank()) {
                        return productRepository.findByCategoriaNombre(category, pageable);
                }
                return productRepository.findAll(pageable);
        }

        public List<Product> searchProducts(String keyword) {
                if (keyword == null || keyword.isBlank()) {
                        return getAllProducts();
                }
                // Escape regex special characters to prevent injection
                String escaped = Pattern.quote(keyword.trim());
                return productRepository.searchProducts(escaped);
        }

        public List<Product> getProductsByIds(List<String> ids) {
                if (ids == null || ids.isEmpty()) return List.of();
                return productRepository.findByIdIn(ids);
        }

        public List<Product> getRelatedProducts(String productId, String category) {
                List<Product> sameCategory = productRepository.findByCategoriaNombre(category).stream()
                                .filter(p -> !p.getId().equals(productId))
                                .collect(Collectors.toCollection(ArrayList::new));

                if (sameCategory.size() >= 8) {
                        return sameCategory.stream().limit(8).toList();
                }

                List<Product> allProducts = productRepository.findAll();
                List<Product> others = allProducts.stream()
                                .filter(p -> !p.getId().equals(productId) && !p.getCategoriaNombre().equals(category))
                                .limit(8 - sameCategory.size())
                                .toList();

                sameCategory.addAll(others);
                return sameCategory;
        }

        @Caching(evict = {
                @CacheEvict(value = "products", allEntries = true),
                @CacheEvict(value = "featured-products", allEntries = true),
                @CacheEvict(value = "product-detail", key = "#product.id", condition = "#product.id != null"),
                @CacheEvict(value = "product-count", allEntries = true)
        })
        public Product saveProduct(Product product) {
                log.info("Guardando producto: {} - invalidando caches", product.getNombre());
                return productRepository.save(product);
        }

        @Caching(evict = {
                @CacheEvict(value = "products", allEntries = true),
                @CacheEvict(value = "featured-products", allEntries = true),
                @CacheEvict(value = "product-detail", key = "#id"),
                @CacheEvict(value = "product-count", allEntries = true)
        })
        public void deleteProduct(String id) {
                productRepository.deleteById(id);
                log.info("Producto eliminado: {} - invalidando caches", id);
        }

        @Cacheable(value = "product-count")
        public long countProducts() {
                return productRepository.count();
        }

        // ═══════════════════════════════════════════════════════
        // CATEGORIES - con Cache Redis
        // ═══════════════════════════════════════════════════════

        @Cacheable(value = "categories", unless = "#result.isEmpty()")
        public List<Category> getAllCategories() {
                log.debug("Cache MISS: cargando categorías desde MongoDB");
                return categoryRepository.findAll();
        }

        public Optional<Category> getCategoryById(String id) {
                return categoryRepository.findById(id);
        }

        @CacheEvict(value = "categories", allEntries = true)
        public Category saveCategory(Category category) {
                log.info("Guardando categoría: {} - invalidando cache", category.getNombre());
                return categoryRepository.save(category);
        }

        @CacheEvict(value = "categories", allEntries = true)
        public void deleteCategory(String id) {
                // Verificar que no tenga productos asociados
                Optional<Category> cat = categoryRepository.findById(id);
                if (cat.isPresent()) {
                        List<Product> products = productRepository.findByCategoriaNombre(cat.get().getNombre());
                        if (!products.isEmpty()) {
                                throw new com.alquiler.furent.exception.InvalidOperationException(
                                        "No se puede eliminar la categoría porque tiene " + products.size() + " productos asociados");
                        }
                }
                categoryRepository.deleteById(id);
                log.info("Categoría eliminada: {} - invalidando cache", id);
        }

        public long countCategories() {
                return categoryRepository.count();
        }

        // ═══════════════════════════════════════════════════════
        // COMBOS - con Cache Redis
        // ═══════════════════════════════════════════════════════

        @Cacheable(value = "combos", unless = "#result.isEmpty()")
        public List<Combo> getAllCombos() {
                log.debug("Cache MISS: cargando todos los combos desde MongoDB");
                return comboRepository.findAll();
        }

        @Cacheable(value = "active-combos", unless = "#result.isEmpty()")
        public List<Combo> getActiveCombos() {
                log.debug("Cache MISS: cargando combos activos desde MongoDB");
                return comboRepository.findByActivoTrue();
        }

        public Optional<Combo> getComboById(String id) {
                return comboRepository.findById(id);
        }

        @Caching(evict = {
                @CacheEvict(value = "combos", allEntries = true),
                @CacheEvict(value = "active-combos", allEntries = true)
        })
        public Combo saveCombo(Combo combo) {
                log.info("Guardando combo: {} - invalidando caches", combo.getNombre());
                return comboRepository.save(combo);
        }

        @Caching(evict = {
                @CacheEvict(value = "combos", allEntries = true),
                @CacheEvict(value = "active-combos", allEntries = true)
        })
        public void deleteCombo(String id) {
                comboRepository.deleteById(id);
                log.info("Combo eliminado: {} - invalidando caches", id);
        }

        public long countCombos() {
                return comboRepository.count();
        }
}
