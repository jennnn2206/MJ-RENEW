package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.TrasladoAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrasladoAntiguedadRepository extends JpaRepository<TrasladoAntiguedad, UUID> {
}
