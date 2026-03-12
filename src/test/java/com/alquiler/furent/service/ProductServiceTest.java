package com.alquiler.furent.service;

import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private ProductService productService;

    @Test
    void getFeaturedProducts_shouldReturnTop6ByRating() {
        List<Product> products = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product p = new Product();
            p.setNombre("Producto " + i);
            p.setCalificacion(i * 0.5);
            p.setDisponible(true);
            products.add(p);
        }
        when(productRepository.findByDisponibleTrue()).thenReturn(products);

        List<Product> featured = productService.getFeaturedProducts();

        assertEquals(6, featured.size());
        assertTrue(featured.get(0).getCalificacion() >= featured.get(1).getCalificacion());
    }

    @Test
    void getProductByIdOrThrow_withValidId_shouldReturn() {
        Product p = new Product();
        p.setId("p1");
        p.setNombre("Silla");
        when(productRepository.findById("p1")).thenReturn(Optional.of(p));

        Product result = productService.getProductByIdOrThrow("p1");

        assertEquals("Silla", result.getNombre());
    }

    @Test
    void getProductByIdOrThrow_withInvalidId_shouldThrow() {
        when(productRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductByIdOrThrow("xxx"));
    }

    @Test
    void searchProducts_shouldEscapeRegex() {
        when(productRepository.searchProducts("\\Qsilla\\E")).thenReturn(List.of());

        List<Product> results = productService.searchProducts("silla");

        assertNotNull(results);
        verify(productRepository).searchProducts("\\Qsilla\\E");
    }

    @Test
    void searchProducts_withNull_shouldReturnAll() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<Product> results = productService.searchProducts(null);

        assertNotNull(results);
        verify(productRepository).findAll();
    }

    @Test
    void getProductsByIds_withEmptyList_shouldReturnEmpty() {
        List<Product> results = productService.getProductsByIds(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void getProductsByIds_withNull_shouldReturnEmpty() {
        List<Product> results = productService.getProductsByIds(null);
        assertTrue(results.isEmpty());
    }
}
