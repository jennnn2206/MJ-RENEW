package com.mjrenew.mjrenew_backend.nucleo.notificacion.mapper;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.dto.NotificacionResponse;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.entity.Notificacion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionMapper { NotificacionResponse toResponse(Notificacion notificacion); }
