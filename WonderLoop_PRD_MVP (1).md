# WonderLoop — Product Requirements Document (PRD)

**Versión:** 1.0  
**Alcance:** MVP para hackathon  
**Plataforma:** App android and ios 
**Público objetivo:** niños de 4 a 12 años, con gestión y consentimiento por parte de padres/tutores  
**Idioma inicial:** español; arquitectura preparada para multidioma  

---

## 1. Resumen del producto

WonderLoop es una plataforma de creatividad infantil que conecta el mundo físico y el digital. El niño dibuja en papel un personaje libremente, la aplicación interpreta su creación y la convierte en el protagonista de una aventura ilustrada y narrada. La historia se adapta a la edad del niño y, cuando procede, permite tomar decisiones que afectan a su desarrollo.

La experiencia no termina al finalizar el cuento. Cada historia propone un nuevo reto creativo que anima al niño a volver al papel: dibujar un amigo, un accesorio, un lugar u otro elemento relacionado con la aventura. Ese nuevo dibujo puede iniciar o continuar una historia posterior.

El producto no busca aumentar el tiempo de pantalla. Su objetivo es utilizar la pantalla como puente para estimular la creación fuera de ella.

**Propuesta de valor principal:** La IA devuelve a los niños al papel y utiliza sus propias creaciones para desarrollar un universo narrativo que evoluciona con ellos.

---

## 2. Visión y principios de producto

### 2.1 Visión

Convertir la IA en un puente entre la imaginación infantil y el mundo real, fomentando un hábito continuo de creación mediante dibujo, narración e interacción.

### 2.2 Principios

1. **Paper first.** La experiencia comienza en el mundo físico y el dibujo infantil es el origen del contenido.
2. **AI second.** La IA interpreta, amplifica y da vida a la creación; no sustituye el dibujo del niño.
3. **Create again.** Las historias deben incentivar una nueva creación física.
4. **Child-safe by design.** La seguridad y la adecuación por edad son requisitos funcionales, no complementos.
5. **No passive-content loop.** El producto no debe optimizar el tiempo de pantalla como objetivo principal.
6. **The child remains the author.** La identidad visual y narrativa del personaje deben conservar el vínculo con el dibujo original.

---

## 3. Objetivos del MVP

### Objetivos principales

- Permitir que un padre cree un perfil infantil y que el niño genere una primera historia a partir de un dibujo en papel.
- Interpretar visualmente el dibujo y convertirlo en un personaje principal consistente.
- Generar una historia ilustrada, narrada y adaptada a la edad.
- Permitir interactividad narrativa para las edades en las que tenga sentido.
- Crear un ciclo de continuidad mediante retos creativos físicos.
- Guardar el personaje principal y las historias para poder continuar el universo.
- Garantizar una experiencia segura para menores.
- Demostrar en el hackathon el concepto de creatividad físico-digital de extremo a extremo.

### Objetivos secundarios

- Crear una experiencia visual y emocionalmente memorable.
- Facilitar que los padres compartan el resultado.
- Sentar las bases para una plataforma de historias persistentes y futuras capacidades educativas.

---

## 4. No objetivos del MVP

Quedan fuera del alcance inicial:

- Aplicación nativa iOS/Android.
- Vídeo animado generado por IA.
- Chat abierto e ilimitado con personajes.
- Red social infantil o mensajería entre menores.
- Marketplace o tienda de contenidos.
- Integración completa de impresión bajo demanda.
- Libros físicos personalizados como flujo operativo real.
- Sistema avanzado de gamificación basado en puntos, niveles o rankings.
- Seguimiento clínico o evaluación formal de capacidades cognitivas.
- Reconocimiento facial del niño.

Estas capacidades podrán formar parte del roadmap posterior, pero no deben comprometer la entrega del MVP.

---

## 5. Usuarios y roles

### 5.1 Padre/tutor

Es el responsable de la cuenta y del consentimiento. Gestiona los perfiles infantiles, configuración, biblioteca y opciones de compartición.

### 5.2 Niño

Es el usuario final de la experiencia creativa. Interactúa principalmente mediante cámara, botones visuales y elecciones sencillas. No debe ser responsable de introducir datos personales sensibles.

### 5.3 Sistema WonderLoop

Genera y adapta historias, interpreta dibujos, mantiene la continuidad narrativa y aplica las reglas de seguridad.

---

## 6. Concepto principal: Creativity Loop

El flujo central del producto es:

**Imaginar → Dibujar → Escanear → Interpretar → Dar vida → Vivir la aventura → Recibir un reto creativo → Volver al papel → Continuar o crear una nueva historia.**

El objetivo es que la finalización de una historia produzca una nueva intención de creación física.

La historia no debe terminar únicamente con un botón de "fin". Debe terminar con una propuesta concreta para continuar creando.

Ejemplos:

- "Mañana dibuja un amigo para nuestro héroe."
- "Dibuja el nuevo lugar al que viajará."
- "Dibuja el accesorio mágico que necesitará."

El niño puede continuar la aventura cuando quiera; el sistema puede recomendar retomarla al día siguiente, pero no debe bloquear deliberadamente la creatividad para aumentar la retención.

---

## 7. Experiencia principal

### 7.1 Alta inicial

El padre/tutor crea la cuenta y configura un perfil infantil.

Datos mínimos del perfil infantil:

- Nombre de pila o apodo.
- Franja de edad o edad necesaria para la adaptación del contenido.
- Idioma.

La aplicación no debe requerir fecha de nacimiento exacta ni datos personales innecesarios para el MVP.

### 7.2 Inicio

La pantalla principal debe priorizar dos acciones:

- **Nueva aventura**
- **Mi universo**

La interfaz infantil debe ser visual, sencilla y táctil, evitando menús complejos.

### 7.3 Crear nueva aventura

El sistema invita al niño a dibujar un personaje en papel.

El niño puede dibujar libremente. No se exige un estilo, especie o escenario concreto.

La edad determina si, además del personaje, se puede incentivar un accesorio o un lugar, pero el punto de entrada siempre debe ser el personaje principal.

### 7.4 Captura

El usuario realiza una fotografía del dibujo desde la cámara del dispositivo o sube una imagen existente.

La experiencia debe:

- mostrar una guía visual para centrar el papel;
- permitir confirmar la captura;
- permitir un recorte sencillo si es necesario;
- evitar pedir edición compleja al niño.

### 7.5 Interpretación

El sistema analiza el dibujo y extrae una representación estructurada del personaje principal.

Debe identificar, cuando sea posible:

- tipo o especie aparente;
- colores predominantes;
- rasgos visuales relevantes;
- objetos o accesorios visibles;
- estilo del dibujo;
- elementos secundarios que puedan utilizarse en la historia;
- nivel de confianza de la interpretación.

La aplicación debe mantener el dibujo original como referencia permanente.

### 7.6 Confirmación del héroe

Cuando la imagen contiene elementos claramente diferenciados, el sistema puede pedir al niño que confirme cuál es el protagonista. La interacción debe ser visual y simple.

Para el MVP, se garantiza consistencia visual del personaje principal. Los demás elementos pueden tratarse como secundarios, decorativos o elementos de la escena.

---

## 8. Adaptación por edad

La edad no debe modificar únicamente el vocabulario. Debe controlar toda la experiencia narrativa.

La adaptación debe contemplar:

- longitud del cuento;
- dificultad del vocabulario;
- longitud de las frases;
- tono;
- humor;
- temas;
- complejidad narrativa;
- cantidad de decisiones;
- tipo de retos;
- intensidad emocional;
- cantidad de texto mostrado;
- velocidad y estilo de narración.

### 8.1 Grupo aproximado 4-6 años

Experiencia principalmente lineal.

Características:

- historias cortas;
- lenguaje sencillo;
- fuerte componente visual y auditivo;
- decisiones limitadas o inexistentes;
- conflictos suaves y resolución positiva;
- retos creativos muy simples.

### 8.2 Grupo aproximado 7-10 años

Experiencia interactiva recomendada.

Características:

- historias más largas;
- mayor desarrollo de personajes;
- decisiones narrativas;
- pequeños misterios, problemas o acertijos adecuados a la edad;
- mayor riqueza de vocabulario;
- retos creativos con más libertad.

La segmentación es orientativa. El sistema debe poder ajustar la experiencia mediante configuración parental en futuras versiones.

---

## 9. Formato del cuento

El MVP utilizará un **libro digital interactivo**, no vídeo.

Cada cuento estará compuesto por varias páginas o escenas.

Cada página podrá contener:

1. Ilustración principal.
2. Texto narrativo.
3. Narración por voz.
4. Música o sonido ambiental cuando proceda.
5. Control de navegación.
6. Elementos interactivos cuando corresponda.

El MVP deberá generar aproximadamente 5-6 páginas por historia, ajustables según edad y coste.

Duración objetivo aproximada: 3-6 minutos por historia.

---

## 10. Tratamiento visual del dibujo

El dibujo original nunca debe ser presentado como un error que necesita corrección.

El objetivo visual es **preservar la identidad del dibujo y embellecerlo**, no sustituirlo por un personaje genérico de aspecto profesional.

La experiencia debe poder mostrar:

- dibujo original;
- versión interpretada/estilizada;
- integración del personaje en las escenas de la historia.

La ilustración generada debe conservar los rasgos distintivos relevantes del personaje principal para maximizar la consistencia entre páginas y futuras historias.

---

## 11. Interactividad narrativa

### 11.1 Principio

La interacción debe servir para implicar al niño en la historia, no para convertir la aplicación en un videojuego.

### 11.2 Funcionamiento

En el modo interactivo, algunas páginas presentan una decisión con un máximo de dos opciones sencillas.

Ejemplo:

> El héroe ha encontrado dos caminos. ¿Cruza el río o sube la montaña?

El niño selecciona una opción y la historia continúa teniendo en cuenta dicha elección.

### 11.3 Requisitos

- Las opciones deben ser apropiadas para la edad.
- La decisión debe tener consecuencias narrativas reales.
- No deben existir árboles de decisión innecesariamente complejos en el MVP.
- Las decisiones deben mantenerse dentro de los límites definidos por seguridad y tono.

---

## 12. Reto creativo y continuidad

Esta funcionalidad es obligatoria en el MVP.

Al finalizar una historia, el sistema debe proponer al menos un reto creativo relacionado con la aventura.

El reto debe pedir una nueva creación física.

Tipos de reto:

- nuevo personaje;
- amigo o compañero;
- objeto o accesorio;
- lugar;
- criatura;
- vehículo;
- elemento necesario para resolver un problema.

### 12.1 Regla de continuidad

Cuando el niño decide continuar una aventura existente, el nuevo dibujo debe incorporarse al universo de esa aventura.

Ejemplo:

1. Historia 1: el niño dibuja a Nilo, un dragón.
2. Reto: "Dibuja un amigo para Nilo."
3. El niño dibuja otro personaje.
4. Historia 2: el nuevo personaje aparece como amigo de Nilo.

### 12.2 Nueva historia vs. continuar

Al iniciar una nueva sesión, el niño puede:

- continuar una aventura o personaje existente;
- crear una historia nueva.

Cuando exista una aventura abierta, el sistema debe proponer de forma visible la opción de continuar, sin obligarla.

---

## 13. Universo y memoria narrativa

Cada niño tendrá un universo creativo persistente.

Para el MVP se conservarán principalmente:

### Personaje principal

- nombre;
- descripción visual;
- colores;
- rasgos distintivos;
- personalidad básica;
- valores o atributos narrativos;
- accesorios relevantes;
- imagen original del dibujo;
- representación visual generada.

### Historias

- título;
- fecha;
- resumen;
- páginas;
- decisiones tomadas;
- reto final;
- recursos generados.

### Continuidad

- relaciones conocidas;
- nuevos elementos introducidos;
- lugares relevantes;
- línea temporal resumida.

El MVP debe priorizar la consistencia del personaje principal sobre la complejidad del universo.

---

## 14. Biblioteca

La biblioteca permite al usuario acceder a sus historias anteriores.

Cada historia se presenta como una portada visual que incluye el vínculo con el dibujo original.

Acciones mínimas:

- abrir historia;
- reproducir narración;
- continuar aventura cuando exista un reto pendiente;
- visualizar el dibujo original asociado.

---

## 15. Mi universo

La pantalla debe presentar de forma visual el contenido creado por el niño.

En el MVP debe mostrar como mínimo:

- personaje principal;
- historias creadas;
- retos pendientes.

No es necesario implementar un mapa complejo, sistema de constelaciones ni un museo de contenido en la primera versión.

---

## 16. Compartición

La compartición se gestiona desde el entorno del padre/tutor.

### MVP

Debe poder generarse un recurso compartible con:

- portada;
- nombre/apodo del autor;
- dibujo original;
- ilustración generada;
- título del cuento;
- páginas del cuento o una selección representativa.

### Redes sociales

La compartición directa a Instagram/TikTok no es requisito funcional del MVP. Puede existir como exportación preparada para que el padre comparta manualmente.

El sistema no debe ofrecer perfiles públicos ni interacción social entre niños.

---

## 17. Exportación e impresión

### MVP

Se debe priorizar la generación de un **PDF imprimible** del cuento.

El PDF debe poder incluir:

- portada;
- autoría del niño mediante nombre/apodo;
- páginas ilustradas;
- texto;
- versión de colorear opcional en futuras iteraciones.

### Futuro

- impresión bajo demanda;
- libro de tapa blanda;
- libro de tapa dura;
- colecciones anuales del tipo "Las aventuras de Lucas".

La impresión física se considera una oportunidad de monetización posterior y un elemento potencial de demo, pero no debe bloquear el MVP.

---

## 18. Seguridad infantil

La seguridad es un requisito de primer nivel.

### 18.1 Control parental

- La cuenta la crea el adulto.
- Los perfiles infantiles dependen de la cuenta del adulto.
- El niño no necesita introducir email ni otros datos de contacto.
- El producto debe evitar recopilar información personal innecesaria.

### 18.2 Contenido generado

Todas las entradas y salidas relevantes deben pasar por controles de seguridad.

Se debe controlar:

- violencia;
- contenido sexual o adulto;
- lenguaje inapropiado;
- terror intenso;
- discriminación;
- bullying;
- autolesión;
- información personal sensible;
- solicitudes inadecuadas.

### 18.3 Redirección positiva

Cuando una petición o dibujo pueda conducir a contenido no adecuado, la aplicación debe intentar redirigir la aventura hacia una alternativa apropiada para la edad en lugar de convertir el error en una experiencia negativa.

La política exacta y la implementación deberán alinearse con las herramientas de moderación elegidas durante el desarrollo.

### 18.4 Imágenes

No se deben solicitar ni necesitar fotografías del rostro del niño.

La cámara se utilizará para capturar dibujos.

### 18.5 Privacidad

El producto debe incluir una política de privacidad y mecanismos de eliminación de datos apropiados para el mercado objetivo antes de una distribución pública amplia.

No se utilizarán dibujos infantiles para entrenar modelos públicos sin el consentimiento correspondiente.

---

## 19. Accesibilidad

El MVP debe incluir:

- narración de todos los textos importantes;
- tamaño de texto legible;
- botones grandes;
- alto contraste suficiente;
- navegación táctil sencilla;
- texto sincronizado con la narración cuando sea viable;
- estructura preparada para soporte multidioma.

El soporte específico para dislexia, configuraciones avanzadas de accesibilidad y personalización avanzada podrán ampliarse después del MVP.

---

## 20. Gamificación

El MVP no utilizará un sistema agresivo de puntos, rankings o recompensas por tiempo de pantalla.

Se permite una representación ligera del progreso creativo, siempre vinculada a creación real.

Ejemplos futuros:

- historias creadas;
- personajes creados;
- aventuras continuadas;
- Creativity Streak basado en días con creación, no en días con apertura de la app.

No debe premiarse abrir la aplicación repetidamente.

---

## 21. Funciones imprescindibles del MVP

| Prioridad | Funcionalidad | Criterio de éxito |
|---|---|---|
| P0 | Cuenta de padre/tutor | El adulto puede acceder a la aplicación |
| P0 | Perfil infantil | Se guarda nombre/apodo y edad/franja |
| P0 | Captura de dibujo | Se puede fotografiar o subir un dibujo |
| P0 | Interpretación multimodal | El sistema identifica el personaje principal y sus rasgos |
| P0 | Generación de historia | Se genera una aventura coherente y adaptada a la edad |
| P0 | Ilustraciones | El personaje aparece de forma consistente en las páginas |
| P0 | Narración | El cuento puede escucharse completo |
| P0 | Interactividad por edad | Los mayores pueden tomar decisiones sencillas |
| P0 | Reto creativo final | Cada historia termina proponiendo una nueva creación física |
| P0 | Persistencia | El personaje y la historia quedan guardados |
| P0 | Continuar aventura | Un nuevo dibujo puede continuar una historia existente |
| P0 | Seguridad | El contenido generado respeta las reglas de edad y seguridad |
| P1 | PDF | El cuento puede exportarse para imprimir |
| P1 | Compartición parental | El padre puede generar un recurso para compartir |
| P1 | Mi universo | El niño puede ver personaje, historias y retos |

---

## 22. Funciones deliberadamente fuera del MVP

| Función | Estado |
|---|---|
| Vídeo generado por IA | V2+ |
| Libro físico | V2+ |
| Voz clonada de padres | V2+ |
| Chat libre con personajes | No prioritario |
| Red social infantil | No |
| Comunidad infantil | No |
| Marketplace | No |
| Sistema avanzado de badges | V2 |
| Creativity DNA avanzado | V2 |
| Creativity Constellation | V2 |
| Wonder Museum | V2 |
| Carta para el futuro / cápsula del tiempo | V2+ |
| Reality/AR | V3 |

---

## 23. Requisitos de calidad de la experiencia

La calidad percibida del MVP debe centrarse en cinco momentos:

1. **Dibujo físico:** el niño entiende inmediatamente qué debe hacer.
2. **Transformación:** el usuario percibe que su dibujo ha cobrado vida, no que ha sido reemplazado.
3. **Primer cuento:** el resultado debe sentirse como un libro infantil real, no como texto generado por un chatbot.
4. **Interacción:** las elecciones deben ser comprensibles y tener efecto visible.
5. **Reto final:** el cuento debe terminar con una razón concreta para volver al papel.

---

## 24. Métricas de producto

Las métricas deben reforzar la misión del producto.

### Métricas principales

- Número de dibujos creados.
- Número de historias generadas.
- Número de historias completadas.
- Porcentaje de historias que generan un nuevo reto creativo.
- Porcentaje de retos que terminan en un nuevo dibujo.
- Número de sesiones de continuación de un universo existente.

### Métricas de impacto

- Tiempo de creación física declarado o medido cuando sea posible.
- Días con actividad creativa.
- Número de nuevos personajes, objetos y lugares creados.
- Continuidad de un personaje a través de varias historias.

### Métricas de negocio futuras

- Conversión a plan de pago.
- Exportaciones de PDF.
- Conversión a producto físico.
- Ingreso por familia.

El producto no debe utilizar el tiempo total de pantalla como KPI principal de éxito.

---

## 25. Criterios de aceptación del flujo principal

Una ejecución completa del MVP se considera correcta cuando:

1. Un adulto puede crear el perfil infantil.
2. El niño puede dibujar un personaje libremente en papel.
3. El dibujo puede capturarse desde el navegador móvil.
4. El sistema identifica el personaje y sus características principales.
5. La historia resultante es adecuada para la edad configurada.
6. El personaje mantiene características visuales reconocibles durante el cuento.
7. La historia contiene ilustraciones, texto y narración.
8. El modo interactivo aparece cuando corresponde y las decisiones afectan a la historia.
9. El contenido pasa por controles de seguridad.
10. La historia termina con un reto de creación física.
11. La historia y el personaje quedan almacenados.
12. El usuario puede volver posteriormente y continuar el universo.

---

## 26. Flujo UX completo del MVP

```text
Padre crea cuenta
        |
        v
Crea perfil infantil
        |
        v
       Home
      /    \
     /      \
Nueva       Mi universo
aventura
   |
   v
"Dibuja un personaje"
   |
   v
Fotografía / subida
   |
   v
Confirmar dibujo
   |
   v
Analizando creación
   |
   v
Detectar personaje
   |
   v
Confirmar héroe si es necesario
   |
   v
Planificar historia
   |
   v
Aplicar adaptación por edad + seguridad
   |
   v
Generar cuento
   |
   v
Leer / escuchar
   |
   +---- modo interactivo ----+
   |                           |
   |                       Elegir A/B
   |                           |
   +---------------------------+
   |
   v
Final de la historia
   |
   v
Reto creativo
   |
   +----------+---------------+
   |                          |
Continuar                  Nueva historia
   |                          |
   v                          v
Nuevo dibujo              Nuevo dibujo
   |                          |
   +------------+-------------+
                |
                v
        Nueva aventura
```

---

## 27. Diseño funcional del sistema de historias

Cada historia debe tratarse como una entidad compuesta por:

- personaje protagonista;
- contexto del universo;
- objetivo narrativo;
- estructura de páginas;
- emociones objetivo;
- texto;
- ilustración por página;
- narración;
- decisiones, si existen;
- reto final;
- elementos nuevos incorporados al universo.

El sistema debe preservar la relación entre el cuento y el dibujo que lo originó.

---

## 28. Consistencia del personaje

La consistencia del protagonista es una prioridad técnica y de producto.

Para conseguirla, el sistema deberá conservar una representación estructurada del personaje que se utilice en futuras generaciones.

En el MVP, la representación debe contener como mínimo:

- descripción visual;
- colores;
- rasgos distintivos;
- personalidad;
- accesorios conocidos;
- referencia del dibujo original;
- referencia de la representación visual generada.

Cuando el niño continúe la historia, el sistema debe reutilizar esta representación en lugar de reinterpretar completamente al personaje desde cero.

---

## 29. Experiencia de generación

La espera durante la generación debe percibirse como parte de la magia.

El usuario no debe ver mensajes técnicos como "request", "token", "API" o "error 500".

Los estados visibles pueden comunicar acciones narrativas, por ejemplo:

- "Estamos descubriendo quién es tu personaje."
- "Estamos imaginando su aventura."
- "Estamos preparando las ilustraciones."
- "Tu cuento está cobrando vida."

Los mensajes deben representar estados reales del sistema y no afirmar acciones que no estén ocurriendo.

---

## 30. Diseño de navegación

La navegación principal del MVP debe limitarse a:

- **Inicio**
- **Nueva aventura**
- **Mi universo**
- **Biblioteca**
- **Perfil parental / configuración**

No se debe introducir navegación social ni feeds.

---

## 31. Monetización prevista

La monetización no debe perjudicar la experiencia infantil.

Modelo previsto para fases posteriores:

### Digital

- plan gratuito limitado;
- plan familiar de pago;
- más historias y funcionalidades premium.

### Físico

- PDF premium opcional;
- libro personalizado;
- colecciones físicas.

La experiencia infantil debe estar libre de publicidad.

---

## 32. Roadmap funcional

### MVP — Hackathon

- Cuenta parental.
- Perfil infantil.
- Dibujo físico → captura.
- Interpretación multimodal.
- Personaje principal.
- Historia adaptada por edad.
- Ilustraciones.
- Narración.
- Interactividad básica.
- Reto creativo.
- Persistencia del personaje e historias.
- Continuación de aventura.
- Biblioteca básica.
- Seguridad y moderación.
- PDF básico si el tiempo lo permite.

### V2

- PDF avanzado para colorear.
- Impresión bajo demanda.
- Mayor consistencia visual del universo.
- Accesorios y lugares reconocidos como elementos persistentes.
- Creativity Streak.
- Creativity DNA.
- Mejor personalización narrativa.
- Compartición social gestionada por padres.

### V3

- Vídeo/animación.
- Experiencias AR.
- Universos mucho más complejos.
- Nuevos formatos creativos: cómics, música, aventuras educativas, etc.
- Productos físicos y colecciones.

---

## 33. Definición de éxito del MVP

El MVP será considerado exitoso si permite a una familia experimentar de principio a fin la siguiente transformación:

**Un dibujo real hecho por un niño → un personaje reconocible → una historia personalizada → una experiencia interactiva cuando corresponda → un nuevo reto de dibujo.**

La demostración debe dejar clara una idea: la tecnología no es el destino de la experiencia. Es el mecanismo que devuelve al niño al mundo físico para seguir creando.

---

## 34. Mensaje de producto

La comunicación del producto debe combinar tres ideas:

**La IA que devuelve a los niños al papel.**

**Una plataforma que desarrolla la creatividad infantil.**

**Una nueva experiencia Phygital donde cada dibujo puede convertirse en una aventura y cada aventura puede inspirar un nuevo dibujo.**

### Concepto de campaña

> **Every story starts with a drawing. Every story ends with a new one.**

---

## 35. Decisiones de producto cerradas para el MVP

Las siguientes decisiones se consideran aprobadas y no deben reabrirse salvo que aparezca un bloqueo grave:

- El punto de entrada será un dibujo libre de un personaje.
- El sistema podrá adaptar la propuesta para que, según edad, también se incorporen accesorios o lugares.
- El usuario podrá crear una historia nueva o continuar una existente.
- Cada historia propondrá al menos un reto de nueva creación física.
- La pantalla no se utilizará como mecanismo de retención artificial; se incentivará volver al papel.
- El personaje principal tendrá memoria persistente y máxima consistencia posible dentro del MVP.
- La experiencia será un libro digital ilustrado y narrado, no un vídeo.
- Habrá interactividad narrativa adaptada por edad.
- La cuenta será gestionada por el adulto.
- No existirá una red social infantil.
- La seguridad será parte del pipeline de generación.
- La tecnología de IA se basará en el ecosistema Gemini definido para el proyecto.

---

## 36. Regla final de producto

Ante cualquier nueva funcionalidad, la pregunta principal será:

> **¿Esta funcionalidad ayuda al niño a imaginar, crear o volver al papel?**

Si no lo hace, debe considerarse fuera de WonderLoop hasta demostrar un valor excepcional para el producto.
