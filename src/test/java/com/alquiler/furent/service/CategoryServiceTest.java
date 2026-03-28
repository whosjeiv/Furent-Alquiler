package com.alquiler.furent.service;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Category;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CategoryService.
 * Valida la lógica de unicidad de nombres y verificación de productos asociados.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId("cat1");
        category1.setNombre("Sillas");
        category1.setDescripcion("Sillas para eventos");

        category2 = new Category();
        category2.setId("cat2");
        category2.setNombre("Mesas");
        category2.setDescripcion("Mesas para eventos");
    }

    @Test
    void validateUniqueName_shouldPassWhenNameIsUnique() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));

        // When & Then - no exception should be thrown
        assertDoesNotThrow(() -> categoryService.validateUniqueName("Decoración", ""));
    }

    @Test
    void validateUniqueName_shouldThrowExceptionWhenNameIsDuplicate() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));

        // When & Then
        InvalidOperationException exception = assertThrows(
            InvalidOperationException.class,
            () -> categoryService.validateUniqueName("Sillas", "")
        );
        
        assertTrue(exception.getMessage().contains("Ya existe una categoría con el nombre"));
    }

    @Test
    void validateUniqueName_shouldPassWhenEditingSameCategory() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));

        // When & Then - editing category1 with same name should pass
        assertDoesNotThrow(() -> categoryService.validateUniqueName("Sillas", "cat1"));
    }

    @Test
    void validateUniqueName_shouldBeCaseInsensitive() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));

        // When & Then
        InvalidOperationException exception = assertThrows(
            InvalidOperationException.class,
            () -> categoryService.validateUniqueName("SILLAS", "")
        );
        
        assertTrue(exception.getMessage().contains("Ya existe una categoría con el nombre"));
    }

    @Test
    void canDelete_shouldReturnTrueWhenNoProductsAssociated() {
        // Given
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(category1));
        when(productRepository.findByCategoriaNombre("Sillas")).thenReturn(Collections.emptyList());

        // When
        boolean result = categoryService.canDelete("cat1");

        // Then
        assertTrue(result);
    }

    @Test
    void canDelete_shouldReturnFalseWhenProductsAssociated() {
        // Given
        Product product = new Product();
        product.setId("prod1");
        product.setCategoriaNombre("Sillas");

        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(category1));
        when(productRepository.findByCategoriaNombre("Sillas")).thenReturn(Arrays.asList(product));

        // When
        boolean result = categoryService.canDelete("cat1");

        // Then
        assertFalse(result);
    }

    @Test
    void delete_shouldSucceedWhenNoProductsAssociated() {
        // Given
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(category1));
        when(productRepository.findByCategoriaNombre("Sillas")).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> categoryService.delete("cat1"));
        verify(categoryRepository).deleteById("cat1");
    }

    @Test
    void delete_shouldThrowExceptionWhenProductsAssociated() {
        // Given
        Product product = new Product();
        product.setId("prod1");
        product.setCategoriaNombre("Sillas");

        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(category1));
        when(productRepository.findByCategoriaNombre("Sillas")).thenReturn(Arrays.asList(product));

        // When & Then
        InvalidOperationException exception = assertThrows(
            InvalidOperationException.class,
            () -> categoryService.delete("cat1")
        );
        
        assertTrue(exception.getMessage().contains("tiene productos asociados"));
        verify(categoryRepository, never()).deleteById(anyString());
    }

    @Test
    void getByIdOrThrow_shouldReturnCategoryWhenExists() {
        // Given
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(category1));

        // When
        Category result = categoryService.getByIdOrThrow("cat1");

        // Then
        assertNotNull(result);
        assertEquals("Sillas", result.getNombre());
    }

    @Test
    void getByIdOrThrow_shouldThrowExceptionWhenNotExists() {
        // Given
        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> categoryService.getByIdOrThrow("nonexistent"));
    }
}
