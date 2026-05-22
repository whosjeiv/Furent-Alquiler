package com.alquiler.furent.service;

import com.alquiler.furent.model.InspirationImage;
import com.alquiler.furent.model.InspirationImage.InspirationPin;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.repository.InspirationImageRepository;
import com.alquiler.furent.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class InspirationService {

    private static final Logger log = LoggerFactory.getLogger(InspirationService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${furent.upload.inspiration-dir:uploads/inspiration}")
    private String uploadDir;

    @Autowired
    private InspirationImageRepository inspirationImageRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<InspirationImage> getAllImages(String tenantId) {
        return inspirationImageRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
    }

    public List<InspirationImage> getActiveImages(String tenantId) {
        return inspirationImageRepository.findByTenantIdAndActiveOrderByDisplayOrderAsc(tenantId, true);
    }

    public List<InspirationImage> getActiveImagesByCategory(String tenantId, String category) {
        if (category == null || category.isEmpty() || category.equals("all")) {
            return getActiveImages(tenantId);
        }
        return inspirationImageRepository.findByTenantIdAndCategoryAndActiveOrderByDisplayOrderAsc(tenantId, category, true);
    }

    public Optional<InspirationImage> getImageById(String id) {
        return inspirationImageRepository.findById(id);
    }

    public InspirationImage createImage(InspirationImage image) {
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        return inspirationImageRepository.save(image);
    }

    public InspirationImage updateImage(InspirationImage image) {
        image.setUpdatedAt(LocalDateTime.now());
        return inspirationImageRepository.save(image);
    }

    public void deleteImage(String id) {
        inspirationImageRepository.findById(id).ifPresent(image -> {
            deletePhysicalFile(image.getImageUrl());
        });
        inspirationImageRepository.deleteById(id);
    }

    private void deletePhysicalFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(filename);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Archivo de inspiración eliminado: {}", filePath);
            } else {
                log.warn("Archivo de inspiración no encontrado para eliminar: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error al eliminar archivo físico de inspiración: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("El archivo está vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IOException("Tipo de archivo no permitido. Solo se aceptan: JPG, PNG, WEBP, GIF.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IOException("Nombre de archivo inválido.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        String extWithoutDot = extension.substring(1);
        if (!ALLOWED_EXTENSIONS.contains(extWithoutDot)) {
            throw new IOException("Extensión no permitida: " + extension);
        }

        String newFilename = "inspiration-" + UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Imagen de inspiración guardada: {}", filePath);
        return "/uploads/inspiration/" + newFilename;
    }

    public InspirationImage addPin(String imageId, InspirationPin pin) {
        Optional<InspirationImage> optImage = inspirationImageRepository.findById(imageId);
        if (optImage.isPresent()) {
            InspirationImage image = optImage.get();
            image.addPin(pin);
            image.setUpdatedAt(LocalDateTime.now());
            return inspirationImageRepository.save(image);
        }
        return null;
    }

    public InspirationImage updatePin(String imageId, InspirationPin updatedPin) {
        Optional<InspirationImage> optImage = inspirationImageRepository.findById(imageId);
        if (optImage.isPresent()) {
            InspirationImage image = optImage.get();
            for (int i = 0; i < image.getPins().size(); i++) {
                if (image.getPins().get(i).getPinId().equals(updatedPin.getPinId())) {
                    image.getPins().set(i, updatedPin);
                    break;
                }
            }
            image.setUpdatedAt(LocalDateTime.now());
            return inspirationImageRepository.save(image);
        }
        return null;
    }

    public InspirationImage removePin(String imageId, String pinId) {
        Optional<InspirationImage> optImage = inspirationImageRepository.findById(imageId);
        if (optImage.isPresent()) {
            InspirationImage image = optImage.get();
            image.removePin(pinId);
            image.setUpdatedAt(LocalDateTime.now());
            return inspirationImageRepository.save(image);
        }
        return null;
    }

    public InspirationImage assignProductToPin(String imageId, String pinId, String productId) {
        Optional<InspirationImage> optImage = inspirationImageRepository.findById(imageId);
        Optional<Product> optProduct = productRepository.findById(productId);

        if (optImage.isPresent() && optProduct.isPresent()) {
            InspirationImage image = optImage.get();
            Product product = optProduct.get();

            for (InspirationPin pin : image.getPins()) {
                if (pin.getPinId().equals(pinId)) {
                    pin.setProductId(product.getId());
                    pin.setProductName(product.getNombre());
                    pin.setProductDescription(product.getDescripcionCorta());
                    pin.setProductImageUrl(product.getImagenUrl());
                    pin.setProductPrice(product.getPrecioPorDia().doubleValue());
                    break;
                }
            }
            image.setUpdatedAt(LocalDateTime.now());
            return inspirationImageRepository.save(image);
        }
        return null;
    }

    public long countImages(String tenantId) {
        return inspirationImageRepository.countByTenantId(tenantId);
    }
}
