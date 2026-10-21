package org.wonderloop.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
}

/** A concrete, drawable idea that changes every day (shown on the "create" card). */
private val DAILY_IDEAS = listOf(
    "un león con una gran melena amarilla" to "a lion with a big yellow mane",
    "un barco pirata con velas de rayas" to "a pirate ship with striped sails",
    "un robot con ruedas y una antena" to "a robot with wheels and an antenna",
    "una tortuga con un caparazón de colores" to "a turtle with a colorful shell",
    "un dragón pequeño con alas verdes" to "a little dragon with green wings",
    "un cohete que va a la Luna" to "a rocket flying to the Moon",
    "un gato con botas rojas" to "a cat wearing red boots"
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

    val idea = remember {
        val day = (PlatformStorage.currentTimeMillis() / 86_400_000L).toInt()
        DAILY_IDEAS[((day % DAILY_IDEAS.size) + DAILY_IDEAS.size) % DAILY_IDEAS.size]
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
                    .height(48.dp),
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
                    Spacer(modifier = Modifier.width(12.dp))
                }
                HeaderChip(
                    icon = childAvatar.ifBlank { "🧒" },
                    label = "${childName.ifBlank { if (en) "Explorer" else "Explorador" }}  ∨",
                    focusRequester = requester("switch"),
                    onClick = remembered("switch", onSwitchProfileClick),
                    contentDescription = if (en) "Switch profile" else "Cambiar de perfil"
                )
                Spacer(modifier = Modifier.width(12.dp))
                HeaderChip(
                    icon = "👤",
                    label = if (en) "My profile" else "Mi perfil",
                    focusRequester = requester("profile"),
                    onClick = remembered("profile", onEditProfileClick)
                )
                Spacer(modifier = Modifier.width(12.dp))
                HeaderChip(
                    icon = "🔒",
                    label = if (en) "Parents" else "Padres",
                    focusRequester = requester("parents"),
                    onClick = remembered("parents", onParentsClick)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ============================================================
            // 2. HERO ROW: create a story · continue reading
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CreateStoryCard(
                    idea = if (en) idea.second else idea.first,
                    focusRequester = requester("create"),
                    onClick = remembered("create", onNewAdventureClick),
                    modifier = Modifier
                        .weight(1.45f)
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
                Spacer(modifier = Modifier.height(20.dp))
                RailHeader(
                    title = if (en) "My stories" else "Mis cuentos",
                    count = if (en) "${sagas.size} ${if (sagas.size == 1) "book" else "books"}" else "${sagas.size} ${if (sagas.size == 1) "libro" else "libros"}"
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.railFocusEntry { requester("story:${sagas.first().sagaId}") },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
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
            }

            // ============================================================
            // 4. RAIL: my friends (characters drawn by the child)
            // ============================================================
            if (friends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                RailHeader(
                    title = if (en) "My friends" else "Mis amigos",
                    count = if (en) "${friends.size} ${if (friends.size == 1) "friend" else "friends"}" else "${friends.size} ${if (friends.size == 1) "amigo" else "amigos"}"
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.railFocusEntry { requester("friend:${friends.first().name.lowercase()}") },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    items(friends.take(12), key = { "friend:${it.name.lowercase()}" }) { friend ->
                        val key = "friend:${friend.name.lowercase()}"
                        FriendCard(
                            friend = friend,
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
                            height = 150.dp,
                            focusRequester = requester("friends_all"),
                            onClick = remembered("friends_all", onNavigateToCharacters)
                        )
                    }
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(modifier = Modifier.size(width = 46.dp, height = 30.dp)) {
            val strokeW = 5.dp.toPx()
            val r = 11.dp.toPx()
            drawCircle(color = Color(0xFFFF6B6B), radius = r, center = Offset(14.dp.toPx(), 15.dp.toPx()), style = Stroke(width = strokeW))
            drawCircle(color = Color(0xFF1FBE9C), radius = r, center = Offset(32.dp.toPx(), 15.dp.toPx()), style = Stroke(width = strokeW))
        }
        Row {
            Text(text = "Wonder", fontFamily = TitleFont(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5484D))
            Text(text = "Loop", fontFamily = TitleFont(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF12A58A))
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
    contentDescription: String? = null
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(50),
        containerColor = Color.White,
        borderColor = HomeCardBorder,
        contentDescription = contentDescription ?: label
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = accent, maxLines = 1)
        }
    }
}

@Composable
private fun CreateStoryCard(idea: String, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier) {
    val en = AppStrings.isEnglish
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
            // Drawing → story, at a glance
            DrawingToStoryArt(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 136.dp, height = 140.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 146.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (en) "✏️  NEW STORY" else "✏️  NUEVO CUENTO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (en) "Turn your drawing into a story" else "Convierte tu dibujo en un cuento",
                        fontFamily = TitleFont(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 30.sp,
                        maxLines = 2
                    )
                }
                Column {
                    Text(
                        text = if (en) "💡 Today's idea: $idea" else "💡 Idea de hoy: $idea",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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

/** A crayon sheet of paper turning into a sparkling storybook. */
@Composable
private fun DrawingToStoryArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 96.dp, height = 112.dp)
                .rotate(-8f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            Text(text = "🦖", fontSize = 48.sp)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp)
                .clip(CircleShape)
                .background(HomeGold)
        ) {
            Text(text = "📖", fontSize = 32.sp)
        }
        Text(
            text = "✨",
            fontSize = 26.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(text = "🖍️ 📱 📺", fontSize = 30.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (en) "Your first story is waiting" else "Tu primer cuento te espera",
                fontFamily = TitleFont(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HomeInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (en) "Draw a character on paper, scan it with any phone and watch it come alive here."
                else "Dibuja un personaje en un papel, escanéalo con cualquier móvil y míralo cobrar vida aquí.",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeInkMuted,
                lineHeight = 22.sp
            )
        }
    }
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

@Composable
private fun FriendCard(friend: CharacterProfile, focusRequester: FocusRequester, onClick: () -> Unit) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 132.dp, height = 150.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        borderColor = HomeCardBorder,
        focusedScale = 1.08f,
        contentDescription = friend.name
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Color(0xFFFFF6E3))
            ) {
                WonderLoopImage(
                    model = friend.reinterpretedImageUrl ?: friend.originalDrawingBase64,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
