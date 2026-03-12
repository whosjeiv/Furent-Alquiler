package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Category;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.service.AuditLogService;
import com.alquiler.furent.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminProductosController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductosController.class);

    private final ProductService productService;
    private final AuditLogService auditLogService;

    public AdminProductosController(ProductService productService, AuditLogService auditLogService) {
        this.productService = productService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/mobiliarios")
    public String listProducts(Model model) {
        model.addAttribute("productos", productService.getAllProducts());
        model.addAttribute("categorias", productService.getAllCategories());
        return "admin/mobiliarios";
    }

    @PostMapping("/mobiliarios/guardar")
    public String saveProduct(@RequestParam String nombre, @RequestParam String descripcion,
            @RequestParam String descripcionCorta, @RequestParam BigDecimal precioPorDia,
            @RequestParam(required = false) List<MultipartFile> galeriaArchivos,
            @RequestParam String categoriaNombre,
            @RequestParam String material, @RequestParam String dimensiones,
            @RequestParam String color, @RequestParam int cantidadMinima,
            @RequestParam int cantidadMaxima, @RequestParam int stock,
            @RequestParam(defaultValue = "10") int stockMinimo,
            @RequestParam(defaultValue = "EXCELENTE") String estadoMantenimiento,
            @RequestParam(required = false) String notasMantenimiento,
            @RequestParam(required = false) String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Product product;
        boolean isNew = (id == null || id.isEmpty());
        if (!isNew) {
            product = productService.getProductById(id).orElse(new Product());
        } else {
            product = new Product();
        }

        if (isNew && (galeriaArchivos == null || galeriaArchivos.isEmpty() || galeriaArchivos.get(0).isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "Es obligatorio subir imágenes para un nuevo mobiliario.");
            return "redirect:/admin/mobiliarios";
        }

        long countValidFiles = (galeriaArchivos == null) ? 0 : galeriaArchivos.stream().filter(f -> !f.isEmpty()).count();
        if (countValidFiles > 5) {
            redirectAttributes.addFlashAttribute("error", "Solo se permiten un máximo de 5 imágenes.");
            return "redirect:/admin/mobiliarios";
        }

        product.setNombre(nombre);
        product.setDescripcion(descripcion);
        product.setDescripcionCorta(descripcionCorta);
        product.setPrecioPorDia(precioPorDia);

        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            if (galeriaArchivos != null && countValidFiles > 0) {
                List<String> urls = new ArrayList<>();
                for (MultipartFile file : galeriaArchivos) {
                    if (!file.isEmpty()) {
                        if (file.getSize() > 104857600) { // 100MB
                            redirectAttributes.addFlashAttribute("error", "El archivo " + file.getOriginalFilename() + " excede los 100MB.");
                            return "redirect:/admin/mobiliarios";
                        }
                        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                        Path filePath = uploadDir.resolve(uniqueFileName);
                        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                        urls.add("/uploads/" + uniqueFileName);
                    }
                }
                if (!urls.isEmpty()) {
                    product.setImagenUrl(urls.get(0));
                    product.setGalleryImages(urls);
                }
            }
        } catch (Exception e) {
            log.error("Error al guardar archivos de imagen: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al subir imágenes: " + e.getMessage());
            return "redirect:/admin/mobiliarios";
        }

        product.setCategoriaNombre(categoriaNombre);
        product.setMaterial(material);
        product.setDimensiones(dimensiones);
        product.setColor(color);
        product.setCantidadMinima(cantidadMinima);
        product.setCantidadMaxima(cantidadMaxima);
        product.setStock(stock);
        product.setStockMinimo(stockMinimo);
        product.setEstadoMantenimiento(estadoMantenimiento);
        product.setNotasMantenimiento(notasMantenimiento);

        boolean available = stock > 0 && !"EN_REPARACION".equals(estadoMantenimiento);
        product.setDisponible(available);

        productService.saveProduct(product);

        String action = (id != null && !id.isEmpty()) ? "ACTUALIZAR" : "CREAR";
        String detail = String.format("Mobiliario: %s | Stock: %d | Estado: %s | Disp: %b",
                nombre, stock, estadoMantenimiento, available);
        auditLogService.log(authentication.getName(), action, "PRODUCTO", product.getId(), detail);

        redirectAttributes.addFlashAttribute("success", "Mobiliario guardado exitosamente");
        return "redirect:/admin/mobiliarios";
    }

    @PostMapping("/mobiliarios/eliminar/{id}")
    public String deleteProduct(@PathVariable String id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("success", "Mobiliario eliminado");
        return "redirect:/admin/mobiliarios";
    }

    // === CATEGORÍAS ===
    @GetMapping("/categorias")
    public String listCategories(Model model) {
        model.addAttribute("categorias", productService.getAllCategories());
        return "admin/categorias";
    }

    @PostMapping("/categorias/guardar")
    public String saveCategory(@RequestParam String nombre, @RequestParam String descripcion,
            @RequestParam String icono, @RequestParam String slug,
            @RequestParam(required = false) String id,
            RedirectAttributes redirectAttributes) {
        Category category;
        if (id != null && !id.isEmpty()) {
            category = productService.getCategoryById(id).orElse(new Category());
        } else {
            category = new Category();
        }
        category.setNombre(nombre);
        category.setDescripcion(descripcion);
        category.setIcono(icono);
        category.setSlug(slug);
        productService.saveCategory(category);
        redirectAttributes.addFlashAttribute("success", "Categoría guardada exitosamente");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/eliminar/{id}")
    public String deleteCategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
        productService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("success", "Categoría eliminada");
        return "redirect:/admin/categorias";
    }
}
