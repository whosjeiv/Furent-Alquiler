package com.alquiler.furent.repository;

import com.alquiler.furent.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByUsuarioId(String usuarioId);

    @Query("{'items.productoId': ?0, 'estado': {$in: ['PENDIENTE','CONFIRMADA','ENTREGADA']}}")
    List<Reservation> findActiveByProductoId(String productoId);

    @Query("{'items.productoId': {$in: ?0}, 'estado': {$in: ['PENDIENTE','CONFIRMADA','ENTREGADA']}}")
    List<Reservation> findActiveByProductIds(List<String> productIds);

    List<Reservation> findByEstado(String estado);

    long countByEstado(String estado);

    List<Reservation> findByUsuarioIdAndEstado(String usuarioId, String estado);

    java.util.Optional<Reservation> findByCodigoPagoEfectivo(String codigoPagoEfectivo);
}
