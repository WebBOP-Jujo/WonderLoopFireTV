package org.wonderloop.app.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.wonderloop.app.platform.PlatformStorage

object AppStrings {
    /**
     * Story generation language ("es" or "en").
     * Configurable in Parents Zone ("Idioma de los cuentos") and HeroConfirmScreen ("Idioma del cuento").
     * Does NOT change the app's UI/menu language, which always follows the device system language.
     */
    var language by mutableStateOf(loadInitialStoryLanguage())
        private set

    val isStoryEnglish: Boolean
        get() = language == "en"

    /**
     * App UI / Menu language always follows the device's system locale ("en" vs "es").
     */
    val uiLanguage: String
        get() = PlatformStorage.getDeviceLanguageCode()

    val isEnglish: Boolean
        get() = uiLanguage == "en"

    private fun loadInitialStoryLanguage(): String {
        val profileLang = org.wonderloop.app.data.ProfileRepository.activeProfile?.preferredLanguage
        if (profileLang == "es" || profileLang == "en") return profileLang
        return PlatformStorage.getDeviceLanguageCode()
    }

    fun init() {
        language = loadInitialStoryLanguage()
    }

    fun syncWithActiveProfile(lang: String?) {
        val clean = when (lang) {
            "en" -> "en"
            "es" -> "es"
            else -> PlatformStorage.getDeviceLanguageCode()
        }
        language = clean
    }

    fun setAppLanguage(lang: String) {
        val clean = if (lang == "en") "en" else "es"
        language = clean
        org.wonderloop.app.data.ProfileRepository.setPreferredLanguage(clean)
    }

    // -------------------------------------------------------------
    // General & Navigation
    // -------------------------------------------------------------
    val back get() = if (isEnglish) "◀ Back" else "◀ Volver"
    val cancel get() = if (isEnglish) "Cancel" else "Cancelar"
    val delete get() = if (isEnglish) "Delete" else "Eliminar"
    val close get() = if (isEnglish) "Close" else "Cerrar"
    val retry get() = if (isEnglish) "🔄 Retry" else "🔄 Reintentar"
    val continueText get() = if (isEnglish) "Continue" else "Continuar"
    val seeAll get() = if (isEnglish) "View all ➔" else "Ver todos ➔"
    val yearsOld get() = if (isEnglish) "yrs" else "años"

    // -------------------------------------------------------------
    // Exit App Confirmation Dialog
    // -------------------------------------------------------------
    val exitDialogTitle get() = if (isEnglish) "Do you want to exit?" else "¿Quieres salir?"
    val exitDialogMessage get() = if (isEnglish)
        "See you next time! Your created characters and stories will be safe on your bookshelf."
    else
        "¡Hasta la próxima! Tus personajes y cuentos creados estarán a salvo en tu estantería."
    val exitDialogStay get() = if (isEnglish) "Keep playing" else "Seguir jugando"
    val exitDialogConfirm get() = if (isEnglish) "Exit app" else "Salir de la app"

    // -------------------------------------------------------------
    // Home Screen
    // -------------------------------------------------------------
    val creator get() = if (isEnglish) "Creator" else "Creador"
    val createStoryTitle get() = if (isEnglish) "Create your story" else "Crea tu cuento"
    val createStorySubtitle get() = if (isEnglish) "Draw a character and snap a photo" else "Dibuja un personaje y hazle una foto"
    val stepDrawShort get() = if (isEnglish) "1 Draw" else "1 Dibuja"
    val stepPhotoShort get() = if (isEnglish) "2 Photo" else "2 Foto"
    val stepMagicShort get() = if (isEnglish) "3 Magic" else "3 Magia"
    val scanDrawingShort get() = if (isEnglish) "Scan drawing" else "Escanear dibujo"
    val todaySparkTitle get() = if (isEnglish) "Today's idea" else "Idea de hoy"
    val todaySparkIdeas: List<String>
        get() = if (isEnglish) listOf(
            "A puppy playing with a red ball",
            "A kitten wearing a funny hat",
            "A big smiling green dinosaur",
            "A little bunny eating a carrot",
            "A colorful little fish in the sea",
            "A little bear walking in the sun",
            "A turtle walking in the garden",
            "A little bird singing in a tree",
            "A lion with a big yellow mane",
            "A butterfly flying among the flowers",
            "An adventurous kid with a red cap",
            "A superhero with a blue cape"
        ) else listOf(
            "Un perrito jugando con una pelota roja",
            "Un gatito con un sombrero divertido",
            "Un dinosaurio verde muy sonriente",
            "Un conejito comiendo una zanahoria",
            "Un pececito de colores en el mar",
            "Un osito paseando bajo el sol",
            "Una tortuga caminando por el jardín",
            "Un pajarito cantando en un árbol",
            "Un león con una gran melena amarilla",
            "Una mariposa volando entre las flores",
            "Un niño aventurero con una gorra roja",
            "Un superhéroe con una capa azul"
        )
    val drawNowAction get() = if (isEnglish) "🖍️ Draw it!" else "🖍️ ¡Dibújalo!"
    val newCard get() = if (isEnglish) "New" else "Nuevo"
    val audioCreateStory get() = if (isEnglish)
        "Create your story! Draw a character on paper and take a photo."
    else
        "¡Crea tu cuento! Dibuja un personaje en un papel y hazle una foto."

    val myStories get() = if (isEnglish) "My Stories" else "Mis Cuentos"
    val myFriends get() = if (isEnglish) "My Friends" else "Mis Amigos"
    val continueReadingTitle get() = if (isEnglish) "Continue reading" else "Continuar leyendo"
    val continueReadingBadge get() = if (isEnglish) "📖 LAST READ" else "📖 ÚLTIMO LEÍDO"
    fun heroAndPagesSubtitle(heroName: String, pages: Int) = if (isEnglish) {
        "✨ $heroName · $pages pages"
    } else {
        "✨ $heroName · $pages págs"
    }
    val continueReadingCta get() = if (isEnglish) "Read ➔" else "Leer ➔"
    val appVersionInfo get() = if (isEnglish) "Version 1.0.0" else "Versión 1.0.0"
    val appDescriptionInfo get() = if (isEnglish)
        "WonderLoop connects traditional children's paper drawing with generative AI, encouraging off-screen creativity through The Creativity Loop."
    else
        "WonderLoop une el dibujo en papel tradicional de los niños con la inteligencia artificial generativa, fomentando la creatividad fuera de pantalla con The Creativity Loop."
    val deleteStoryQuestion get() = if (isEnglish) "Delete Story?" else "¿Eliminar cuento?"
    fun deleteStoryWarning(title: String) = if (isEnglish)
        "Are you sure you want to delete «$title» from your bookshelf? This action cannot be undone."
    else
        "¿Seguro que quieres eliminar «$title» de tu estantería? Esta acción no se puede deshacer."
    val deleteSagaQuestion get() = if (isEnglish) "Delete entire saga?" else "¿Eliminar saga completa?"
    fun deleteSagaWarning(title: String, count: Int) = if (isEnglish)
        "Are you sure you want to delete the entire saga «$title» with all its $count chapters? This cannot be undone."
    else
        "¿Seguro que quieres eliminar toda la saga «$title» con sus $count capítulos? Esta acción no se puede deshacer."
    val confirmDelete get() = if (isEnglish) "Yes, delete" else "Sí, eliminar"

    // -------------------------------------------------------------
    // MyStoriesScreen (Bookshelf)
    // -------------------------------------------------------------
    val myBookshelf get() = if (isEnglish) "📚 My Bookshelf" else "📚 Mi Estantería"
    fun booksCount(count: Int) = if (count == 1) {
        if (isEnglish) "1 book" else "1 libro"
    } else {
        if (isEnglish) "$count books" else "$count libros"
    }
    val bookshelfSubtitle get() = if (isEnglish)
        "Choose a book with the remote and press OK to read it · Grown-ups: ☰ Menu to delete"
    else
        "Elige un libro con el mando y pulsa OK para leerlo · Adultos: ☰ Menú para borrarlo"
    val emptyBookshelfTitle get() = if (isEnglish) "Your bookshelf is waiting" else "Tu estantería está esperando"
    val emptyBookshelfDesc get() = if (isEnglish)
        "Draw your hero on paper, snap a photo, and their illustrated book will appear here on the shelf!"
    else
        "¡Dibuja a tu héroe en un papel, hazle una foto y su libro ilustrado aparecerá aquí colocado en la balda!"
    val createFirstBook get() = if (isEnglish) "📸 Create my first book" else "📸 Crear mi primer libro"
    fun pagesCountRibbon(count: Int) = if (isEnglish) "$count pgs" else "$count págs"
    val newStoryBookstand get() = if (isEnglish) "New Story" else "Nuevo Cuento"

    // -------------------------------------------------------------
    // MyCharactersScreen (Pinboard)
    // -------------------------------------------------------------
    val friendsBoard get() = if (isEnglish) "📌 My Friends" else "📌 Mis Amigos"
    fun friendsCount(count: Int) = if (count == 1) {
        if (isEnglish) "1 friend" else "1 amigo"
    } else {
        if (isEnglish) "$count friends" else "$count amigos"
    }
    val boardSubtitle get() = if (isEnglish)
        "Choose a friend with the remote and press OK to read their story:"
    else
        "Elige un amigo con el mando y pulsa OK para leer su cuento:"
    val emptyBoardTitle get() = if (isEnglish) "Your art board is open" else "Tu tablón de arte está libre"
    val emptyBoardDesc get() = if (isEnglish)
        "Draw your hero on paper, snap a photo, and we'll pin their magical artwork here!"
    else
        "¡Dibuja a tu héroe en papel, hazle una foto y colgaremos su lámina mágica aquí con una chincheta!"
    val pinFirstDrawing get() = if (isEnglish) "📸 Pin my first drawing" else "📸 Colgar mi primer dibujo"
    val paperDrawingBadge get() = if (isEnglish) "🖍️ Paper drawing" else "🖍️ Dibujo papel"
    val aiVersionBadge get() = if (isEnglish) "✨ AI Version" else "✨ Versión IA"
    val magicalFriend get() = if (isEnglish) "✨ Magical friend" else "✨ Amigo mágico"
    fun readStoryButton(count: Int) = if (count > 1) {
        if (isEnglish) "📖 $count stories" else "📖 $count cuentos"
    } else {
        if (isEnglish) "📖 Read" else "📖 Leer"
    }

    // -------------------------------------------------------------
    // ProfileScreen & ProfilePickerScreen
    // -------------------------------------------------------------
    val editProfile get() = if (isEnglish) "Edit Profile" else "Editar Perfil"
    fun ageYears(age: Int) = if (isEnglish) "🎂 $age yrs" else "🎂 $age años"
    fun storiesCreated(count: Int) = if (count == 1) {
        if (isEnglish) "Story created" else "Cuento creado"
    } else {
        if (isEnglish) "Stories created" else "Cuentos creados"
    }
    fun aliveDrawings(count: Int) = if (count == 1) {
        if (isEnglish) "Drawing brought alive" else "Dibujo con vida"
    } else {
        if (isEnglish) "Drawings brought alive" else "Dibujos con vida"
    }
    val levelTitle get() = if (isEnglish) "Level" else "Nivel"
    val levelCurious get() = if (isEnglish) "Curious" else "Curioso"
    val languageSubtitle get() = if (isEnglish) "Learn English while playing!" else "¡Aprende inglés mientras juegas!"
    val spanish get() = if (isEnglish) "🇪🇸 Spanish" else "🇪🇸 Español"
    val english get() = if (isEnglish) "🇬🇧 English" else "🇬🇧 Inglés"
    val privacyBadge get() = if (isEnglish)
        "100% Private & Safe: Profile and drawings are stored exclusively on this device."
    else
        "100% Privado y Seguro: El perfil y dibujos se almacenan exclusivamente en este dispositivo."

    val whoIsImaginingClean get() = if (isEnglish) "Who is imagining today?" else "¿Quién imagina hoy?"
    val tapYourCharacter get() = if (isEnglish) "Choose your character with the remote" else "Elige tu personaje con el mando"
    val addProfileButton get() = if (isEnglish) "Add" else "Añadir"
    val newProfileSubtitle get() = if (isEnglish) "New profile" else "Nuevo perfil"
    val parentsZoneLink get() = if (isEnglish) "Parents Zone" else "Zona de padres"
    val newProfileTitle get() = if (isEnglish) "New profile" else "Nuevo perfil"
    val chooseYourAvatarTitle get() = if (isEnglish) "Choose your avatar" else "Elige tu avatar"
    val whatIsYourNameTitle get() = if (isEnglish) "What's your name?" else "¿Cómo te llamas?"
    val childNameMockupPlaceholder get() = if (isEnglish) "E.g. Lucas, Valentina..." else "Ej. Lucas, Valentina..."
    val howOldAreYouTitle get() = if (isEnglish) "How old are you?" else "¿Cuántos años tienes?"
    val allSetCta get() = if (isEnglish) "All Set!" else "¡Listo!"

    // Child Profile Screen ("Mi Perfil")
    val myProfileTitle get() = if (isEnglish) "My Profile" else "Mi Perfil"
    val tapAvatarToCustomize get() = if (isEnglish) "Select your avatar to customize it" else "Elige tu avatar para personalizarlo"
    val chooseYourNewAvatar get() = if (isEnglish) "Choose your avatar" else "Elige tu avatar"
    val myMedalsTitle get() = if (isEnglish) "My Magical Medals" else "Mis Medallas Mágicas"
    val medalFirstStoryTitle get() = if (isEnglish) "First Spark" else "Primer Trazo"
    val medalFirstStoryDesc get() = if (isEnglish) "Create your first story" else "Crea tu primer cuento"
    val medalThreeFriendsTitle get() = if (isEnglish) "Great Friend" else "Gran Amigo"
    val medalThreeFriendsDesc get() = if (isEnglish) "Bring 3 drawings to life" else "Da vida a 3 personajes"
    val medalSagaTitle get() = if (isEnglish) "To Be Continued" else "Continuará..."
    val medalSagaDesc get() = if (isEnglish) "Continue a story saga" else "Continúa un capítulo de saga"
    val medalMasterTitle get() = if (isEnglish) "Super Author" else "Súper Autor"
    val medalMasterDesc get() = if (isEnglish) "Create 5 magical stories" else "Crea 5 cuentos mágicos"

    // Parents Zone Screen ("Zona de padres" - Mockup 11)
    val parentsZoneTitle get() = if (isEnglish) "Parents Zone" else "Zona de padres"
    val planProActiveLabel get() = if (isEnglish) "Pro Plan · active" else "Plan Pro · activo"
    val planFreeActiveLabel get() = if (isEnglish) "Free Plan · active" else "Plan Gratuito · activo"
    val manageButtonLabel get() = if (isEnglish) "Manage" else "Gestionar"
    val profilesSectionHeader get() = if (isEnglish) "PROFILES" else "PERFILES"
    val addProfileRowLabel get() = if (isEnglish) "+ Add profile" else "+ Añadir perfil"
    val settingsSectionHeader get() = if (isEnglish) "SETTINGS" else "AJUSTES"
    val storyLanguageRowLabel get() = if (isEnglish) "Story language" else "Idioma de los cuentos"
    val readingVoiceRowLabel get() = if (isEnglish) "Reading voice" else "Voz de lectura"
    val voiceSoftLabel get() = if (isEnglish) "Soft" else "Suave"
    val voiceNormalLabel get() = if (isEnglish) "Normal" else "Normal"
    val voiceLivelyLabel get() = if (isEnglish) "Lively" else "Alegre"
    val soundEffectsRowLabel get() = if (isEnglish) "Sound effects" else "Efectos de sonido"
    val sfxActiveLabel get() = if (isEnglish) "On" else "Activados"
    val sfxMutedLabel get() = if (isEnglish) "Muted" else "Silenciados"
    val replayTutorialRowLabel get() = if (isEnglish) "Welcome tutorial" else "Tutorial de bienvenida"
    val privacyAndDataRowLabel get() = if (isEnglish) "Privacy & data" else "Privacidad y datos"
    val helpAndAboutRowLabel get() = if (isEnglish) "Help & about" else "Ayuda y acerca de"

    fun bookshelfOf(name: String) = if (isEnglish) "📚 $name's Bookshelf" else "📚 Estantería de $name"
    fun friendsOf(name: String) = if (isEnglish) "📌 $name's Friends" else "📌 Amigos de $name"
    fun deleteProfileQuestion(name: String) = if (isEnglish) "Delete $name's profile?" else "¿Eliminar el perfil de $name?"
    fun deleteProfileWarning(name: String) = if (isEnglish)
        "All stories created by $name will also be removed. This cannot be undone."
    else
        "Se eliminarán todos los cuentos creados por $name. Esta acción no se puede deshacer."
    val gateForAuthorSettings get() = if (isEnglish)
        "Parental access required to add or modify child profiles."
    else
        "Se requiere acceso de un adulto para gestionar o modificar perfiles de niños."

    // -------------------------------------------------------------
    // CameraScanScreen
    // -------------------------------------------------------------
    val scanSubtitle get() = if (isEnglish)
        "Place the sheet of paper on a well-lit table"
    else
        "Coloca el folio de papel sobre una mesa bien iluminada"
    val frameYourDrawing get() = if (isEnglish) "Frame your drawing here" else "Encuadra aquí tu dibujo"
    val frameDrawingHint get() = if (isEnglish)
        "Make sure the whole character and its colors are clearly visible"
    else
        "Intenta que se vea todo el personaje y sus colores claramente"
    val cameraPermissionDialogTitle get() = if (isEnglish) "Camera Permission" else "Permiso de Cámara"
    val cameraPermissionDialogDesc get() = if (isEnglish)
        "To let WonderLoop bring your paper drawings to life, camera access is needed. You can turn it on in Settings."
    else
        "Para que WonderLoop pueda dar vida a tus dibujos en papel, se necesita acceso a la cámara. Puedes activarlo con un toque en Ajustes."
    val openSettings get() = if (isEnglish) "⚙️ Open Settings" else "⚙️ Abrir Ajustes"
    val cameraPermissionRequired get() = if (isEnglish) "📷 Camera permission needed" else "📷 Permiso de cámara necesario"
    val cameraPermissionNotice get() = if (isEnglish)
        "WonderLoop needs the camera to photograph your drawing. Tap «Ask Permission» or turn it on in Settings."
    else
        "WonderLoop necesita la cámara para fotografiar tu dibujo. Pulsa «Pedir Permiso» o activa el permiso en Ajustes."
    val settingsButton get() = if (isEnglish) "⚙️ Settings" else "⚙️ Ajustes"
    val askPermissionButton get() = if (isEnglish) "🔄 Ask Permission" else "🔄 Pedir Permiso"
    val chooseFromGallery get() = if (isEnglish) "🖼️ Choose from Gallery" else "🖼️ Elegir foto de la Galería"
    val bringToLifeButton get() = if (isEnglish) "✨ Bring this Character to Life! ✨" else "✨ ¡Dar Vida a este Personaje! ✨"
    val takeAnotherPhoto get() = if (isEnglish) "🔄 Take another photo" else "🔄 Tomar otra foto"

    // -------------------------------------------------------------
    // HeroConfirmScreen
    // -------------------------------------------------------------
    val heroBornTitle get() = if (isEnglish) "Your hero has come to life!" else "¡Tu protagonista ha nacido!"
    fun heroBornSubtitle(childName: String) = if (isEnglish)
        "Ready for a great adventure with $childName"
    else
        "Listo para vivir una gran historia con $childName"
    val yourHero get() = if (isEnglish) "⭐ Your Hero" else "⭐ Tu Personaje"
    val defaultHeroSpeech get() = if (isEnglish)
        "«Hello! I cannot wait to start this magical story with you.»"
    else
        "«¡Hola! Estoy deseando empezar este mágico cuento contigo.»"
    val storyLanguageLabel get() = if (isEnglish) "🌐 Story Language:" else "🌐 Idioma del Cuento:"
    val inSpanish get() = "🇪🇸 En Español"
    val inEnglish get() = "🇬🇧 In English ✨"
    val startStoryButton get() = if (isEnglish) "📖 Start Magical Story! ➔" else "📖 ¡Comenzar el Cuento! ➔"
    val scanAnotherCharacter get() = if (isEnglish) "🔄 Draw or scan another character" else "🔄 Dibujar o escanear otro personaje"
    fun heroConfirmContinuationTitle(prevHero: String) = if (isEnglish) "Joining $prevHero!" else "¡Se une a $prevHero!"
    fun heroConfirmContinuationSubtitle(prevHero: String, childName: String) = if (isEnglish)
        "A new companion has arrived to continue the saga with $prevHero and $childName!"
    else
        "¡Un nuevo compañero para continuar la saga con $prevHero y $childName!"
    fun sagaBadge(chapter: Int) = if (isEnglish) "SAGA · Chapter $chapter" else "SAGA · Capítulo $chapter"
    fun continueSagaButton(chapter: Int) = if (isEnglish) "📖 Continue Adventure (Ch. $chapter) ➔" else "📖 ¡Continuar Aventura (Cap. $chapter)! ➔"

    // Blank / Unclear Drawing Retry State
    val retryScanBadge get() = if (isEnglish) "🔍 ALMOST THERE! · NO DRAWING DETECTED" else "🔍 ¡CASI LO TENEMOS! · DIBUJO NO DETECTADO"
    val retryScanTitle get() = if (isEnglish) "Help us find your drawing!" else "¿Nos ayudas a ver tu dibujo?"
    val retryScanSubtitle get() = if (isEnglish)
        "We could not spot clear drawing lines in the photo. Try these magical tricks:"
    else
        "No hemos podido ver los trazos del dibujo en la foto. ¡Prueba estos trucos mágicos!"
    val retryTip1Title get() = if (isEnglish) "Good Light & Strong Colors" else "Buena Luz y Colores Vivos"
    val retryTip1Desc get() = if (isEnglish)
        "Draw with vibrant markers or crayons on white paper with good room light."
    else
        "Dibuja con rotuladores oscuros o colores vivos sobre papel blanco bien iluminado."
    val retryTip2Title get() = if (isEnglish) "Center Drawing & Hold Steady" else "Centra la Hoja y Móvil Firme"
    val retryTip2Desc get() = if (isEnglish)
        "Bring your phone camera closer so the drawing fills the frame and hold it steady."
    else
        "Acerca la cámara para que el dibujo ocupe la pantalla y mantenla quieta un segundo."
    val retryScanButton get() = if (isEnglish) "📸 Scan Again with Phone" else "📸 Volver a escanear con el móvil"
    val retryMobileNoticeTitle get() = if (isEnglish) "Take another photo on your phone" else "Toma otra foto en tu móvil"
    val retryMobileNoticeSubtitle get() = if (isEnglish)
        "Focus on your paper drawing with good light. The TV will update automatically!"
    else
        "Enfoca bien tu dibujo en el papel. ¡La tele cambiará sola al recibirla!"
    val retryDemoHint get() = if (isEnglish)
        "💡 In the scan screen you can also try sample drawings if you have no paper!"
    else
        "💡 En la siguiente pantalla también puedes elegir un dibujo de ejemplo si no tienes papel."

    // -------------------------------------------------------------
    // MagicLoadingScreen
    // -------------------------------------------------------------
    val wakingUpDrawing get() = if (isEnglish) "✨ Waking up your drawing" else "✨ Despertando tu dibujo"
    val bringingCreationToLife get() = if (isEnglish) "Bringing your creation to life" else "Dando vida a tu creación"

    // -------------------------------------------------------------
    // StoryGeneratingScreen
    // -------------------------------------------------------------
    val creatingDigitalBook get() = if (isEnglish) "Creating your Digital Book" else "Creando tu Libro Digital"
    val cancelStoryQuestion get() = if (isEnglish) "Cancel story?" else "¿Cancelar cuento?"
    fun cancelStoryWarning(characterName: String) = if (isEnglish)
        "We are creating the illustrations and magical voice for $characterName. If you leave now, the adventure will stop."
    else
        "Estamos creando las ilustraciones y la voz mágica de $characterName. Si sales ahora, se detendrá la aventura."
    val keepWaiting get() = if (isEnglish) "Keep waiting" else "Seguir esperando"
    val exitToHome get() = if (isEnglish) "Exit to home" else "Salir al inicio"
    val magicPause get() = if (isEnglish) "Magic Pause" else "Pausa Mágica"
    val backToHome get() = if (isEnglish) "Back to Home" else "Volver al Inicio"
    fun writingChapter(chapter: Int, hero1: String, hero2: String) = if (isEnglish)
        "📖 Writing Chapter $chapter with $hero1 & $hero2..."
    else
        "📖 Escribiendo el Capítulo $chapter con $hero1 y $hero2..."
    val paintingBothHeroes get() = if (isEnglish)
        "🎨 Painting both companions together in watercolor..."
    else
        "🎨 Pintando a ambos compañeros juntos en acuarela..."
    val resolvingChallenge get() = if (isEnglish)
        "✨ Weaving your new drawing into the challenge..."
    else
        "✨ Incorporando tu nuevo dibujo para superar el reto..."

    // -------------------------------------------------------------
    // StoryReaderScreen
    // -------------------------------------------------------------
    fun pageOf(current: Int, total: Int) = if (isEnglish) "Page $current of $total" else "Página $current de $total"
    val creativeChallengeNav get() = if (isEnglish) "🌟 Creative Challenge" else "🌟 Reto Creativo"
    val exitReaderDialogTitle get() = if (isEnglish) "Close story?" else "¿Cerrar el cuento?"
    val exitReaderDialogConfirm get() = if (isEnglish) "Close story" else "Cerrar cuento"
    fun storySavedNotice(title: String) = if (isEnglish)
        "Your story «$title» is safely saved in your collection 📚."
    else
        "Tu cuento «$title» está guardado en tu colección 📚."
    val goToMyBookshelf get() = if (isEnglish) "📚 Go to my bookshelf" else "📚 Ir a mi estantería"
    val keepReading get() = if (isEnglish) "Keep reading" else "Seguir leyendo"
    val listenWithVoice get() = if (isEnglish) "Listen with voice" else "Escuchar con voz"
    val listenPageWithVoice get() = if (isEnglish) "Listen to page" else "Escuchar página con voz"
    val pauseNarration get() = if (isEnglish) "Pause narration" else "Pausar narración"
    val challengeAlreadyCompletedTitle get() = if (isEnglish) "🎉 Challenge Completed!" else "🎉 ¡Reto Superado!"
    fun challengeAlreadyCompletedBody(companion: String) = if (isEnglish)
        "You drew $companion and continued the great adventure!"
    else
        "Dibujaste a $companion y continuaste la gran aventura."
    fun sagaChaptersBadge(count: Int) = if (isEnglish) "$count Chapters" else "$count Capítulos"

    // -------------------------------------------------------------
    // OnboardingScreen (5-Step Welcome Tutorial)
    // -------------------------------------------------------------
    val skip get() = if (isEnglish) "Skip" else "Saltar"
    val next get() = if (isEnglish) "Next" else "Siguiente"
    val startMyAdventure get() = if (isEnglish) "🚀 Start my adventure!" else "🚀 ¡Comenzar mi aventura!"
    val splashSubtitle get() = if (isEnglish) "MAGICAL STORIES" else "CUENTOS MÁGICOS"

    val onboardingStep1Badge get() = if (isEnglish) "🎨 STEP 1 OF 5 · DRAW ON PAPER" else "🎨 PASO 1 DE 5 · DIBUJA EN PAPEL"
    val onboardingStep1Title get() = if (isEnglish) "Magic starts on a real sheet of paper" else "La magia empieza en un folio de verdad"
    val onboardingStep1Desc get() = if (isEnglish)
        "Grab your favorite crayons or markers and draw any character you imagine: a playful dragon, a space rocket, or your own hero."
    else
        "Coge tus ceras o rotuladores favoritos y dibuja cualquier personaje que imagines: un dragón travieso, un cohete espacial o tu propio héroe."

    val onboardingStep2Badge get() = if (isEnglish) "📸 STEP 2 OF 5 · PHYGITAL MAGIC" else "📸 PASO 2 DE 5 · MAGIA PHYGITAL"
    val onboardingStep2Title get() = if (isEnglish) "Snap a photo and bring it to life" else "Haz una foto y dale vida"
    val onboardingStep2Desc get() = if (isEnglish)
        "Take a photo of your drawing with an adult's help. Our AI instantly discovers its colors, smile, and personality to start your story."
    else
        "Fotografía tu dibujo con ayuda de un adulto. Nuestra IA descubre sus colores, su sonrisa y su personalidad al instante para empezar tu cuento."

    val onboardingStep3Badge get() = if (isEnglish) "📖 STEP 3 OF 5 · YOUR STORY" else "📖 PASO 3 DE 5 · TU CUENTO"
    val onboardingStep3Title get() = if (isEnglish) "Star in your own book" else "Protagoniza tu propio libro"
    val onboardingStep3Desc get() = if (isEnglish)
        "Your drawing becomes the hero of a 5-page illustrated story, with lovely watercolors or 3D animation and a voice that reads it to you."
    else
        "Tu dibujo se convierte en el héroe de un cuento ilustrado de 5 páginas, con preciosas acuarelas o animación 3D y una voz que te lo cuenta."

    val onboardingStep4Badge get() = if (isEnglish) "🌍 STEP 4 OF 5 · LEARN BY PLAYING" else "🌍 PASO 4 DE 5 · APRENDE JUGANDO"
    val onboardingStep4Title get() = if (isEnglish) "One story, two languages" else "Un cuento, dos idiomas"
    val onboardingStep4Desc get() = if (isEnglish)
        "Choose to read and listen in Spanish or English. Effortlessly learn new words while your imagination grows in both languages."
    else
        "Elige leerlo y escucharlo en español o en inglés. Sin darte cuenta, aprenderás palabras nuevas mientras tu imaginación crece en los dos idiomas."

    val onboardingStep5Badge get() = if (isEnglish) "🌱 STEP 5 OF 5 · THE CREATIVITY LOOP" else "🌱 PASO 5 DE 5 · THE CREATIVITY LOOP"
    val onboardingStep5Title get() = if (isEnglish) "Turn off the screen, keep creating" else "Apaga la pantalla, sigue creando"
    val onboardingStep5Desc get() = if (isEnglish)
        "Every story ends with an off-screen drawing mission. We care for your time: fewer minutes watching, more hands creating."
    else
        "Cada cuento termina con una misión para dibujar sin pantalla. Cuidamos tu tiempo: menos minutos mirando, más manos creando."

    // -------------------------------------------------------------
    // PaywallScreen
    // -------------------------------------------------------------
    val paywallBrandBadge get() = "✨ WonderLoop Pro"
    val paywallTitle get() = if (isEnglish) "Unlock the magical universe" else "Desbloquea el universo mágico"
    val paywallSubtitle get() = if (isEnglish)
        "Personalized stories and magical voices for the whole family"
    else
        "Cuentos personalizados y voces mágicas para toda la familia"

    // 6 Real Compact Grid Benefits
    val gridBenefitStories get() = if (isEnglish) "Up to 15/mo" else "Hasta 15/mes"
    val gridBenefitWatercolors get() = if (isEnglish) "HD Watercolors" else "Acuarelas HD"
    val gridBenefitNarration get() = if (isEnglish) "Narration" else "Narración"
    val gridBenefitRollover get() = if (isEnglish) "Rollover" else "Acumulables"
    val gridBenefitProfiles get() = if (isEnglish) "3 profiles" else "3 perfiles"
    val gridBenefitNoAds get() = if (isEnglish) "Ad-free" else "Sin anuncios"

    val paywallToggleAnnual get() = if (isEnglish) "Annual · save 27%" else "Anual · ahorra 27%"
    val paywallToggleMonthly get() = if (isEnglish) "Monthly" else "Mensual"
    val paywallMostPopularRibbon get() = if (isEnglish) "MOST POPULAR" else "MÁS POPULAR"

    val planProTitle get() = if (isEnglish) "Magic Creator Plan" else "Plan Creador Mágico"
    val planProSub get() = if (isEnglish) "15 stories/month · rollover up to 30" else "15 cuentos/mes · acumula hasta 30"

    val planBasicTitle get() = if (isEnglish) "Storyteller Plan" else "Plan Cuentacuentos"
    val planBasicSub get() = if (isEnglish) "6 stories/month · rollover up to 12" else "6 cuentos/mes · acumula hasta 12"

    val planPackTitle get() = if (isEnglish) "5 Stories Pack" else "Pack 5 cuentos"
    val planPackPriceMember get() = if (isEnglish) "$2.99" else "2,99€"
    val planPackPriceRegular get() = if (isEnglish) "$4.49" else "4,49€"
    val planPackSub get() = if (isEnglish) "5 standalone stories · never expire" else "5 cuentos sueltos · sin caducidad"
    val planPackBadgeMember get() = if (isEnglish) "Pro Price (-33%)" else "Precio Pro (-33%)"
    val planPackBadgeRegular get() = if (isEnglish) "No subscription" else "Sin suscripción"

    val sectionPacks get() = if (isEnglish) "NO SUBSCRIPTION · ONE-TIME" else "SIN SUSCRIPCIÓN · PAGO ÚNICO"

    val ctaAnnual get() = if (isEnglish) "🚀 Start with Pro — $5.83/mo" else "🚀 Empezar con Pro — 5,83€/mes"
    val ctaBasicAnnual get() = if (isEnglish) "🚀 Start with Pro — $3.33/mo" else "🚀 Empezar con Pro — 3,33€/mes"
    val ctaPro get() = if (isEnglish) "🚀 Start with Pro — $7.99/mo" else "🚀 Empezar con Pro — 7,99€/mes"
    val ctaBasic get() = if (isEnglish) "🚀 Start with Pro — $3.99/mo" else "🚀 Empezar con Pro — 3,99€/mes"
    val ctaPackMember get() = if (isEnglish) "🎁 Buy 5 stories — $2.99" else "🎁 Comprar 5 cuentos — 2,99€"
    val ctaPackRegular get() = if (isEnglish) "🎁 Buy 5 stories — $4.49" else "🎁 Comprar 5 cuentos — 4,49€"

    val paywallAnnualEquivalent get() = if (isEnglish) "Billed $69.99/year" else "Facturado 69,99€/año"
    val paywallBasicAnnualEquivalent get() = if (isEnglish) "Billed $39.99/year" else "Facturado 39,99€/año"
    val paywallCancelGuarantee get() = if (isEnglish) "Cancel anytime via Amazon" else "Cancela cuando quieras en Amazon"
    val paywallOneTimeGuarantee get() = if (isEnglish) "One-time payment · Stories never expire" else "Pago único · Tus cuentos nunca caducan"
    val paywallPackMemberSavings get() = if (isEnglish) "Pro discount applied (-33%)" else "Descuento Pro aplicado (-33%)"
    val paywallPackRegularHint get() = if (isEnglish) "With Pro: $2.99 (-33%)" else "Con Pro: 2,99€ (-33%)"

    // Quota and Rollover Counters
    fun storiesAvailable(count: Int) = if (isEnglish) {
        if (count == 1) "1 story available" else "$count stories available"
    } else {
        if (count == 1) "1 cuento disponible" else "$count cuentos disponibles"
    }

    val quotaExhaustedPrompt get() = if (isEnglish)
        "You have used all your available stories! To continue creating adventures, choose a Pro plan or a 5-story pack."
    else
        "¡Has completado tus cuentos disponibles! Para seguir dando vida a tus dibujos, elige un plan Pro o un pack de 5 cuentos."

    val packAddedSuccess get() = if (isEnglish) "5 magical stories added to your balance!" else "¡5 cuentos mágicos añadidos a tu saldo!"
    val planActivatedSuccess get() = if (isEnglish) "Pro Plan activated! Stories added to your balance." else "¡Plan Pro activado! Cuentos añadidos a tu saldo."

    val restorePurchases get() = if (isEnglish) "Restore purchases" else "Restaurar compras"
    val redeemCode get() = if (isEnglish) "🎟️ Redeem code" else "🎟️ Canjear código"

    val vipActivated get() = if (isEnglish) "Pro Plan activated with your coupon!" else "¡Plan Pro activado con tu cupón!"
    val restoreSuccess get() = if (isEnglish) "Purchases restored successfully!" else "¡Compras restauradas con éxito!"
    val restoreNotFound get() = if (isEnglish) "No active previous purchases found." else "No se encontraron compras activas previas."

    // -------------------------------------------------------------
    // ParentalGateDialog
    // -------------------------------------------------------------
    val parentalGateTitle get() = if (isEnglish) "Parental control" else "Control parental"
    val parentalGatePrompt get() = if (isEnglish)
        "To protect little ones, an adult must answer this question:"
    else
        "Para proteger a los pequeños, un adulto debe responder a esta pregunta:"
    fun mathQuestion(n1: Int, n2: Int) = if (isEnglish) "What is $n1 × $n2?" else "¿Cuánto es $n1 × $n2?"
    val writeResult get() = if (isEnglish) "Enter the answer" else "Escribe el resultado"
    val wrongAnswer get() = if (isEnglish) "Incorrect answer. Try the new question." else "Respuesta incorrecta. Prueba con la nueva pregunta."

    // -------------------------------------------------------------
    // RedeemCouponDialog
    // -------------------------------------------------------------
    val redeemDialogTitle get() = if (isEnglish) "Redeem Promo Code" else "Canjear Código Promocional"
    val redeemDialogPrompt get() = if (isEnglish)
        "If you have a sponsor code or coupon, enter it here:"
    else
        "Si dispones de un código o cupón de patrocinador, introdúcelo aquí:"
    val couponValidationFailed get() = if (isEnglish) "Could not validate coupon." else "No se pudo validar el cupón."
    val redeemButton get() = if (isEnglish) "Redeem" else "Canjear"
}
