package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Category;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de categorías de productos.
 * Implementa validaciones de unicidad de nombres y verificación de productos asociados.
 *
 * @author Furent Team
 * @since 2.0
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Valida que el nombre de la categoría sea único.
     * 
     * @param nombre Nombre de la categoría a validar
     * @param excludeId ID de categoría a excluir de la validación (para edición)
     * @throws InvalidOperationException si el nombre ya existe
     */
    public void validateUniqueName(String nombre, String excludeId) {
        List<Category> categories = categoryRepository.findAll();
        
        boolean duplicateFound = categories.stream()
            .filter(c -> !c.getId().equals(excludeId))
            .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre));
        
        if (duplicateFound) {
            throw new InvalidOperationException("Ya existe una categoría con el nombre: " + nombre);
        }
    }

    /**
     * Verifica si una categoría puede ser eliminada.
     * Una categoría solo puede eliminarse si no tiene productos asociados.
     * 
     * @param categoryId ID de la categoría
     * @return true si puede eliminarse, false si tiene productos asociados
     */
    public boolean canDelete(String categoryId) {
        Category category = getByIdOrThrow(categoryId);
        List<com.alquiler.furent.model.Product> products = 
            productRepository.findByCategoriaNombre(category.getNombre());
        return products.isEmpty();
    }

    /**
     * Obtiene una categoría por ID o lanza excepción.
     * 
     * @param id ID de la categoría
     * @return Categoría encontrada
     * @throws ResourceNotFoundException si no existe
     */
    public Category getByIdOrThrow(String id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    /**
     * Obtiene todas las categorías.
     * 
     * @return Lista de categorías
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Guarda una categoría (crear o actualizar).
     * 
     * @param category Categoría a guardar
     * @return Categoría guardada
     */
    public Category save(Category category) {
        if (category.getTenantId() == null) {
            category.setTenantId(TenantContext.getCurrentTenant());
        }
        return categoryRepository.save(category);
    }

    /**
     * Elimina una categoría si no tiene productos asociados.
     * 
     * @param categoryId ID de la categoría
     * @throws InvalidOperationException si tiene productos asociados
     */
    public void delete(String categoryId) {
        if (!canDelete(categoryId)) {
            Category category = getByIdOrThrow(categoryId);
            throw new InvalidOperationException(
                "No se puede eliminar la categoría '" + category.getNombre() + 
                "' porque tiene productos asociados"
            );
        }
        categoryRepository.deleteById(categoryId);
    }

    /**
     * Actualiza el contador de productos de una categoría.
     * 
     * @param categoryName Nombre de la categoría
     */
    public void updateProductCount(String categoryName) {
        List<Category> categories = categoryRepository.findAll();
        Optional<Category> categoryOpt = categories.stream()
            .filter(c -> c.getNombre().equals(categoryName))
            .findFirst();
        
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            int count = productRepository.findByCategoriaNombre(categoryName).size();
            category.setCantidadProductos(count);
            categoryRepository.save(category);
        }
    }
}
