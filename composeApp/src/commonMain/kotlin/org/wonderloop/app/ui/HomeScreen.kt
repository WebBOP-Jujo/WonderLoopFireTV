package org.wonderloop.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wonderloop.app.data.CharacterProfile
import org.wonderloop.app.data.FullStoryResponse
import org.wonderloop.app.data.SagaBook
import org.wonderloop.app.data.StoryRepository
import org.wonderloop.app.platform.PlatformStorage
import org.wonderloop.app.platform.WonderLoopImage
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.tv.TvClickableSurface
import org.wonderloop.app.ui.components.WonderBackdrop
import org.wonderloop.app.ui.components.WonderLoopMark
import org.wonderloop.app.ui.components.MinimalistLockIcon

private val HomeCream = Color(0xFFFFFBF5)
private val HomeIvoryGlow = Color(0xFFFFF6E3)
private val HomeInk = Color(0xFF2B2340)
private val HomeInkMuted = Color(0xFF6B6480)
private val HomeCoral = Color(0xFFD9384A)
private val HomeCoralEnd = Color(0xFFE5566A)
private val HomeCoralSoft = Color(0xFFFCEAE7)
private val HomeTeal = Color(0xFF0E7F64)
private val HomeTealSoft = Color(0xFFDDF5EE)
private val HomeGold = Color(0xFFFFD166)
private val HomeCardBorder = Color(0xFFEDE3D5)

/** Remembers the last element the user opened so BACK returns focus to it (Fire TV focus restoration). */
private object HomeFocusMemory {
    var lastKey: String? = null
    var lastIdea: DailyIdea? = null
}

private data class DailyIdea(val emoji: String, val es: String, val en: String)

/** Concrete, drawable ideas; one is picked at random on each visit (paper note on the "create" card). Kept short to fit the note. */
private val DAILY_IDEAS = listOf(
    DailyIdea("🦁", "Un león con gran melena", "A lion with a big mane"),
    DailyIdea("🏴‍☠️", "Un barco pirata con bandera", "A pirate ship with a flag"),
    DailyIdea("🤖", "Un robot con una antena", "A robot with an antenna"),
    DailyIdea("🐢", "Una tortuga de colores", "A colorful turtle"),
    DailyIdea("🐉", "Un dragón con alas verdes", "A dragon with green wings"),
    DailyIdea("🚀", "Un cohete que va a la Luna", "A rocket flying to the Moon"),
    DailyIdea("🐱", "Un gato con botas rojas", "A cat wearing red boots"),
    DailyIdea("🐙", "Un pulpo con ocho brazos", "An octopus with eight arms"),
    DailyIdea("🦄", "Un unicornio de arcoíris", "A rainbow unicorn"),
    DailyIdea("🐘", "Un elefante con un sombrero", "An elephant wearing a hat"),
    DailyIdea("🦖", "Un dinosaurio con manchas", "A dinosaur with spots"),
    DailyIdea("🐝", "Una abeja con gafas", "A bee wearing glasses"),
    DailyIdea("🦉", "Un búho con ojos grandes", "An owl with big eyes"),
    DailyIdea("🐧", "Un pingüino con bufanda", "A penguin with a scarf"),
    DailyIdea("🐸", "Una rana con una corona", "A frog wearing a crown"),
    DailyIdea("🦊", "Un zorro con una mochila", "A fox with a backpack")
)

@Composable
fun HomeScreen(
    childName: String,
    focusRestoreTick: Int = 0,
    childAvatar: String = "🧒",
    backendOnline: Boolean?,
    onNewAdventureClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSwitchProfileClick: () -> Unit = {},
    onNavigateToStories: () -> Unit = {},
    onNavigateToCharacters: () -> Unit = {},
    onParentsClick: () -> Unit = onEditProfileClick,
    onRetryConnection: () -> Unit = {},
    onOpenStory: (FullStoryResponse) -> Unit = {}
) {
    val en = AppStrings.isEnglish
    val profileStories = StoryRepository.getStoriesForProfile()
    val lastReadStory = StoryRepository.getLastReadStoryForProfile()
    val sagas = remember(profileStories.size) { StoryRepository.getBookshelfSagas() }
    val displayStory = lastReadStory ?: profileStories.firstOrNull()

    val friends = remember(profileStories) {
        buildList {
            profileStories.forEach { story ->
                add(story.character)
                story.continuation?.effectiveHero?.let { add(it) }
            }
        }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }
    }

    // One FocusRequester per focusable element, keyed so focus can be restored after BACK
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requester(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }
    fun remembered(key: String, action: () -> Unit): () -> Unit = {
        HomeFocusMemory.lastKey = key
        action()
    }

    // Restore focus on entry and whenever a dialog on top (e.g. the parental gate) closes
    LaunchedEffect(focusRestoreTick) {
        delay(if (focusRestoreTick == 0) 150 else 250)
        val restored = HomeFocusMemory.lastKey?.let { key ->
            requesters.containsKey(key) && runCatching { requesters[key]?.requestFocus() }.isSuccess
        } ?: false
        if (!restored) runCatching { requester("create").requestFocus() }
    }

    // A fresh idea each time the child lands on Home, never the same one twice in a row
    val idea = remember {
        DAILY_IDEAS.filter { it != HomeFocusMemory.lastIdea }.random().also { HomeFocusMemory.lastIdea = it }
    }

    WonderBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV overscan safe zone
        ) {
            // ============================================================
            // 1. TOP BAR: logo · profile · parents
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WonderLoopLogo()
                Spacer(modifier = Modifier.weight(1f))
                if (backendOnline == false) {
                    HeaderChip(
                        icon = "⚠️",
                        label = if (en) "Offline · Retry" else "Sin conexión · Reintentar",
                        focusRequester = requester("retry"),
                        onClick = onRetryConnection,
                        accent = HomeCoral
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                HeaderChip(
                    icon = childAvatar.ifBlank { "🧒" },
                    label = "${childName.ifBlank { if (en) "Explorer" else "Explorador" }}  ∨",
                    focusRequester = requester("switch"),
                    onClick = remembered("switch", onSwitchProfileClick),
                    contentDescription = if (en) "Switch profile" else "Cambiar de perfil"
                )
                Spacer(modifier = Modifier.width(10.dp))
                HeaderChip(
                    icon = "👤",
                    label = if (en) "My profile" else "Mi perfil",
                    focusRequester = requester("profile"),
                    onClick = remembered("profile", onEditProfileClick)
                )
                Spacer(modifier = Modifier.width(10.dp))
                HeaderChip(
                    icon = "",
                    label = if (en) "Parents" else "Padres",
                    focusRequester = requester("parents"),
                    onClick = remembered("parents", onParentsClick),
                    lockIcon = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ============================================================
            // 2. HERO ROW: create a story · continue reading
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CreateStoryCard(
                    childName = childName,
                    idea = idea,
                    focusRequester = requester("create"),
                    onClick = remembered("create", onNewAdventureClick),
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight()
                )
                if (displayStory != null) {
                    ContinueReadingCard(
                        story = displayStory,
                        focusRequester = requester("continue"),
                        onClick = remembered("continue") { onOpenStory(displayStory) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    WelcomeCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            // ============================================================
            // 3. RAIL: my stories (16:9 covers from the real illustrations)
            // ============================================================
            if (sagas.isNotEmpty()) {
                // Rail rhythm: ~16dp from the row above to its title, ~12dp from title to cards
                // (the 8dp content padding leaves room for the 1.08x focus scale)
                Spacer(modifier = Modifier.height(16.dp))
                RailHeader(
                    title = if (en) "My stories" else "Mis cuentos",
                    count = if (en) "${sagas.size} ${if (sagas.size == 1) "book" else "books"}" else "${sagas.size} ${if (sagas.size == 1) "libro" else "libros"}"
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.railFocusEntry { requester("story:${sagas.first().sagaId}") },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    items(sagas.take(10), key = { it.sagaId }) { saga ->
                        val key = "story:${saga.sagaId}"
                        StoryCoverCard(
                            saga = saga,
                            focusRequester = requester(key),
                            onClick = remembered(key) { onOpenStory(saga.latestChapter) }
                        )
                    }
                    item(key = "library") {
                        SeeAllCard(
                            icon = "📚",
                            label = if (en) "Open library" else "Ver biblioteca",
                            width = 224.dp,
                            height = 126.dp,
                            focusRequester = requester("library"),
                            onClick = remembered("library", onNavigateToStories)
                        )
                    }
                }
            } else {
                // New profile: the rail shows where books will appear, with one card to start
                Spacer(modifier = Modifier.height(16.dp))
                RailHeader(title = if (en) "My stories" else "Mis cuentos", count = if (en) "Empty for now" else "Aún vacío")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EmptySlotCard(
                        icon = "＋",
                        label = if (en) "Your first story" else "Tu primer cuento",
                        width = 224.dp,
                        height = 126.dp,
                        focusRequester = requester("empty_story"),
                        onClick = remembered("empty_story", onNewAdventureClick)
                    )
                    repeat(3) { GhostCard(width = 224.dp, height = 126.dp) }
                }
            }

            // ============================================================
            // 4. RAIL: my friends (characters drawn by the child)
            // ============================================================
            if (friends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                RailHeader(
                    title = if (en) "My friends" else "Mis amigos",
                    count = if (en) "${friends.size} ${if (friends.size == 1) "friend" else "friends"}" else "${friends.size} ${if (friends.size == 1) "amigo" else "amigos"}"
                )
                Spacer(modifier = Modifier.height(2.dp))
                LazyRow(
                    modifier = Modifier.railFocusEntry { requester("friend:${friends.first().name.lowercase()}") },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    itemsIndexed(friends.take(12), key = { _, f -> "friend:${f.name.lowercase()}" }) { index, friend ->
                        val key = "friend:${friend.name.lowercase()}"
                        FriendPolaroid(
                            friend = friend,
                            index = index,
                            focusRequester = requester(key),
                            onClick = remembered(key) {
                                val story = profileStories.firstOrNull { s ->
                                    s.character.name.equals(friend.name, ignoreCase = true) ||
                                        s.sagaHero.name.equals(friend.name, ignoreCase = true)
                                }
                                if (story != null) onOpenStory(story) else onNavigateToCharacters()
                            }
                        )
                    }
                    item(key = "friends_all") {
                        SeeAllCard(
                            icon = "🎨",
                            label = if (en) "All friends" else "Ver todos",
                            width = 132.dp,
                            height = 164.dp,
                            focusRequester = requester("friends_all"),
                            onClick = remembered("friends_all", onNavigateToCharacters)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                RailHeader(title = if (en) "My friends" else "Mis amigos", count = if (en) "Draw the first one" else "Dibuja el primero")
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EmptyFriendPolaroid(
                        focusRequester = requester("empty_friend"),
                        onClick = remembered("empty_friend", onNewAdventureClick)
                    )
                    repeat(4) { index -> GhostPolaroid(index = index + 1) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Leanback-style rail entry: ▲ ▼ into a rail land on its first card, or on the card the child
 * last left there (the rail keeps its scroll), never on whatever card happens to sit below.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.railFocusEntry(firstItem: () -> FocusRequester): Modifier =
    focusRestorer { firstItem() }

@Composable
private fun WonderLoopLogo() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WonderLoopMark(modifier = Modifier.size(width = 40.dp, height = 24.dp))
        Row {
            Text(text = "Wonder", fontFamily = TitleFont(), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5484D))
            Text(text = "Loop", fontFamily = TitleFont(), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFF12A58A))
        }
    }
}

@Composable
private fun HeaderChip(
    icon: String,
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    accent: Color = HomeInk,
    contentDescription: String? = null,
    lockIcon: Boolean = false
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(50),
        containerColor = Color.White.copy(alpha = 0.85f),
        focusedContainerColor = Color.White,
        borderColor = HomeCardBorder,
        contentDescription = contentDescription ?: label
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .align(Alignment.Center)
        ) {
            if (lockIcon) {
                // Same line-art lock as the profile picker: grown-up area, not a toy to press
                MinimalistLockIcon(tint = accent, size = 16.dp)
            } else {
                Text(text = icon, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
        }
    }
}

@Composable
private fun CreateStoryCard(childName: String, idea: DailyIdea, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier) {
    val en = AppStrings.isEnglish
    val name = childName.ifBlank { if (en) "explorer" else "explorador" }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.focusRequester(focusRequester),
        shape = RoundedCornerShape(26.dp),
        containerColor = HomeCoral,
        contentColor = Color.White,
        focusedScale = 1.04f,
        glowColor = HomeCoral.copy(alpha = 0.4f),
        contentDescription = if (en) "Create a new story with your drawing" else "Crear un cuento nuevo con tu dibujo"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(HomeCoral, HomeCoralEnd)))
                .padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
        ) {
            DailyIdeaNote(
                idea = idea,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(212.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 228.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (en) "Hi, $name! 👋" else "¡Hola, $name! 👋",
                        fontFamily = TitleFont(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        // Balanced break instead of a lone "hoy?" on the second line
                        text = if (en) "What story shall we\ncreate today?" else "¿Qué cuento vamos\na crear hoy?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 23.sp,
                        maxLines = 2
                    )
                }
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (en) "📱 Scan my drawing" else "📱 Escanear mi dibujo",
                            fontFamily = TitleFont(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeCoral
                        )
                    }
                }
            }
        }
    }
}

/** Today's drawing idea as a slightly tilted paper note taped onto the card. */
@Composable
private fun DailyIdeaNote(idea: DailyIdea, modifier: Modifier = Modifier) {
    val en = AppStrings.isEnglish
    Box(modifier = modifier.rotate(3f), contentAlignment = Alignment.TopCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFF6DE))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, HomeGold, CircleShape)
            ) {
                Text(text = idea.emoji, fontSize = 27.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (en) "💡 TODAY'S IDEA" else "💡 IDEA DE HOY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF9E6700)
                )
                Text(
                    text = if (en) idea.en else idea.es,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HomeInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Washi tape
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 18.dp)
                .rotate(-4f)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun ContinueReadingCard(story: FullStoryResponse, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier) {
    val en = AppStrings.isEnglish
    val image = story.scenes.firstOrNull { !it.illustrationUrl.isNullOrBlank() }?.illustrationUrl
        ?: story.sagaHero.reinterpretedImageUrl
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.focusRequester(focusRequester),
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        focusedScale = 1.04f,
        contentDescription = (if (en) "Continue reading " else "Seguir leyendo ") + story.title
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WonderLoopImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f)))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HomeGold)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (en) "▶  CONTINUE READING" else "▶  SEGUIR LEYENDO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HomeInk
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.title,
                    fontFamily = TitleFont(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val chapter = story.chapterIndex ?: 1
                Text(
                    text = "✨ ${story.sagaHero.name}" + if (chapter > 1) (if (en) " · Chapter $chapter" else " · Capítulo $chapter") else "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun WelcomeCard(modifier: Modifier) {
    val en = AppStrings.isEnglish
    val steps = if (en) {
        listOf("🖍️" to "Draw a character", "📱" to "Scan it with a phone", "📺" to "Watch it come alive here!")
    } else {
        listOf("🖍️" to "Dibuja un personaje", "📱" to "Escanéalo con el móvil", "📺" to "¡Míralo cobrar vida aquí!")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = if (en) "It's this easy!" else "¡Así de fácil!",
                fontFamily = TitleFont(),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = HomeInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            steps.forEachIndexed { i, (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (i == 0) HomeCoralSoft else HomeIvoryGlow)
                    ) {
                        Text(text = icon, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeInkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Dashed, focusable "start here" slot shown in an empty rail. */
@Composable
private fun EmptySlotCard(
    icon: String,
    label: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .size(width = width, height = height)
            // Only focusable card of its rail: RIGHT must not escape diagonally
            .onPreviewKeyEvent { it.key == Key.DirectionRight }
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White.copy(alpha = 0.7f),
        focusedContainerColor = Color.White,
        focusedScale = 1.08f,
        contentDescription = label
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dashedOutline(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HomeCoralSoft)
                ) {
                    Text(text = icon, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HomeCoral)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = label, fontFamily = TitleFont(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HomeCoral)
            }
        }
    }
}

/** A faint, non-focusable placeholder hinting at where future books will line up. */
@Composable
private fun GhostCard(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.35f))
    )
}

/** The first friend slot: a pinned Polaroid with a "?" silhouette that leads to scanning. */
@Composable
private fun EmptyFriendPolaroid(focusRequester: FocusRequester, onClick: () -> Unit) {
    val en = AppStrings.isEnglish
    Box(modifier = Modifier.size(width = 132.dp, height = 164.dp).rotate(-2.5f)) {
        TvClickableSurface(
            onClick = onClick,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxSize()
                .onPreviewKeyEvent { it.key == Key.DirectionRight }
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            borderColor = HomeCardBorder,
            focusedScale = 1.08f,
            contentDescription = if (en) "Scan your first friend" else "Escanea tu primer amigo"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HomeIvoryGlow)
                        .dashedOutline(6.dp)
                ) {
                    Text(text = "?", fontFamily = TitleFont(), fontSize = 46.sp, fontWeight = FontWeight.Bold, color = HomeCoral.copy(alpha = 0.6f))
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (en) "Your first friend" else "Tu primer amigo",
                        fontFamily = TitleFont(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeCoral,
                        maxLines = 1
                    )
                }
            }
        }
        Pushpin3D(
            pinColor = PinColors[0],
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
        )
    }
}

/** A faint, non-focusable Polaroid waiting for a future friend. */
@Composable
private fun GhostPolaroid(index: Int) {
    val tilt = listOf(-2.5f, 1.8f, -1.2f, 2.4f)[index % 4]
    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 164.dp)
            .rotate(tilt)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.35f))
    )
}

private fun Modifier.dashedOutline(corner: androidx.compose.ui.unit.Dp): Modifier = drawBehind {
    drawRoundRect(
        color = HomeCoral.copy(alpha = 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx()),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
        )
    )
}

@Composable
private fun RailHeader(title: String, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontFamily = TitleFont(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HomeInk)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(HomeCoralSoft)
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HomeCoral)
        }
    }
}

@Composable
private fun StoryCoverCard(saga: SagaBook, focusRequester: FocusRequester, onClick: () -> Unit) {
    val en = AppStrings.isEnglish
    val story = saga.latestChapter
    val cover = saga.rootStory.scenes.firstOrNull { !it.illustrationUrl.isNullOrBlank() }?.illustrationUrl
        ?: saga.rootStory.sagaHero.reinterpretedImageUrl
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 224.dp, height = 126.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        focusedScale = 1.08f,
        contentDescription = story.title
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WonderLoopImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f)))
            )
            if (saga.isMultiChapter) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HomeGold)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (en) "🌟 ${saga.chapterCount} chapters" else "🌟 ${saga.chapterCount} capítulos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HomeInk
                    )
                }
            }
            Text(
                text = saga.rootStory.title,
                fontFamily = TitleFont(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

/** A friend's portrait as a pinned Polaroid, like the corkboard in "Mis amigos". */
@Composable
private fun FriendPolaroid(friend: CharacterProfile, index: Int, focusRequester: FocusRequester, onClick: () -> Unit) {
    val tilt = listOf(-2.5f, 1.8f, -1.2f, 2.4f)[index % 4]
    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 164.dp)
            .rotate(tilt)
    ) {
        TvClickableSurface(
            onClick = onClick,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxSize()
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            borderColor = HomeCardBorder,
            focusedScale = 1.08f,
            contentDescription = friend.name
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WonderLoopImage(
                    model = friend.reinterpretedImageUrl ?: friend.originalDrawingBase64,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HomeIvoryGlow)
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = friend.name,
                        fontFamily = TitleFont(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Pushpin3D(
            pinColor = PinColors[index % PinColors.size],
            // Above the card even while it is scaled up by focus
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
        )
    }
}

@Composable
private fun SeeAllCard(
    icon: String,
    label: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .size(width = width, height = height)
            // Last card of a rail: RIGHT must not escape diagonally to the top bar
            .onPreviewKeyEvent { it.key == Key.DirectionRight }
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        borderColor = HomeCardBorder,
        focusedScale = 1.08f,
        contentDescription = label
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(HomeTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$label →", fontFamily = TitleFont(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HomeTeal)
        }
    }
}
