// ═══════════════════════════════════════════════════════════════
// MJ RENEW — afd-estados.js
// Datos: estados del proceso de restauracion (AFD)
// ═══════════════════════════════════════════════════════════════

// ── AFD: ESTADOS DEL PROCESO ───────────────────────────────────
const AFD_STATES = {
    q0:  { id:'q0',  label:'Pieza capturada',           phase:'Captura' },
    q1:  { id:'q1',  label:'En evaluación',             phase:'Evaluación' },
    q2:  { id:'q2',  label:'Calculando presupuesto',    phase:'Evaluación' },
    q3:  { id:'q3',  label:'Presupuesto presentado',    phase:'Evaluación' },
    q4:  { id:'q4',  label:'Pago en escrow',            phase:'Evaluación' },
    q5:  { id:'q5',  label:'Recolección agendada',      phase:'Logística ida' },
    q6:  { id:'q6',  label:'En recolección',            phase:'Logística ida' },
    q7:  { id:'q7',  label:'Recibido en taller',        phase:'Logística ida' },
    q8:  { id:'q8',  label:'En restauración',           phase:'Restauración' },
    q9:  { id:'q9',  label:'Avance publicado',          phase:'Restauración' },
    q10: { id:'q10', label:'Restauración lista',        phase:'Restauración' },
    q11: { id:'q11', label:'Decisión en taller',        phase:'Restauración' },
    q12: { id:'q12', label:'Entrega agendada',          phase:'Logística vuelta' },
    q13: { id:'q13', label:'En devolución',             phase:'Logística vuelta' },
    q14: { id:'q14', label:'Entregado al dueño',        phase:'Logística vuelta' },
    q15: { id:'q15', label:'En catálogo',               phase:'Venta' },
    q16: { id:'q16', label:'Link de pago enviado',      phase:'Venta' },
    qD1: { id:'qD1', label:'Disputa abierta',           phase:'Disputa', cancel:true },
    qD2: { id:'qD2', label:'En revisión admin',         phase:'Disputa', cancel:true },
    qD3: { id:'qD3', label:'Disputa resuelta',          phase:'Disputa', cancel:true },
    qC1: { id:'qC1', label:'No cumple requisitos',      phase:'Cancelación', cancel:true },
    qC2: { id:'qC2', label:'Dueño no quiso restaurar',  phase:'Cancelación', cancel:true },
    qC3: { id:'qC3', label:'Nadie en casa – recolección', phase:'Cancelación', cancel:true },
    qC4: { id:'qC4', label:'Nadie en casa – entrega',   phase:'Cancelación', cancel:true },
    qF1: { id:'qF1', label:'Restauración completada',   phase:'Finalizado', success:true },
    qF2: { id:'qF2', label:'Venta completada',          phase:'Finalizado', success:true },
};

// Secuencia visual del tracker (main happy path)
const AFD_TRACKER_STEPS = [
    'q0','q1','q2','q3','q4','q5','q6','q7','q8','q9','q10','q11','q12','q13','q14','q15','qF1'
];