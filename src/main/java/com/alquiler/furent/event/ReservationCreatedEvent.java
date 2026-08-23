package com.alquiler.furent.event;

import com.alquiler.furent.model.Reservation;

public class ReservationCreatedEvent extends FurentEvent {

    private final Reservation reservation;

    public ReservationCreatedEvent(Object source, Reservation reservation, String tenantId) {
        super(source, tenantId, reservation.getUsuarioId());
        this.reservation = reservation;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
