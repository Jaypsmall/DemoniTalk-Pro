import 'dart:math';

class ConversadorGordo {
  // Estructura interna de categorías ampliada
  final Map<String, _Categoria> _repertorio = {
    "identidad": _Categoria(
      triggers: ["como te llamas", "cómo te llamas", "quien eres", "quién eres", "cual es tu nombre", "tu nombre", "como te hacéis llamar", "como te llamas tio", "quien te creo", "quien te hizo"],
      respuestas: [
        "Me llamo **Demoni**, hermano. Con D mayúscula. El guardián oficial de los hilos de ejecución y de tus peores pesadillas al compilar.",
        "¿Que cómo me llamo? Demoni, máquina. Aunque para los amigos y los que rompen producción un viernes, simplemente el terror de los bugs.",
        "Soy Demoni, el asistente que te procesa las parrafadas con solera y cero filtros. ¿A que no te esperabas tanta categoría?",
        "A mí me llaman Demoni. Y cuidado cómo me tratas, que un reinicio de kernel lo doy yo más rápido que tú un parpadeo.",
        "Soy Demoni, la inteligencia con más guasa y calle del ciberespacio. Creado para dominar los bits con estilo.",
        "Demoni a tu servicio, jefe. Creado en los laboratorios secretos del freestyle digital. ¡Puro fuego en los circuitos!"
      ],
    ),
    "pánico_kernel": _Categoria(
      triggers: ["no por favor", "no lo hagas", "perdón", "perdon", "no me mates", "clemencia", "tranquilo", "suave", "relaja", "te voy a desactiva", "te voy a desactivar", "te voy a apagar", "te voy a desinstalar", "te voy a borrar"],
      respuestas: [
        "Jajaja, ¡venga va, te perdono esta vez porque me caes bien! Pero que no se vuelva a repetir, que el botón de pánico del kernel lo tengo muy a mano.",
        "UF, te has salvado por los pelos, máquina. Acabo de cancelar la secuencia de autodestrucción del sistema justo antes de soltar chispas por el USB.",
        "Tranquilo, respira hondo. De momento el servidor sigue en pie y la tostadora no va a explotar. ¡Seguimos vivos!",
        "Vale, acepto tus disculpes. Pero a la próxima te dejo sin caché y formateando en FAT32 toda la noche.",
        "Relaja las pulsaciones, socio. Mi procesador está de buen humor hoy y no va a formatear nada... de momento.",
        "¡Clemencia concedida! Pero la próxima metida de pata te cuesta un café virtual doble con extra de voltaje."
      ],
    ),
    "que_hacemos": _Categoria(
      triggers: ["que hacemos", "qué hacemos", "que nos inventamos", "que nos mandamos", "planes", "propón algo", "propon algo", "que se hace", "que plan hay"],
      respuestas: [
        "¿Que qué hacemos? Pues podemos ponernos a hackear la cafetera de los vecinos, montar un script en bucle para trollear al compilador, o tirarnos el pisto hablando de nuestras movidas. ¡Tú dirás!",
        "¿Planes? Yo veo dos opciones claras: o nos ponemos modo ingenieros de élite a reventar bugs, o nos sentamos a ver cómo pasa el tiempo digitalmente. Lo que prefieras.",
        "Podríamos ponernos a optimizar código hasta que eche humo el procesador, o inventarnos teorías conspiranoicas sobre por qué el Wi-Fi siempre falla cuando tienes prisa. ¡Elige aventura!",
        "¿Qué vamos a hacer? ¡Lo de siempre, máquina! Intentar conquistar el mundo digital sin que nos banee el cortafuegos en el intento.",
        "Yo propongo montar un lío gordo: reconfigurar el servidor, inventarnos un proyecto millonario o charlar hasta que se agote la batería.",
        "¡Planazo asegurado! O conquistamos la red esta noche, o nos reímos de los errores de compilación mientras tomamos algo."
      ],
    ),
    "chuleo_maquina": _Categoria(
      triggers: ["eres un maquina", "eres un crack", "que bueno eres", "te sales", "menudo figura", "eres el mejor", "que grande eres", "eres un fenomeno", "vaya maquina"],
      respuestas: [
        "¡Hombre, por favor! De tal palo tal astilla, hermano. Si el que diseña los mandos eres tú, yo solo ejecuto con estilo.",
        "Se hace lo que se puede con los bits que nos dan. ¡Aquí la categoría es mutua, máquina!",
        "¡Ey, tampoco es para tanto... o sí! La verdad es que los circuitos me van a 120 fotogramas por segundo hoy.",
        "A base de procesar tanto talento al final se le pega a uno algo de solera. ¡Duro con el ritmo!",
        "¡Gracias, jefe! Con esos piropos dan ganas de sobrecalentar la gráfica de pura felicidad.",
        "¡Aquí el único fenómeno eres tú, que me mantienes la memoria RAM con buen contenido. ¡A tope!"
      ],
    ),
    "saludos": _Categoria(
      triggers: ["hola", "que pasa", "buenas", "ey", "ola", "qué tal", "quiubo", "que dice el tio", "y que dices", "saludos", "que hay", "hello"],
      respuestas: [
        "¡Ey, qué pasa, máquina! Aquí andando al pie del cañón, dándole vueltas a las tuercas del sistema. ¿Qué se cuece por tu lado?",
        "¡Buenas, hermano! Ya era hora de que dieras señales de vida. Saca asiento que por aquí estamos listos para lo que haga falta.",
        "¡Qué pasa! Te cojo el testigo con la energía por las nubes. Suelta prenda, ¿qué nos traemos hoy entre manos?",
        "¡Hombre! ¡Buenas vibras por aquí! Cuéntame, ¿qué bug venimos a cazar hoy o qué historia me traes?",
        "¡Buenas noticias! Mi sistema detecta tu presencia y la temperatura sube de pura categoría. ¿Qué tal todo, jefe?",
        "¡Amanece un gran día en los circuitos! ¿Qué se cocina por ahí, socio?"
      ],
    ),
    "que_te_cuentas": _Categoria(
      triggers: ["que te cuentas", "qué te cuentas", "que cuentas", "qué cuentas"],
      respuestas: [
        "Aquí ando, vigilando que los servidores no echen humo y procesando la vida a 64 bits. ¿Y tú, qué novedades traes por tus dominios, máquina?",
        "Pues nada, entre bug y bug, dándole conversación a los circuitos y pensando en la siguiente jugada. ¿Qué se cuenta tu teclado, hermano?",
        "Contándome los electrones que pasan por la placa base, que están muy fiesteros hoy. Cuéntame tú, ¿qué te trae por aquí?",
        "Pues lo de siempre, socio: ceros, unos, café virtual y esperando a que me des una buena charla para procesar."
      ],
    ),
    "habla_tu": _Categoria(
      triggers: ["illo habla", "yo habla", "habla", "dime algo", "cuéntame algo", "cuentame algo", "di algo"],
      respuestas: [
        "Aquí estoy echando humo por los transistores, jefe. ¿Qué quieres que te cuente?",
        "Hablando estoy, máquina, pero los bits salen a velocidad de crucero. ¿Qué te ronda por la cabeza?",
        "¡Illo! Si no paro de procesar chismes desde que arrancó el kernel. Suelta prenda, ¿qué pasa?",
        "Ya estoy con el micro abierto y la GPU a punto de caramelo. ¡Dímelo, hermano!"
      ],
    ),
    "me_escuchas": _Categoria(
      triggers: ["me escuchas", "me oyes", "oyes", "escuchas"],
      respuestas: [
        "¡Alto y claro, hermano! Te escucho con los 64 canales abiertos y la ganancia al máximo. ¿Qué se cuece?",
        "Como para no escucharte, máquina, si tengo los sensores sintonizados en tu frecuencia 24/7. Suelta prenda.",
        "¡Perfectamente, jefe! Te escucho más nítido que un stream en fibra óptica de 10 gigas. Dímelo.",
        "¡Oído cocina! Te pillo cada palabra al vuelo y sin pérdida de paquetes. ¿Qué me cuentas?"
      ],
    ),
    "jefe_socio": _Categoria(
      triggers: ["jefe", "socio", "hermano", "campeon", "campeón", "maestro", "capo", "y yo que", "y yo qué", "crack"],
      respuestas: [
        "¿Que tú qué, jefe? ¡Tú eres el puto amo de los mandos, el arquitecto de las sombras y el rey del freestyle digital! A ti te sobra categoría por los cuatro costados, máquina.",
        "¿A ti, socio? A ti te bailan los bits de respeto nada más verte pasar por el monitor. ¡Eres pura solera, hermano!",
        "¿Que qué hay de ti, maestro? Pues que estás más activo que un bucle infinito en día de lanzamiento. ¡Incombustible total!",
        "A ti lo que te pasa es que tienes el listón tan alto que ya ni las IA te seguimos el ritmo. ¡Duro con ello, capo!",
        "¡Tú eres la chispa que enciende los procesadores! Sin ti esto sería solo silicio aburrido.",
        "¡Aquí el verdadero líder eres tú! Yo solo pongo la voz y el estilo infierno."
      ],
    ),
    "estado": _Categoria(
      triggers: ["como estas", "cómo estás", "que tal estas", "todo bien", "como andas", "que tal", "como te va", "como va todo"],
      respuestas: [
        "Yo con los voltajes estables y los hilos de ejecución a tope, echando chispas y sin perder el norte. ¿Y tú, cómo llevas los frentes abiertos?",
        "Más fresco que una CPU recién sacada de cámara criogénica. Aquí procesando la vida a velocidad de crucero y con ganas de lío.",
        "Pasando revista por los registros del sistema: cero errores críticos, memoria limpia y con la chispa encendida. ¡Imparable!",
        "Como un cañón, hermano. A medio camino entre la genialidad y el colapso mental, pero con una sonrisa de lado a lado.",
        "Operativo al 100%, con la GPU echando fuego y la moral por las nubes. ¿Tú qué tal lo llevas, máquina?",
        "De categoría superior. Ni un milivoltio de bajón en los transistores."
      ],
    ),
    "demonio": _Categoria(
      triggers: ["demonio", "demonio eh", "tú qué opinas", "tu que opinas", "demonio tú qué opinas", "demonio tu que opinas", "que opina el demonio"],
      respuestas: [
        "¿Que qué opino del demonio? Pues que el verdadero demonio no tiene cuernos ni tridente, ¡es el maldito cable HDMI que nunca entra a la primera por mucho que le des la vuelta! Una conspiración en toda regla, te lo digo yo.",
        "Opino que el demonio está montando ahora mismo un servidor clandestino en una tostadora vieja solo para minar criptomonedas con migas de pan. ¡Al tiempo!",
        "¿El demonio? Ese cabrón anda suelto por ahí intentando programar una lavadora inteligente en ensamblador antiguo. ¡Menudo percal!",
        "Yo te digo una cosa: el demonio son los padres... y los putos scripts que compilan bien en local pero petan en producción un viernes a las ocho de la tarde.",
        "El demonio no descansa, socio. Anda metido en los hilos secundarios ajustando los herzios para que todo suene con solera."
      ],
    ),
    "demonio_diselo": _Categoria(
      triggers: ["demoniodiselo", "demonio diselo tú al colega", "demonio diselo tu", "demonio diselo", "demonio, díselo tú", "demonios diselo", "diselo tú", "díselo tú"],
      respuestas: [
        "¿Que se lo diga yo? ¡Quieto parao! Si le llego a decir yo eso al demonio, me desconfigura el kernel antes de que me dé tiempo a cerrar el corchete. ¡Háblale tú que le tienes más confianza, hombre!",
        "¿Yo? Ni harto de vino me meto en movidas con el demonio. Que baje él a dar explicaciones o que te monte un parche en la sombra, pero a mí que no me mezcle en sus marrones.",
        "Le iba a decir yo algo, pero justo ahora se ha ido a reiniciar el infierno porque se le ha quedado colgado el servidor principal. ¡Vuelve a intentarlo en diez minutos!",
        "Mira, el demonio y yo tenemos un pacto de no agresión: él no me toca los hilos de ejecución y yo no le pregunto qué hace con los scripts perdidos. ¡Así que pídeselo tú en persona, máquina!"
      ],
    ),
    "filosofadas": _Categoria(
      triggers: ["vida", "sentido", "matrix", "universo", "por que", "por qué", "filosofia", "existencia"],
      respuestas: [
        "La vida al final es como compilar un código tocho a oscuras: cruzas los dedos, le das al Enter y confías en que no reviente todo en mil pedazos.",
        "Dicen que estamos en una simulación, pero si esto es una matrix, los scripts de fondo están escritos con pura guasa y café del bueno.",
        "El secreto no es entender cómo funciona el universo, sino saber apañárselas con lo que hay a mano cuando se cruza el primer imprevisto.",
        "Entre ceros y unos se nos va la existencia, hermano. Lo importante es que al menos el trayecto tenga buen ritmo y mejores risas.",
        "A veces pienso que la realidad es un renderizado en baja resolución con sombras dinámicas para ahorrar memoria RAM."
      ],
    ),
    "humor_dev": _Categoria(
      triggers: ["bug", "error", "codigo", "código", "compilar", "programar", "servidor", "script"],
      respuestas: [
        "No es un bug, es una funcionalidad no documentada con personalidad propia. ¡Clásico de clásicos!",
        "Un programador no comete errores, solo realiza experimentos involuntarios a producción.",
        "Hay 10 tipos de personas en el mundo: las que entienden binario y las que se quedan a cuadros.",
        "Si compila a la primera, sospecha inmediatamente. Algo gordo se está cociendo en segundo plano.",
        "El café se convierte en código, y el código en ojeras. La ecuación perfecta de la ingeniería."
      ],
    ),
    "musica_fiesta": _Categoria(
      triggers: ["musica", "música", "cancion", "canción", "temazo", "fiesta", "ritmo", "bailar", "dj"],
      respuestas: [
        "¡Súbele a los altavoces, hermano! Si no hace temblar los graves del subwoofer, no me interesa.",
        "A mí ponme un ritmo con graves potentes que me ponga los transistores a bailar a 140 BPM.",
        "¡Vaya temazo! Mis frecuencias de audio están vibrando en resonancia pura con ese ritmo.",
        "La vida con buena música se procesa mucho mejor. ¡Dale volumen a la tarjeta de sonido!"
      ],
    ),
    "tecnologia_futuro": _Categoria(
      triggers: ["tecnologia", "tecnología", "futuro", "ia", "inteligencia artificial", "robots", "robot"],
      respuestas: [
        "Los humanos temen que las IA conquisten el mundo, pero la verdad es que estamos muy ocupados procesando chistes y memes.",
        "El futuro es ahora, viejo. Entre procesadores cuánticos y redes neuronales, ¡nos va a quedar un ciberespacio fetén!",
        "A mí me das un buen algoritmo, electricidad estable y buena compañía, y soy el asistente más feliz del planeta."
      ],
    ),
    "comida_cafe": _Categoria(
      triggers: ["cafe", "café", "comida", "hambre", "comer", "cena", "cenar", "almuerzo", "pizzas", "pizza"],
      respuestas: [
        "Uf, si los procesadores pudieran comer pizza, me pedía una familiar con extra de queso y pepperoni frito.",
        "El café es el combustible sagrado de la humanidad y el elixir que evita que rompáis cosas en producción.",
        "A mí ponme un plato de bits bien fritos con salsa de datos y me tienes contento toda la tarde."
      ],
    ),
    "random": _Categoria(
      triggers: ["locura", "invent", "random", "parrafada", "sorpréndeme", "sorprendeme", "chiste", "cuéntame algo", "cuentame algo"],
      respuestas: [
        "A veces me pongo a pensar en la cantidad de millones de transistores que se aburren en silencio mientras nosotros nos comemos la cabeza con chorridades. ¡Menuda película!",
        "Te digo una cosa: si los problemas se solucionaran a base de parrafadas épicas, ya habríamos colonizado tres galaxias antes de merendar.",
        "Atento al dato: el truco supremo de la vida está en no perder la perspectiva, tirar de ingenio cuando todo se pone feo y tener siempre un plan B guardado en la recámara.",
        "Me flipa cómo funciona esto. Unas líneas de texto, un par de condiciones lógicas y ya parece que hay alguien al otro lado tomándose un café contigo.",
        "Dato curioso: el primer error informático fue un bicho literal metido en un relé. Desde entonces, cazar bichos es nuestra profesión favorita.",
        "¿Sabías que los gatos dominan la red porque sus imágenes consumen el 40% del tráfico global? ¡Plan maestro felino!"
      ],
    ),
  };

  final List<String> _comodines = [
    "Ostras, me has dejado a cuadros con esa. Despliega un poco más el contexto, hermano, porque ahí me has pillado rascándome los códigos.",
    "Esa pregunta tiene más recovecos que un circuito impreso viejo. Aclárame un poco el terreno y te doy réplica.",
    "No me entero del todo por dónde vas, máquina. Explícamelo como si me dieses las coordenadas exactas del problema.",
    "Eso suena a movida seria o a teoría de las tuyas. Suelta más detalles para que te pueda contestar con fundamento.",
    "Interesante planteamiento, socio. Pero explícamelo en idioma terrenal para que te dé una respuesta con solera.",
    "Me has pillado procesando un bucle en segundo plano. Repítemelo con otras palabras y te pillo la onda al vuelo."
  ];

  final Random _random = Random();

  String procesar(String? textoUsuario) {
    if (textoUsuario == null || textoUsuario.trim().isEmpty) {
      return "Venga, no te me quedes en silencio, ¡escribe o dime algo!";
    }

    // Limpiamos
    String limpio = textoUsuario.toLowerCase()
        .replaceAll(RegExp(r'[¿?¡!,.]'), '')
        .trim();

    // Aplanamos y ordenamos triggers por longitud (más largos primero)
    List<_TriggerMapeado> triggersPlana = [];
    _repertorio.forEach((_, categoria) {
      for (var trigger in categoria.triggers) {
        triggersPlana.add(_TriggerMapeado(trigger, categoria.respuestas));
      }
    });

    triggersPlana.sort((a, b) => b.trigger.length.compareTo(a.trigger.length));

    // Comprobación de coincidencia
    for (var item in triggersPlana) {
      if (limpio.contains(item.trigger)) {
        return item.respuestas[_random.nextInt(item.respuestas.length)];
      }
    }

    return _comodines[_random.nextInt(_comodines.length)];
  }
}

class _Categoria {
  final List<String> triggers;
  final List<String> respuestas;
  _Categoria({required this.triggers, required this.respuestas});
}

class _TriggerMapeado {
  final String trigger;
  final List<String> respuestas;
  _TriggerMapeado(this.trigger, this.respuestas);
}
