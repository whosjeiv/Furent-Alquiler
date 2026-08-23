package com.alquiler.furent.event;

import com.alquiler.furent.model.Product;

public class ProductUpdatedEvent extends FurentEvent {

    private final Product product;
    private final String action; // CREATED, UPDATED, DELETED

    public ProductUpdatedEvent(Object source, Product product, String tenantId, String action) {
        super(source, tenantId, null);
        this.product = product;
        this.action = action;
    }

    public Product getProduct() {
        return product;
    }

    public String getAction() {
        return action;
    }
}
