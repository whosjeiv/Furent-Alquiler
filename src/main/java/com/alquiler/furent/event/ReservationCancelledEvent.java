package com.alquiler.furent.event;

import com.alquiler.furent.model.Reservation;

public class ReservationCancelledEvent extends FurentEvent {

    private final Reservation reservation;
    private final String reason;

    public ReservationCancelledEvent(Object source, Reservation reservation, String tenantId, String reason) {
        super(source, tenantId, reservation.getUsuarioId());
        this.reservation = reservation;
        this.reason = reason;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public String getReason() {
        return reason;
    }
}
