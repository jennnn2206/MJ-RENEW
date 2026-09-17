package com.mjrenew.mjrenew_backend.transaccion.repository;

import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransaccionBancariaRepository extends JpaRepository<TransaccionBancaria, UUID> {
}
