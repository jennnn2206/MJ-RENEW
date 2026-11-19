-- La venta desde el taller conserva el origen de la entrega al comprador.
-- NULL en publicaciones anteriores y en ventas desde el domicilio del propietario.
ALTER TABLE catalogo_antiguedad
    ADD COLUMN IF NOT EXISTS direccion_recogida_taller_catalogo VARCHAR(300);
