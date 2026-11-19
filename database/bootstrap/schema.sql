-- Esquema inicial de MJ Renew para una base PostgreSQL vacía.
-- Exportado sin datos ni propietarios desde la base local.
--
-- PostgreSQL database dump
--


-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: disponibilidad_restaurador; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.disponibilidad_restaurador AS ENUM (
    'DISPONIBLE',
    'NO_DISPONIBLE'
);


--
-- Name: estado_antiguedad; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_antiguedad AS ENUM (
    'PIEZA_CAPTURADA',
    'EN_EVALUACION',
    'CALCULANDO_PRESUPUESTO',
    'PRESUPUESTO_PRESENTADO',
    'PAGO_EN_ESCROW',
    'HORARIO_RECOLECCION_AGENDADO',
    'EN_RECOLECCION',
    'RECIBIDO_EN_TALLER',
    'EN_RESTAURACION',
    'AVANCE_PUBLICADO',
    'RESTAURACION_LISTA',
    'DECISION_EN_TALLER',
    'HORARIO_ENTREGA_AGENDADO',
    'EN_DEVOLUCION_PROPIETARIO',
    'ENTREGADO_AL_PROPIETARIO',
    'EN_CATALOGO',
    'LINK_PAGO_ENVIADO',
    'SOLICITUD_DEVOLUCION_TALLER',
    'CANCELADA_NO_CUMPLE',
    'CANCELADA_DUENO_DECLINO',
    'CANCELADA_RECOLECCION',
    'CANCELADA_ENTREGA',
    'RESTAURACION_COMPLETADA',
    'VENTA_COMPLETADA',
    'DEVUELTA_DESDE_TALLER'
);


--
-- Name: estado_cotizacion; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_cotizacion AS ENUM (
    'ENVIADA',
    'ACEPTADA',
    'RECHAZADA'
);


--
-- Name: estado_transaccion; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_transaccion AS ENUM (
    'PENDIENTE',
    'EXITOSA',
    'FALLIDA'
);


--
-- Name: estilo_mueble; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estilo_mueble AS ENUM (
    'COLONIAL',
    'VICTORIANO',
    'ART_DECO',
    'ART_NOUVEAU',
    'PORFIRIATO',
    'EDUARDIANO',
    'LUIS_XV',
    'OTRO'
);


--
-- Name: flujo_transaccion; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.flujo_transaccion AS ENUM (
    'INGRESO',
    'EGRESO'
);


--
-- Name: material_mueble; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.material_mueble AS ENUM (
    'CEDRO',
    'NOGAL',
    'CAOBA',
    'ROBLE',
    'HIERRO_FORJADO',
    'MADERA_TALLADA',
    'MIXTO',
    'OTRO'
);


--
-- Name: resultado_traslado; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.resultado_traslado AS ENUM (
    'PENDIENTE',
    'EXITOSO',
    'NADIE_EN_CASA',
    'CANCELADO'
);


--
-- Name: tipo_fotografia; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_fotografia AS ENUM (
    'ESTADO_INICIAL',
    'AVANCE',
    'ESTADO_FINAL',
    'TRASLADO_SALIDA',
    'TRASLADO_LLEGADA',
    'TRASLADO_INTENTO_FALLIDO'
);


--
-- Name: tipo_mueble; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_mueble AS ENUM (
    'ESCRITORIO',
    'VITRINA',
    'COMEDOR',
    'SILLON',
    'COMODA',
    'APARADOR',
    'MESA',
    'OTRO'
);


--
-- Name: tipo_transaccion; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_transaccion AS ENUM (
    'PAGO_RESTAURACION',
    'LIBERACION_RESTAURADOR',
    'PAGO_VENTA',
    'LIBERACION_PROPIETARIO'
);


--
-- Name: tipo_traslado; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_traslado AS ENUM (
    'RECOLECCION',
    'ENTREGA_PROPIETARIO',
    'ENTREGA_COMPRADOR'
);


--
-- Name: tipo_usuario; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_usuario AS ENUM (
    'PROPIETARIO',
    'RESTAURADOR',
    'TRANSPORTISTA',
    'COMPRADOR',
    'ADMINISTRADOR'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: antiguedades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.antiguedades (
    antiguedades_id uuid DEFAULT gen_random_uuid() NOT NULL,
    propietario_id uuid NOT NULL,
    restaurador_id uuid,
    tipo_mueble character varying(30) NOT NULL,
    estilo_mueble character varying(30),
    material_mueble character varying(30),
    descripcion_danios_antiguedad text,
    procedencia_mueble character varying(300),
    estado_actual_antiguedad character varying(40) DEFAULT 'PIEZA_CAPTURADA'::public.estado_antiguedad NOT NULL,
    registrada_en_antiguedad timestamp with time zone DEFAULT now() NOT NULL,
    restauracion_inicio_antiguedad timestamp with time zone,
    restauracion_fin_antiguedad timestamp with time zone,
    motivo_rechazo_evaluacion character varying(255),
    CONSTRAINT chk_fechas_restauracion_antiguedad CHECK (((restauracion_fin_antiguedad IS NULL) OR (restauracion_inicio_antiguedad IS NULL) OR (restauracion_fin_antiguedad >= restauracion_inicio_antiguedad)))
);


--
-- Name: avances_restauracion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avances_restauracion (
    avances_restauracion_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    restaurador_id uuid NOT NULL,
    descripcion_avance character varying(300) NOT NULL,
    publicado_en_avance timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: catalogo_antiguedad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.catalogo_antiguedad (
    catalogo_antiguedad_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    precio_mxn_catalogo numeric(10,2) NOT NULL,
    publicada_en_catalogo timestamp with time zone DEFAULT now() NOT NULL,
    expira_en_catalogo timestamp with time zone NOT NULL,
    ultima_actualizacion_precio_catalogo timestamp with time zone,
    activa_catalogo boolean DEFAULT true NOT NULL,
    comprador_id uuid,
    id_link_stripe_catalogo character varying(255),
    url_link_pago_catalogo text,
    id_pago_stripe_catalogo character varying(255),
    completada_en_catalogo timestamp with time zone,
    recepcion_confirmada_en_catalogo timestamp with time zone,
    direccion_recogida_taller_catalogo character varying(300),
    CONSTRAINT chk_actualizacion_precio_catalogo CHECK (((ultima_actualizacion_precio_catalogo IS NULL) OR (ultima_actualizacion_precio_catalogo >= publicada_en_catalogo))),
    CONSTRAINT chk_completada_catalogo CHECK (((completada_en_catalogo IS NULL) OR (completada_en_catalogo >= publicada_en_catalogo))),
    CONSTRAINT chk_expiracion_catalogo CHECK ((expira_en_catalogo >= publicada_en_catalogo)),
    CONSTRAINT chk_precio_catalogo CHECK ((precio_mxn_catalogo > (0)::numeric))
);


--
-- Name: cotizaciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cotizaciones (
    cotizaciones_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    restaurador_id uuid NOT NULL,
    costo_minimo_mxn_cotizacion numeric(10,2) NOT NULL,
    costo_maximo_mxn_cotizacion numeric(10,2) NOT NULL,
    tiempo_semanas_cotizacion integer NOT NULL,
    estado_cotizacion public.estado_cotizacion DEFAULT 'ENVIADA'::public.estado_cotizacion NOT NULL,
    motivo_rechazo_cotizacion text,
    enviada_en_cotizacion timestamp with time zone DEFAULT now() NOT NULL,
    respondida_en_cotizacion timestamp with time zone,
    descripcion_proceso_cotizacion character varying(2000) NOT NULL,
    monto_final_mxn numeric(10,2),
    CONSTRAINT chk_costo_maximo_cotizacion CHECK ((costo_maximo_mxn_cotizacion >= costo_minimo_mxn_cotizacion)),
    CONSTRAINT chk_costo_minimo_cotizacion CHECK ((costo_minimo_mxn_cotizacion > (0)::numeric)),
    CONSTRAINT chk_fecha_respuesta_cotizacion CHECK (((respondida_en_cotizacion IS NULL) OR (respondida_en_cotizacion >= enviada_en_cotizacion))),
    CONSTRAINT chk_monto_final_cotizacion CHECK (((monto_final_mxn IS NULL) OR ((monto_final_mxn >= costo_minimo_mxn_cotizacion) AND (monto_final_mxn <= costo_maximo_mxn_cotizacion)))),
    CONSTRAINT chk_tiempo_semanas_cotizacion CHECK ((tiempo_semanas_cotizacion > 0))
);


--
-- Name: cuentas_stripe_conectadas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cuentas_stripe_conectadas (
    cuenta_stripe_conectada_id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    stripe_account_id character varying(255) NOT NULL,
    detalles_enviados boolean DEFAULT false NOT NULL,
    transferencias_habilitadas boolean DEFAULT false NOT NULL,
    payouts_habilitados boolean DEFAULT false NOT NULL,
    creada_en timestamp with time zone NOT NULL,
    actualizada_en timestamp with time zone NOT NULL
);


--
-- Name: dimensiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dimensiones (
    dimensiones_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    alto_cm_antiguedad numeric(6,2),
    ancho_cm_antiguedad numeric(6,2),
    profundidad_cm_antiguedad numeric(6,2),
    peso_kg_antiguedad numeric(6,2),
    registradas_en_dimensiones timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_alto_cm_antiguedad CHECK (((alto_cm_antiguedad IS NULL) OR (alto_cm_antiguedad > (0)::numeric))),
    CONSTRAINT chk_ancho_cm_antiguedad CHECK (((ancho_cm_antiguedad IS NULL) OR (ancho_cm_antiguedad > (0)::numeric))),
    CONSTRAINT chk_peso_kg_antiguedad CHECK (((peso_kg_antiguedad IS NULL) OR (peso_kg_antiguedad > (0)::numeric))),
    CONSTRAINT chk_profundidad_cm_antiguedad CHECK (((profundidad_cm_antiguedad IS NULL) OR (profundidad_cm_antiguedad > (0)::numeric)))
);


--
-- Name: disputas_antiguedad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.disputas_antiguedad (
    disputas_antiguedad_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    abierta_por uuid NOT NULL,
    estado_ciclo_al_abrir_disputa public.estado_antiguedad NOT NULL,
    descripcion_disputa text NOT NULL,
    admin_asignado uuid,
    resolucion_disputa text,
    abierta_en_disputa timestamp with time zone DEFAULT now() NOT NULL,
    resuelta_en_disputa timestamp with time zone,
    CONSTRAINT chk_fecha_resuelta_disputa CHECK (((resuelta_en_disputa IS NULL) OR (resuelta_en_disputa >= abierta_en_disputa)))
);


--
-- Name: fotografias_antiguedad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fotografias_antiguedad (
    fotografias_antiguedad_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    avance_id uuid,
    tipo_fotografia public.tipo_fotografia NOT NULL,
    url_almacen_fotografia character varying(300) NOT NULL,
    subida_en_fotografia timestamp with time zone DEFAULT now() NOT NULL,
    traslado_id uuid,
    CONSTRAINT chk_fotografia_avance CHECK ((((tipo_fotografia = 'AVANCE'::public.tipo_fotografia) AND (avance_id IS NOT NULL)) OR ((tipo_fotografia <> 'AVANCE'::public.tipo_fotografia) AND (avance_id IS NULL))))
);


--
-- Name: fotografias_reclamo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fotografias_reclamo (
    fotografias_reclamo_id uuid DEFAULT gen_random_uuid() NOT NULL,
    disputas_antiguedad_id uuid NOT NULL,
    antiguedad_id uuid NOT NULL,
    descripcion_reclamo text NOT NULL,
    url_almacen_reclamo character varying(300) NOT NULL,
    subida_en_reclamo timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: perfiles_restaurador; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.perfiles_restaurador (
    perfiles_restaurador_id uuid DEFAULT gen_random_uuid() NOT NULL,
    restaurador_id uuid NOT NULL,
    especialidad_restaurador character varying(150) NOT NULL,
    anos_experiencia_restaurador smallint NOT NULL,
    descripcion_bio_restaurador text,
    disponibilidad_restaurador public.disponibilidad_restaurador DEFAULT 'DISPONIBLE'::public.disponibilidad_restaurador NOT NULL,
    aprobado_por_admin_restaurador boolean DEFAULT false NOT NULL,
    actualizado_en_restaurador timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_anos_experiencia_restaurador CHECK (((anos_experiencia_restaurador >= 0) AND (anos_experiencia_restaurador <= 100)))
);


--
-- Name: transacciones_bancarias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transacciones_bancarias (
    transaccion_id uuid DEFAULT gen_random_uuid() NOT NULL,
    tipo_transaccion public.tipo_transaccion NOT NULL,
    flujo_mjrenew public.flujo_transaccion NOT NULL,
    originator_id uuid NOT NULL,
    beneficiario_id uuid NOT NULL,
    antiguedad_id uuid NOT NULL,
    catalogo_antiguedad_id uuid,
    transaccion_padre_id uuid,
    monto_bruto_mxn_transaccion numeric(10,2) NOT NULL,
    estado_transaccion public.estado_transaccion DEFAULT 'PENDIENTE'::public.estado_transaccion NOT NULL,
    referencia_stripe_transaccion character varying(100),
    creada_en_transaccion timestamp with time zone DEFAULT now() NOT NULL,
    procesada_en_transaccion timestamp with time zone,
    comision_mjrenew_mxn numeric(10,2),
    monto_antes_cargo_stripe_mxn numeric(10,2),
    cargo_stripe_mxn numeric(10,2),
    monto_neto_propietario_mxn numeric(10,2),
    stripe_payment_intent_id character varying(100),
    stripe_charge_id character varying(100),
    stripe_balance_transaction_id character varying(100),
    CONSTRAINT chk_fecha_procesada_transaccion CHECK (((procesada_en_transaccion IS NULL) OR (procesada_en_transaccion >= creada_en_transaccion))),
    CONSTRAINT chk_monto_bruto_transaccion CHECK ((monto_bruto_mxn_transaccion > (0)::numeric))
);


--
-- Name: traslados_antiguedad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.traslados_antiguedad (
    traslados_antiguedad_id uuid DEFAULT gen_random_uuid() NOT NULL,
    antiguedad_id uuid NOT NULL,
    transportista_id uuid,
    tipo_traslado public.tipo_traslado NOT NULL,
    direccion_origen_traslado character varying(300) NOT NULL,
    direccion_destino_traslado character varying(300) NOT NULL,
    fecha_acordada_traslado date NOT NULL,
    hora_inicio_acordada_traslado time without time zone NOT NULL,
    hora_fin_acordada_traslado time without time zone NOT NULL,
    numero_intento_traslado smallint DEFAULT 1 NOT NULL,
    resultado_traslado public.resultado_traslado DEFAULT 'PENDIENTE'::public.resultado_traslado NOT NULL,
    costo_estimado_mxn_traslado numeric(10,2),
    motivo_fallo_traslado text,
    agendado_en_traslado timestamp with time zone DEFAULT now() NOT NULL,
    ejecutado_en_traslado timestamp with time zone,
    salida_confirmada_en_traslado timestamp with time zone,
    CONSTRAINT chk_costo_estimado_traslado CHECK (((costo_estimado_mxn_traslado IS NULL) OR (costo_estimado_mxn_traslado >= (0)::numeric))),
    CONSTRAINT chk_ejecutado_en_traslado CHECK (((ejecutado_en_traslado IS NULL) OR (ejecutado_en_traslado >= agendado_en_traslado))),
    CONSTRAINT chk_horario_traslado CHECK ((hora_fin_acordada_traslado >= hora_inicio_acordada_traslado)),
    CONSTRAINT chk_numero_intento_traslado CHECK (((numero_intento_traslado >= 1) AND (numero_intento_traslado <= 3)))
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    usuarios_id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre_completo_usuario character varying(100) NOT NULL,
    correo_electronico_usuario character varying(150) NOT NULL,
    contrasena_hash_usuario character varying(72) NOT NULL,
    tipo_usuario character varying(20) NOT NULL,
    telefono_usuario character varying(15),
    direccion_texto_usuario character varying(300),
    latitud_usuario numeric(9,6),
    longitud_usuario numeric(9,6),
    correo_verificado_usuario boolean DEFAULT false NOT NULL,
    activo_usuario boolean DEFAULT true NOT NULL,
    registrado_en_usuario timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_latitud_usuario CHECK (((latitud_usuario IS NULL) OR ((latitud_usuario >= ('-90'::integer)::numeric) AND (latitud_usuario <= (90)::numeric)))),
    CONSTRAINT chk_longitud_usuario CHECK (((longitud_usuario IS NULL) OR ((longitud_usuario >= ('-180'::integer)::numeric) AND (longitud_usuario <= (180)::numeric))))
);


--
-- Name: antiguedades antiguedades_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.antiguedades
    ADD CONSTRAINT antiguedades_pkey PRIMARY KEY (antiguedades_id);


--
-- Name: avances_restauracion avances_restauracion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avances_restauracion
    ADD CONSTRAINT avances_restauracion_pkey PRIMARY KEY (avances_restauracion_id);


--
-- Name: catalogo_antiguedad catalogo_antiguedad_antiguedad_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogo_antiguedad
    ADD CONSTRAINT catalogo_antiguedad_antiguedad_id_key UNIQUE (antiguedad_id);


--
-- Name: catalogo_antiguedad catalogo_antiguedad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogo_antiguedad
    ADD CONSTRAINT catalogo_antiguedad_pkey PRIMARY KEY (catalogo_antiguedad_id);


--
-- Name: cotizaciones cotizaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizaciones
    ADD CONSTRAINT cotizaciones_pkey PRIMARY KEY (cotizaciones_id);


--
-- Name: cuentas_stripe_conectadas cuentas_stripe_conectadas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_stripe_conectadas
    ADD CONSTRAINT cuentas_stripe_conectadas_pkey PRIMARY KEY (cuenta_stripe_conectada_id);


--
-- Name: dimensiones dimensiones_antiguedad_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dimensiones
    ADD CONSTRAINT dimensiones_antiguedad_id_key UNIQUE (antiguedad_id);


--
-- Name: dimensiones dimensiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dimensiones
    ADD CONSTRAINT dimensiones_pkey PRIMARY KEY (dimensiones_id);


--
-- Name: disputas_antiguedad disputas_antiguedad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputas_antiguedad
    ADD CONSTRAINT disputas_antiguedad_pkey PRIMARY KEY (disputas_antiguedad_id);


--
-- Name: fotografias_antiguedad fotografias_antiguedad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_antiguedad
    ADD CONSTRAINT fotografias_antiguedad_pkey PRIMARY KEY (fotografias_antiguedad_id);


--
-- Name: fotografias_reclamo fotografias_reclamo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_reclamo
    ADD CONSTRAINT fotografias_reclamo_pkey PRIMARY KEY (fotografias_reclamo_id);


--
-- Name: perfiles_restaurador perfiles_restaurador_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_restaurador
    ADD CONSTRAINT perfiles_restaurador_pkey PRIMARY KEY (perfiles_restaurador_id);


--
-- Name: perfiles_restaurador perfiles_restaurador_restaurador_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_restaurador
    ADD CONSTRAINT perfiles_restaurador_restaurador_id_key UNIQUE (restaurador_id);


--
-- Name: transacciones_bancarias transacciones_bancarias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT transacciones_bancarias_pkey PRIMARY KEY (transaccion_id);


--
-- Name: traslados_antiguedad traslados_antiguedad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.traslados_antiguedad
    ADD CONSTRAINT traslados_antiguedad_pkey PRIMARY KEY (traslados_antiguedad_id);


--
-- Name: cuentas_stripe_conectadas uk_cuenta_stripe_account_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_stripe_conectadas
    ADD CONSTRAINT uk_cuenta_stripe_account_id UNIQUE (stripe_account_id);


--
-- Name: cuentas_stripe_conectadas uk_cuenta_stripe_usuario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_stripe_conectadas
    ADD CONSTRAINT uk_cuenta_stripe_usuario UNIQUE (usuario_id);


--
-- Name: usuarios usuarios_correo_electronico_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_correo_electronico_usuario_key UNIQUE (correo_electronico_usuario);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (usuarios_id);


--
-- Name: idx_antiguedades_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_antiguedades_estado ON public.antiguedades USING btree (estado_actual_antiguedad);


--
-- Name: idx_antiguedades_propietario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_antiguedades_propietario ON public.antiguedades USING btree (propietario_id);


--
-- Name: idx_antiguedades_restaurador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_antiguedades_restaurador ON public.antiguedades USING btree (restaurador_id);


--
-- Name: idx_avances_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avances_antiguedad ON public.avances_restauracion USING btree (antiguedad_id);


--
-- Name: idx_avances_restaurador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avances_restaurador ON public.avances_restauracion USING btree (restaurador_id);


--
-- Name: idx_catalogo_activa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_catalogo_activa ON public.catalogo_antiguedad USING btree (activa_catalogo);


--
-- Name: idx_catalogo_comprador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_catalogo_comprador ON public.catalogo_antiguedad USING btree (comprador_id);


--
-- Name: idx_cotizaciones_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cotizaciones_antiguedad ON public.cotizaciones USING btree (antiguedad_id);


--
-- Name: idx_cotizaciones_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cotizaciones_estado ON public.cotizaciones USING btree (estado_cotizacion);


--
-- Name: idx_cotizaciones_restaurador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cotizaciones_restaurador ON public.cotizaciones USING btree (restaurador_id);


--
-- Name: idx_disputas_abierta_por; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_disputas_abierta_por ON public.disputas_antiguedad USING btree (abierta_por);


--
-- Name: idx_disputas_admin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_disputas_admin ON public.disputas_antiguedad USING btree (admin_asignado);


--
-- Name: idx_disputas_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_disputas_antiguedad ON public.disputas_antiguedad USING btree (antiguedad_id);


--
-- Name: idx_fotografias_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_antiguedad ON public.fotografias_antiguedad USING btree (antiguedad_id);


--
-- Name: idx_fotografias_avance; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_avance ON public.fotografias_antiguedad USING btree (avance_id);


--
-- Name: idx_fotografias_reclamo_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_reclamo_antiguedad ON public.fotografias_reclamo USING btree (antiguedad_id);


--
-- Name: idx_fotografias_reclamo_disputa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_reclamo_disputa ON public.fotografias_reclamo USING btree (disputas_antiguedad_id);


--
-- Name: idx_fotografias_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_tipo ON public.fotografias_antiguedad USING btree (tipo_fotografia);


--
-- Name: idx_fotografias_traslado_mjrenew; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotografias_traslado_mjrenew ON public.fotografias_antiguedad USING btree (traslado_id);


--
-- Name: idx_perfiles_restaurador_disponibilidad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_perfiles_restaurador_disponibilidad ON public.perfiles_restaurador USING btree (disponibilidad_restaurador);


--
-- Name: idx_transacciones_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_antiguedad ON public.transacciones_bancarias USING btree (antiguedad_id);


--
-- Name: idx_transacciones_beneficiario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_beneficiario ON public.transacciones_bancarias USING btree (beneficiario_id);


--
-- Name: idx_transacciones_catalogo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_catalogo ON public.transacciones_bancarias USING btree (catalogo_antiguedad_id);


--
-- Name: idx_transacciones_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_estado ON public.transacciones_bancarias USING btree (estado_transaccion);


--
-- Name: idx_transacciones_originator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_originator ON public.transacciones_bancarias USING btree (originator_id);


--
-- Name: idx_transacciones_padre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transacciones_padre ON public.transacciones_bancarias USING btree (transaccion_padre_id);


--
-- Name: idx_traslados_antiguedad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traslados_antiguedad ON public.traslados_antiguedad USING btree (antiguedad_id);


--
-- Name: idx_traslados_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traslados_fecha ON public.traslados_antiguedad USING btree (fecha_acordada_traslado);


--
-- Name: idx_traslados_resultado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traslados_resultado ON public.traslados_antiguedad USING btree (resultado_traslado);


--
-- Name: idx_traslados_transportista; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traslados_transportista ON public.traslados_antiguedad USING btree (transportista_id);


--
-- Name: idx_usuarios_activo_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_activo_usuario ON public.usuarios USING btree (activo_usuario);


--
-- Name: idx_usuarios_tipo_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_tipo_usuario ON public.usuarios USING btree (tipo_usuario);


--
-- Name: antiguedades fk_antiguedad_propietario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.antiguedades
    ADD CONSTRAINT fk_antiguedad_propietario FOREIGN KEY (propietario_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: antiguedades fk_antiguedad_restaurador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.antiguedades
    ADD CONSTRAINT fk_antiguedad_restaurador FOREIGN KEY (restaurador_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: avances_restauracion fk_avance_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avances_restauracion
    ADD CONSTRAINT fk_avance_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: avances_restauracion fk_avance_restaurador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avances_restauracion
    ADD CONSTRAINT fk_avance_restaurador FOREIGN KEY (restaurador_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: catalogo_antiguedad fk_catalogo_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogo_antiguedad
    ADD CONSTRAINT fk_catalogo_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: catalogo_antiguedad fk_catalogo_comprador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogo_antiguedad
    ADD CONSTRAINT fk_catalogo_comprador FOREIGN KEY (comprador_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: cotizaciones fk_cotizacion_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizaciones
    ADD CONSTRAINT fk_cotizacion_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: cotizaciones fk_cotizacion_restaurador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizaciones
    ADD CONSTRAINT fk_cotizacion_restaurador FOREIGN KEY (restaurador_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: cuentas_stripe_conectadas fk_cuenta_stripe_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_stripe_conectadas
    ADD CONSTRAINT fk_cuenta_stripe_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: dimensiones fk_dimensiones_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dimensiones
    ADD CONSTRAINT fk_dimensiones_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: disputas_antiguedad fk_disputa_abierta_por; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputas_antiguedad
    ADD CONSTRAINT fk_disputa_abierta_por FOREIGN KEY (abierta_por) REFERENCES public.usuarios(usuarios_id);


--
-- Name: disputas_antiguedad fk_disputa_admin; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputas_antiguedad
    ADD CONSTRAINT fk_disputa_admin FOREIGN KEY (admin_asignado) REFERENCES public.usuarios(usuarios_id);


--
-- Name: disputas_antiguedad fk_disputa_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputas_antiguedad
    ADD CONSTRAINT fk_disputa_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: fotografias_antiguedad fk_fotografia_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_antiguedad
    ADD CONSTRAINT fk_fotografia_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: fotografias_antiguedad fk_fotografia_avance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_antiguedad
    ADD CONSTRAINT fk_fotografia_avance FOREIGN KEY (avance_id) REFERENCES public.avances_restauracion(avances_restauracion_id);


--
-- Name: fotografias_reclamo fk_fotografia_reclamo_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_reclamo
    ADD CONSTRAINT fk_fotografia_reclamo_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: fotografias_reclamo fk_fotografia_reclamo_disputa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_reclamo
    ADD CONSTRAINT fk_fotografia_reclamo_disputa FOREIGN KEY (disputas_antiguedad_id) REFERENCES public.disputas_antiguedad(disputas_antiguedad_id);


--
-- Name: fotografias_antiguedad fk_fotografia_traslado_mjrenew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotografias_antiguedad
    ADD CONSTRAINT fk_fotografia_traslado_mjrenew FOREIGN KEY (traslado_id) REFERENCES public.traslados_antiguedad(traslados_antiguedad_id);


--
-- Name: perfiles_restaurador fk_perfil_restaurador_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_restaurador
    ADD CONSTRAINT fk_perfil_restaurador_usuario FOREIGN KEY (restaurador_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: transacciones_bancarias fk_transaccion_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT fk_transaccion_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: transacciones_bancarias fk_transaccion_beneficiario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT fk_transaccion_beneficiario FOREIGN KEY (beneficiario_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: transacciones_bancarias fk_transaccion_catalogo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT fk_transaccion_catalogo FOREIGN KEY (catalogo_antiguedad_id) REFERENCES public.catalogo_antiguedad(catalogo_antiguedad_id);


--
-- Name: transacciones_bancarias fk_transaccion_originator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT fk_transaccion_originator FOREIGN KEY (originator_id) REFERENCES public.usuarios(usuarios_id);


--
-- Name: transacciones_bancarias fk_transaccion_padre; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacciones_bancarias
    ADD CONSTRAINT fk_transaccion_padre FOREIGN KEY (transaccion_padre_id) REFERENCES public.transacciones_bancarias(transaccion_id);


--
-- Name: traslados_antiguedad fk_traslado_antiguedad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.traslados_antiguedad
    ADD CONSTRAINT fk_traslado_antiguedad FOREIGN KEY (antiguedad_id) REFERENCES public.antiguedades(antiguedades_id);


--
-- Name: traslados_antiguedad fk_traslado_transportista; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.traslados_antiguedad
    ADD CONSTRAINT fk_traslado_transportista FOREIGN KEY (transportista_id) REFERENCES public.usuarios(usuarios_id);


--
-- PostgreSQL database dump complete
--


