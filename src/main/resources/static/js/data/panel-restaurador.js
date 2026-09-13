
// Datos: solicitudes y restauraciones activas del dashboard de restaurador

// ── SOLICITUDES PARA RESTAURADOR ───────────────────────────────
const RESTAURADOR_REQUESTS_DEFAULT = [
    {
        id: 'req1',
        pieza: 'Cómoda victoriana con marquetería',
        propietario: 'Patricia García',
        fecha: '2025-03-20',
        estado: 'pendiente',
        descripcion: 'Cómoda de finales del siglo XIX con marquetería deteriorada. Requiere consolidación estructural y restauración de superficies.',
        presupuesto_estimado: '$12,000 – $16,000 MXN',
        img: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=300&q=70',
    },
    {
        id: 'req2',
        pieza: 'Mesa de centro Art Déco',
        propietario: 'Roberto Suárez',
        fecha: '2025-03-19',
        estado: 'pendiente',
        descripcion: 'Mesa de centro con tablero de mármol y base en hierro forjado. Presenta oxidación en la base y una fisura en el mármol.',
        presupuesto_estimado: '$6,000 – $9,000 MXN',
        img: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=300&q=70',
    },
];

const RESTAURADOR_ACTIVE_DEFAULT = [
    {
        id: 'act1',
        pieza: 'Vitrina colonial de nogal',
        propietario: 'Jennifer Torres',
        estado_afd: 'q9',
        progreso: 65,
        inicio: '2025-03-10',
        fin_estimado: '2025-04-01',
        img: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=300&q=70',
    },
];