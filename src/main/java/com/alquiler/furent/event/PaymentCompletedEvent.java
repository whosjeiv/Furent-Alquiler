package com.alquiler.furent.event;

import com.alquiler.furent.model.Payment;

public class PaymentCompletedEvent extends FurentEvent {

    private final Payment payment;

    public PaymentCompletedEvent(Object source, Payment payment, String tenantId) {
        super(source, tenantId, payment.getUsuarioId());
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }
}
