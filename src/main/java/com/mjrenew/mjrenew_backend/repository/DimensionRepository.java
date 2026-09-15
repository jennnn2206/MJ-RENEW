package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DimensionRepository extends JpaRepository<Dimension, UUID> {
}
