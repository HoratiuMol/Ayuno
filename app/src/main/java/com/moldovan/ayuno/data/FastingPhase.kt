package com.moldovan.ayuno.data

data class FastingPhase(
    val name: String,
    val startHour: Int,
    val endHour: Int,
    val description: String,
    val motivation: String,
    val benefits: List<String>,
    val cautions: List<String>,
    val hungerLevel: String,
    val hungerEmoji: String
)

val FASTING_PHASES = listOf(
    FastingPhase(
        name        = "Fase postprandial",
        startHour   = 0,
        endHour     = 6,
        description = "Tu cuerpo está utilizando la energía de los alimentos que acabas de comer. El páncreas produce insulina para usar la glucosa y almacenar el exceso como glucógeno y grasa.",
        motivation  = "La disciplina empieza cuando termina comer",
        benefits    = listOf(
            "Digestión activa y absorción de nutrientes",
            "Almacenamiento de glucógeno hepático y muscular"
        ),
        cautions    = listOf(
            "No es recomendable hacer ejercicio intenso justo después de comer"
        ),
        hungerLevel = "Nulo",
        hungerEmoji = "😌"
    ),
    FastingPhase(
        name        = "Quema de reservas",
        startHour   = 6,
        endHour     = 16,
        description = "Tu cuerpo comienza a usar las reservas de glucógeno. La glucosa almacenada en el hígado mantiene los niveles en sangre. Se activan la gluconeogénesis y la lipólisis.",
        motivation  = "Tu cuerpo aprende a usar reservas",
        benefits    = listOf(
            "Se inicia la quema de grasa almacenada",
            "Producción de cuerpos cetónicos para energía",
            "Comienza la autofagia (renovación celular)",
            "Aumento de sensibilidad a la insulina"
        ),
        cautions    = listOf(
            "Puedes sentir hambre o ligera irritabilidad",
            "Mantente bien hidratado/a"
        ),
        hungerLevel = "Creciente",
        hungerEmoji = "😐"
    ),
    FastingPhase(
        name        = "Cetosis temprana",
        startHour   = 16,
        endHour     = 24,
        description = "La glucosa en las células y el glucógeno se agotan. Tu cuerpo quema grasa almacenada como fuente principal de energía. La autofagia se intensifica.",
        motivation  = "La incomodidad forja control y claridad",
        benefits    = listOf(
            "Quema activa de grasa corporal",
            "Autofagia más intensa: limpieza celular",
            "Regulación del perfil lipídico",
            "Mejora de la sensibilidad a la insulina"
        ),
        cautions    = listOf(
            "Posible dolor de cabeza si no estás hidratado",
            "No recomendado sin experiencia previa en ayunos"
        ),
        hungerLevel = "Alto",
        hungerEmoji = "😣"
    ),
    FastingPhase(
        name        = "Cetosis profunda",
        startHour   = 24,
        endHour     = 72,
        description = "Tu cuerpo entra en cetosis plena: quema reservas de grasa para energía. Los cuerpos cetónicos actúan como combustible para el cerebro.",
        motivation  = "Ahora quemas grasa, sigue adelante",
        benefits    = listOf(
            "Rendimiento cognitivo mejorado y claridad mental",
            "Mayor sensación de energía y bienestar",
            "Reducción de triglicéridos y colesterol LDL",
            "Renovación celular profunda (autofagia)",
            "Posible efecto preventivo contra el cáncer y el envejecimiento"
        ),
        cautions    = listOf(
            "Requiere supervisión médica",
            "No apto para principiantes",
            "Asegúrate de tomar agua, infusiones y electrolitos",
            "Detener si aparecen mareos persistentes o debilidad"
        ),
        hungerLevel = "Decreciente",
        hungerEmoji = "🙂"
    ),
    FastingPhase(
        name        = "Cetosis extendida",
        startHour   = 72,
        endHour     = 96,
        description = "Estado profundo de cetosis. Todos los órganos usan cuerpos cetónicos y grasas. Las hormonas tiroideas pueden verse afectadas.",
        motivation  = "Tu cuerpo se renueva desde dentro",
        benefits    = listOf(
            "Máxima autofagia y renovación celular",
            "Mayor resistencia al estrés y toxinas",
            "El hambre tiende a disminuir a partir del tercer día"
        ),
        cautions    = listOf(
            "⚠️ SOLO con supervisión médica estricta",
            "Las hormonas tiroideas pueden alterarse",
            "El metabolismo puede verse afectado negativamente",
            "No apto para personas con TCA, embarazadas, niños o ancianos",
            "Detener inmediatamente si hay desmayos o confusión"
        ),
        hungerLevel = "Bajo",
        hungerEmoji = "😶"
    )
)