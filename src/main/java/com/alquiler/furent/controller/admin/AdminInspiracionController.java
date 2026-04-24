package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.InspirationImage;
import com.alquiler.furent.model.InspirationImage.InspirationPin;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.service.InspirationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/inspiracion")
public class AdminInspiracionController {

    @Autowired
    private InspirationService inspirationService;

    @Autowired
    private ProductRepository productRepository;

    private String getTenantId(HttpSession session) {
        String tenantId = (String) session.getAttribute("tenantId");
        return tenantId != null ? tenantId : "default";
    }

    @GetMapping
    public String inspiracionPage(Model model, HttpSession session) {
        String tenantId = getTenantId(session);
        
        List<InspirationImage> images;
        try {
            images = inspirationService.getAllImages(tenantId);
        } catch (Exception e) {
            images = new java.util.ArrayList<>();
        }
        
        List<Product> products;
        try {
            products = productRepository.findByTenantIdAndDisponible(tenantId, true);
            if (products == null || products.isEmpty()) {
                products = productRepository.findByDisponibleTrue();
            }
        } catch (Exception e) {
            products = new java.util.ArrayList<>();
        }
        
        model.addAttribute("images", images != null ? images : new java.util.ArrayList<>());
        model.addAttribute("products", products != null ? products : new java.util.ArrayList<>());
        model.addAttribute("activeMenu", "inspiracion");
        return "admin/inspiracion";
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "guestCount", required = false) Integer guestCount,
            @RequestParam(value = "category", required = false) String category,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            String tenantId = getTenantId(session);
            String imageUrl = inspirationService.uploadImage(file);
            
            InspirationImage image = new InspirationImage();
            image.setTenantId(tenantId);
            image.setImageUrl(imageUrl);
            image.setTitle(title);
            image.setEventType(eventType);
            image.setLocation(location);
            image.setGuestCount(guestCount);
            image.setCategory(category != null ? category : "bodas");
            image.setDisplayOrder((int) inspirationService.countImages(tenantId));
            
            InspirationImage saved = inspirationService.createImage(image);
            
            response.put("success", true);
            response.put("image", saved);
            response.put("message", "Imagen subida correctamente");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir la imagen: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateImage(
            @PathVariable String id,
            @RequestBody InspirationImage imageData) {
        
        Map<String, Object> response = new HashMap<>();
        Optional<InspirationImage> optImage = inspirationService.getImageById(id);
        
        if (optImage.isPresent()) {
            InspirationImage image = optImage.get();
            image.setTitle(imageData.getTitle());
            image.setEventType(imageData.getEventType());
            image.setLocation(imageData.getLocation());
            image.setGuestCount(imageData.getGuestCount());
            image.setCategory(imageData.getCategory());
            image.setActive(imageData.isActive());
            image.setDisplayOrder(imageData.getDisplayOrder());
            
            InspirationImage updated = inspirationService.updateImage(image);
            response.put("success", true);
            response.put("image", updated);
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Imagen no encontrada");
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            inspirationService.deleteImage(id);
            response.put("success", true);
            response.put("message", "Imagen eliminada correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la imagen");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{imageId}/pins")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addPin(
            @PathVariable String imageId,
            @RequestBody InspirationPin pin) {
        
        Map<String, Object> response = new HashMap<>();
        InspirationImage updated = inspirationService.addPin(imageId, pin);
        
        if (updated != null) {
            response.put("success", true);
            response.put("image", updated);
            response.put("pin", pin);
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Imagen no encontrada");
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{imageId}/pins/{pinId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePin(
            @PathVariable String imageId,
            @PathVariable String pinId,
            @RequestBody InspirationPin pin) {
        
        Map<String, Object> response = new HashMap<>();
        pin.setPinId(pinId);
        InspirationImage updated = inspirationService.updatePin(imageId, pin);
        
        if (updated != null) {
            response.put("success", true);
            response.put("image", updated);
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Imagen o pin no encontrado");
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{imageId}/pins/{pinId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removePin(
            @PathVariable String imageId,
            @PathVariable String pinId) {
        
        Map<String, Object> response = new HashMap<>();
        InspirationImage updated = inspirationService.removePin(imageId, pinId);
        
        if (updated != null) {
            response.put("success", true);
            response.put("image", updated);
            response.put("message", "Pin eliminado correctamente");
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Imagen o pin no encontrado");
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{imageId}/pins/{pinId}/assign-product")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignProductToPin(
            @PathVariable String imageId,
            @PathVariable String pinId,
            @RequestBody Map<String, String> body) {
        
        Map<String, Object> response = new HashMap<>();
        String productId = body.get("productId");
        
        InspirationImage updated = inspirationService.assignProductToPin(imageId, pinId, productId);
        
        if (updated != null) {
            response.put("success", true);
            response.put("image", updated);
            response.put("message", "Producto asignado correctamente");
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "No se pudo asignar el producto");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<InspirationImage> getImage(@PathVariable String id) {
        Optional<InspirationImage> image = inspirationService.getImageById(id);
        return image.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/reorder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorderImages(@RequestBody List<Map<String, Object>> orderList) {
        Map<String, Object> response = new HashMap<>();
        try {
            orderList.forEach(item -> {
                String id = (String) item.get("id");
                int order = ((Number) item.get("order")).intValue();
                inspirationService.getImageById(id).ifPresent(image -> {
                    image.setDisplayOrder(order);
                    inspirationService.updateImage(image);
                });
            });
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleImageStatus(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        Optional<InspirationImage> optImage = inspirationService.getImageById(id);
        
        if (optImage.isPresent()) {
            InspirationImage image = optImage.get();
            image.setActive(!image.isActive());
            InspirationImage updated = inspirationService.updateImage(image);
            response.put("success", true);
            response.put("image", updated);
            response.put("active", updated.isActive());
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Imagen no encontrada");
        return ResponseEntity.notFound().build();
    }
}
