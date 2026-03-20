package com.alquiler.furent.dto;

import com.alquiler.furent.model.User;
import java.math.BigDecimal;

public class UserAdminDetailsDTO {
    private User user;
    private long totalReservas;
    private BigDecimal totalInvertido;
    private long favoritosCount;

    public UserAdminDetailsDTO() {}

    public UserAdminDetailsDTO(User user, long totalReservas, BigDecimal totalInvertido, long favoritosCount) {
        this.user = user;
        this.totalReservas = totalReservas;
        this.totalInvertido = totalInvertido;
        this.favoritosCount = favoritosCount;
    }

    // Getters
    public User getUser() { return user; }
    public long getTotalReservas() { return totalReservas; }
    public BigDecimal getTotalInvertido() { return totalInvertido; }
    public long getFavoritosCount() { return favoritosCount; }
}
