package com.alquiler.furent.controller.admin;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.model.Category;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.service.AuditLogService;
import com.alquiler.furent.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controlador para gestión administrativa de categorías.
 * Permite CRUD completo: listar, crear, editar, eliminar.
 */
@Controller
@RequestMapping("/admin/categorias")
public class AdminCategoriasController {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public AdminCategoriasController(CategoryService categoryService, 
                                     ProductRepository productRepository,
                                     AuditLogService auditLogService) {
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Lista todas las categorías con contador de productos.
     * 
     * @param model Modelo para la vista
     * @return Vista de lista de categorías
     */
    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.findAll();
        
        // Actualizar contador de productos para cada categoría
        for (Category category : categories) {
            int productCount = productRepository.findByCategoriaNombre(category.getNombre()).size();
            category.setCantidadProductos(productCount);
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("newCategory", new Category());
        
        return "admin/categorias";
    }

    /**
     * Guarda una categoría (crear o editar).
     * 
     * @param category Categoría a guardar
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de categorías
     */
    @PostMapping("/guardar")
    public String saveCategory(
            @ModelAttribute Category category,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validar nombre único
            String excludeId = category.getId() != null ? category.getId() : "";
            categoryService.validateUniqueName(category.getNombre(), excludeId);
            
            // Generar slug si no existe
            if (category.getSlug() == null || category.getSlug().isEmpty()) {
                category.setSlug(generateSlug(category.getNombre()));
            }
            
            boolean isNew = category.getId() == null;
            Category savedCategory = categoryService.save(category);
            
            // Registrar en audit log
            String accion = isNew ? "CREAR_CATEGORIA" : "EDITAR_CATEGORIA";
            auditLogService.log(
                principal.getName(),
                accion,
                "CATEGORIA",
                savedCategory.getId(),
                "Categoría: " + savedCategory.getNombre()
            );
            
            String mensaje = isNew ? "Categoría creada exitosamente" : "Categoría actualizada exitosamente";
            redirectAttributes.addFlashAttribute("success", mensaje);
            
        } catch (InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar categoría: " + e.getMessage());
        }
        
        return "redirect:/admin/categorias";
    }

    /**
     * Elimina una categoría si no tiene productos asociados.
     * 
     * @param id ID de la categoría
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de categorías
     */
    @DeleteMapping("/{id}")
    public String deleteCategory(
            @PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            Category category = categoryService.getByIdOrThrow(id);
            String categoryName = category.getNombre();
            
            // Intentar eliminar (lanzará excepción si tiene productos)
            categoryService.delete(id);
            
            // Registrar en audit log
            auditLogService.log(
                principal.getName(),
                "ELIMINAR_CATEGORIA",
                "CATEGORIA",
                id,
                "Categoría eliminada: " + categoryName
            );
            
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada exitosamente");
            
        } catch (InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar categoría: " + e.getMessage());
        }
        
        return "redirect:/admin/categorias";
    }

    /**
     * Genera un slug a partir del nombre de la categoría.
     * 
     * @param nombre Nombre de la categoría
     * @return Slug generado
     */
    private String generateSlug(String nombre) {
        return nombre.toLowerCase()
            .replaceAll("[áàäâ]", "a")
            .replaceAll("[éèëê]", "e")
            .replaceAll("[íìïî]", "i")
            .replaceAll("[óòöô]", "o")
            .replaceAll("[úùüû]", "u")
            .replaceAll("[ñ]", "n")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }
}
