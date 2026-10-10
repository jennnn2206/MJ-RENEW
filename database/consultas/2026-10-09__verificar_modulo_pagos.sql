-- MJ Renew | Consultas de verificación SIN CAMBIOS de datos.
-- Ejecutar desde Query Tool de pgAdmin en la base MJRenew-DB.

-- 1) Tipos actuales de columnas usadas por Stripe en catálogo.
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'catalogo_antiguedad'
  AND column_name IN ('id_link_stripe_catalogo', 'id_pago_stripe_catalogo', 'url_link_pago_catalogo')
ORDER BY column_name;

-- 2) Comprobar tipos PostgreSQL (incluidas ENUM nativas) usados por las entidades.
SELECT table_name, column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (
    (table_name = 'transacciones_bancarias' AND column_name IN ('tipo_transaccion', 'flujo_mjrenew', 'estado_transaccion'))
        OR (table_name = 'perfiles_restaurador' AND column_name = 'disponibilidad_restaurador')
        OR (table_name = 'cotizaciones' AND column_name = 'estado_cotizacion')
        OR (table_name = 'traslados_antiguedad' AND column_name IN ('tipo_traslado', 'resultado_traslado'))
        OR (table_name = 'fotografias_antiguedad' AND column_name = 'tipo_fotografia')
        OR (table_name = 'disputas_antiguedad' AND column_name = 'estado_ciclo_al_abrir_disputa')
    )
ORDER BY table_name, column_name;

-- 3) Últimas transacciones de restauración con estado del AFD.
-- Nunca hacer UPDATE aquí: Stripe confirma a través de webhook o conciliación autorizada.
SELECT
    t.transaccion_id,
    t.tipo_transaccion,
    t.estado_transaccion,
    t.monto_bruto_mxn_transaccion,
    t.referencia_stripe_transaccion,
    t.creada_en_transaccion,
    t.procesada_en_transaccion,
    a.estado_actual_antiguedad
FROM public.transacciones_bancarias AS t
         JOIN public.antiguedades AS a ON a.antiguedades_id = t.antiguedad_id
WHERE t.tipo_transaccion::text = 'PAGO_RESTAURACION'
ORDER BY t.creada_en_transaccion DESC
    LIMIT 10;

-- 4) Cuentas demo presentes (sin devolver hashes ni contraseñas).
SELECT nombre_completo_usuario, correo_electronico_usuario, tipo_usuario,
       activo_usuario, correo_verificado_usuario
FROM public.usuarios
WHERE correo_electronico_usuario IN
      ('propietario1@test.mx', 'restaurador1@test.mx', 'comprador1@test.mx')
ORDER BY tipo_usuario;
