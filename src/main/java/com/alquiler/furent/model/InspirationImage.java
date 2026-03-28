package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "inspiration_images")
public class InspirationImage {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String imageUrl;
    private String title;
    private String eventType;
    private String location;
    private Integer guestCount;
    private String category;
    private boolean active;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<InspirationPin> pins = new ArrayList<>();

    public InspirationImage() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        this.displayOrder = 0;
    }

    public static class InspirationPin {
        private String pinId;
        private double positionX;
        private double positionY;
        private String productId;
        private String productName;
        private String productDescription;
        private String productImageUrl;
        private double productPrice;
        private String iconColor;

        public InspirationPin() {
            this.pinId = java.util.UUID.randomUUID().toString();
            this.iconColor = "green";
        }

        public String getPinId() {
            return pinId;
        }

        public void setPinId(String pinId) {
            this.pinId = pinId;
        }

        public double getPositionX() {
            return positionX;
        }

        public void setPositionX(double positionX) {
            this.positionX = positionX;
        }

        public double getPositionY() {
            return positionY;
        }

        public void setPositionY(double positionY) {
            this.positionY = positionY;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getProductDescription() {
            return productDescription;
        }

        public void setProductDescription(String productDescription) {
            this.productDescription = productDescription;
        }

        public String getProductImageUrl() {
            return productImageUrl;
        }

        public void setProductImageUrl(String productImageUrl) {
            this.productImageUrl = productImageUrl;
        }

        public double getProductPrice() {
            return productPrice;
        }

        public void setProductPrice(double productPrice) {
            this.productPrice = productPrice;
        }

        public String getIconColor() {
            return iconColor;
        }

        public void setIconColor(String iconColor) {
            this.iconColor = iconColor;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<InspirationPin> getPins() {
        return pins;
    }

    public void setPins(List<InspirationPin> pins) {
        this.pins = pins;
    }

    public void addPin(InspirationPin pin) {
        this.pins.add(pin);
    }

    public void removePin(String pinId) {
        this.pins.removeIf(p -> p.getPinId().equals(pinId));
    }
}
