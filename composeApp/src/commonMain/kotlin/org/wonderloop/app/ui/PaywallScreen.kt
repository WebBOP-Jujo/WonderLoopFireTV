package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.data.SubscriptionPlan
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.platform.PlatformAudioService
import org.wonderloop.app.platform.PlatformBillingService
import org.wonderloop.app.platform.SoundEffect
import org.wonderloop.app.theme.*
import org.wonderloop.app.ui.components.tvFocusable

enum class PaywallPlan(val id: String) {
    ANNUAL_STAR("wonderloop_annual_star_15"),
    ANNUAL_BASIC("wonderloop_annual_basic_6"),
    MONTHLY_PRO("wonderloop_pro_15"),
    MONTHLY_BASIC("wonderloop_basic_6"),
    PACK_FIVE("wonderloop_pack_5")
}

private enum class SelectedTier {
    STORYTELLER,    // Cuentacuentos 3,99 €/mes
    STAR_ANNUAL,    // Anual Estrella 4,99 €/mes (59,99 €/año) - Most Popular
    MAGIC_CREATOR,  // Creador Mágico 7,99 €/mes
    PACK_FIVE       // Pack 5 cuentos 2,99 €
}

private fun Modifier.paywallDashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Float = 14f,
    gapLength: Float = 8f
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
    )
    drawRoundRect(
        color = color,
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}

@Composable
fun PaywallScreen(
    api: WonderLoopApi,
    userId: String,
    onPurchaseCompleted: () -> Unit,
    onCloseClick: () -> Unit
) {
    var selectedTier by remember { mutableStateOf(SelectedTier.STAR_ANNUAL) }
    var isProcessing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showRedeemDialog by remember { mutableStateOf(false) }

    val selectedPlan = when (selectedTier) {
        SelectedTier.STORYTELLER -> PaywallPlan.MONTHLY_BASIC
        SelectedTier.STAR_ANNUAL -> PaywallPlan.ANNUAL_STAR
        SelectedTier.MAGIC_CREATOR -> PaywallPlan.MONTHLY_PRO
        SelectedTier.PACK_FIVE -> PaywallPlan.PACK_FIVE
    }

    val isSubscriber = SubscriptionManager.isPremium

    if (showRedeemDialog) {
        RedeemCouponDialog(
            api = api,
            userId = userId,
            onDismiss = { showRedeemDialog = false },
            onCouponRedeemed = {
                showRedeemDialog = false
                feedbackMessage = AppStrings.vipActivated
                onPurchaseCompleted()
            }
        )
    }

    val performPurchase = {
        isProcessing = true
        feedbackMessage = null
        PlatformBillingService.purchasePlan(
            planId = selectedPlan.id,
            onSuccess = {
                isProcessing = false
                when (selectedPlan) {
                    PaywallPlan.ANNUAL_STAR -> {
                        SubscriptionManager.activatePlan(SubscriptionPlan.STAR_ANNUAL)
                        feedbackMessage = AppStrings.planActivatedSuccess
                    }
                    PaywallPlan.ANNUAL_BASIC -> {
                        SubscriptionManager.activatePlan(SubscriptionPlan.BASIC_ANNUAL)
                        feedbackMessage = AppStrings.planActivatedSuccess
                    }
                    PaywallPlan.MONTHLY_PRO -> {
                        SubscriptionManager.activatePlan(SubscriptionPlan.PRO_MONTHLY)
                        feedbackMessage = AppStrings.planActivatedSuccess
                    }
                    PaywallPlan.MONTHLY_BASIC -> {
                        SubscriptionManager.activatePlan(SubscriptionPlan.BASIC_MONTHLY)
                        feedbackMessage = AppStrings.planActivatedSuccess
                    }
                    PaywallPlan.PACK_FIVE -> {
                        SubscriptionManager.buyPack(SubscriptionManager.isPremium)
                        feedbackMessage = AppStrings.packAddedSuccess
                    }
                }
                onPurchaseCompleted()
            },
            onError = { err ->
                isProcessing = false
                feedbackMessage = err
            }
        )
    }

    // 100% WIDESCREEN 16:9 CANVAS (1920x1080) - NO VERTICAL SCROLL
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
            .padding(horizontal = 48.dp, vertical = 22.dp) // Fire TV 5% Safe Zone
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // TOP BAR (.pwtop)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFF1CF)
                ) {
                    Text(
                        text = "✨ WonderLoop Pro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB87800),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .height(38.dp)
                        .tvFocusable(
                            shape = RoundedCornerShape(20.dp),
                            focusedScale = 1.08f,
                            onClick = {
                                PlatformAudioService.playSfx(SoundEffect.POP)
                                onCloseClick()
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "✕", fontSize = 13.sp, color = Color(0xFF2B2340))
                        Text(
                            text = "Cerrar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2340)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================================================================
            // 2-COLUMN WIDESCREEN BODY (.pw)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                // -------------------------------------------------------------
                // LEFT COLUMN (.pwl): Star + Title + Benefits
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(0.75f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF1CF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "★", fontSize = 28.sp, color = Color(0xFFE0A100))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Desbloquea el universo mágico",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2B2340),
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Cuentos ilimitados y voces mágicas para toda la familia",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A4260),
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6 Benefit Checkmarks
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Cuentos únicos creados con IA",
                            "Ilustraciones HD en acuarela",
                            "Narración con voz",
                            "Cuentos sin caducidad",
                            "Hasta 3 perfiles",
                            "100 % seguro y sin anuncios"
                        ).forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDDF5EE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0E7F64)
                                    )
                                }
                                Text(
                                    text = benefit,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B2340)
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // RIGHT COLUMN (.pwr): 3 Plans + Pack + CTA + Amazon Legal
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row of 3 Plans (.pcs)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Cuentacuentos
                        val isStoryteller = selectedTier == SelectedTier.STORYTELLER
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isStoryteller) Color(0xFFFFF8F2) else Color.White,
                            border = BorderStroke(
                                2.5.dp,
                                if (isStoryteller) Color(0xFFD9384A) else Color(0xFFEFE7EA)
                            ),
                            shadowElevation = if (isStoryteller) 6.dp else 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(22.dp),
                                    focusedScale = 1.04f,
                                    onClick = { selectedTier = SelectedTier.STORYTELLER }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cuentacuentos",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2340)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.dp,
                                                if (isStoryteller) Color(0xFFD9384A) else Color(0xFFDCD3C8),
                                                CircleShape
                                            )
                                            .background(if (isStoryteller) Color(0xFFD9384A) else Color.Transparent)
                                    )
                                }

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "3,99 €",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2340)
                                    )
                                    Text(
                                        text = "/mes",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B6480),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "• 6 cuentos al mes", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                    Text(text = "• Acumula hasta 12", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                }
                            }
                        }

                        // 2. Anual Estrella (POPULAR)
                        val isStar = selectedTier == SelectedTier.STAR_ANNUAL
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isStar) Color(0xFFFFF8F2) else Color.White,
                            border = BorderStroke(
                                2.5.dp,
                                if (isStar) Color(0xFFD9384A) else Color(0xFFEFE7EA)
                            ),
                            shadowElevation = if (isStar) 8.dp else 2.dp,
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(22.dp),
                                    focusedScale = 1.04f,
                                    onClick = { selectedTier = SelectedTier.STAR_ANNUAL }
                                )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Ribbon on Top
                                Surface(
                                    color = Color(0xFFD9384A),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "★ MÁS POPULAR · −38 %",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Anual Estrella",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2B2340)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    if (isStar) Color(0xFFD9384A) else Color(0xFFDCD3C8),
                                                    CircleShape
                                                )
                                                .background(if (isStar) Color(0xFFD9384A) else Color.Transparent)
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "4,99 €",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFD9384A)
                                            )
                                            Text(
                                                text = "/mes",
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B6480),
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "59,99 € al año",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6B6480)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "• 15 cuentos al mes", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                        Text(text = "• Acumula hasta 30", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                    }
                                }
                            }
                        }

                        // 3. Creador Mágico
                        val isMagic = selectedTier == SelectedTier.MAGIC_CREATOR
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isMagic) Color(0xFFFFF8F2) else Color.White,
                            border = BorderStroke(
                                2.5.dp,
                                if (isMagic) Color(0xFFD9384A) else Color(0xFFEFE7EA)
                            ),
                            shadowElevation = if (isMagic) 6.dp else 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .tvFocusable(
                                    shape = RoundedCornerShape(22.dp),
                                    focusedScale = 1.04f,
                                    onClick = { selectedTier = SelectedTier.MAGIC_CREATOR }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Creador Mágico",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2340)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.dp,
                                                if (isMagic) Color(0xFFD9384A) else Color(0xFFDCD3C8),
                                                CircleShape
                                            )
                                            .background(if (isMagic) Color(0xFFD9384A) else Color.Transparent)
                                    )
                                }

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "7,99 €",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2340)
                                    )
                                    Text(
                                        text = "/mes",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B6480),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "• 15 cuentos al mes", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                    Text(text = "• Acumula hasta 30", fontSize = 11.5.sp, color = Color(0xFF6B6480))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pack 5 Cuentos (.pk)
                    val isPack = selectedTier == SelectedTier.PACK_FIVE
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .paywallDashedBorder(2.dp, Color(0xFFE8C9A8), 18.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(18.dp),
                                focusedScale = 1.03f,
                                onClick = { selectedTier = SelectedTier.PACK_FIVE }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(
                                            2.dp,
                                            if (isPack) Color(0xFFD9384A) else Color(0xFFDCD3C8),
                                            CircleShape
                                        )
                                        .background(if (isPack) Color(0xFFD9384A) else Color.Transparent)
                                )
                                Text(
                                    text = "Pack 5 cuentos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2B2340)
                                )
                                Text(
                                    text = "Pago único · sin caducidad",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B6480)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "2,99 €",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2B2340)
                                )
                                Text(
                                    text = "−33 % con Pro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0E7F64)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big CTA Button (.pcta)
                    val ctaText = when (selectedTier) {
                        SelectedTier.STAR_ANNUAL -> "🚀 Suscribirme · 59,99 € al año"
                        SelectedTier.STORYTELLER -> "🚀 Suscribirme · 3,99 € al mes"
                        SelectedTier.MAGIC_CREATOR -> "🚀 Suscribirme · 7,99 € al mes"
                        SelectedTier.PACK_FIVE -> "🚀 Comprar Pack · 2,99 €"
                    }
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFFD9384A),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(26.dp),
                                focusedScale = 1.04f,
                                onClick = {
                                    PlatformAudioService.playSfx(SoundEffect.MAGIC_CHIME)
                                    performPurchase()
                                }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = ctaText,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Amazon Legal Note (.pleg)
                    Text(
                        text = "Se renueva automáticamente salvo que canceles 24 h antes del fin del periodo. Se cobra a tu cuenta de Amazon y puedes cancelar cuando quieras.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF6B6480),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Links (.plk): Restaurar compras & Canjear código
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFE8DE),
                            modifier = Modifier
                                .height(32.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    focusedScale = 1.06f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        PlatformBillingService.restorePurchases(
                                            onSuccess = { feedbackMessage = AppStrings.restoreSuccess },
                                            onError = { feedbackMessage = AppStrings.restoreNotFound }
                                        )
                                    }
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Restaurar compras",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B2340)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFE8DE),
                            modifier = Modifier
                                .height(32.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    focusedScale = 1.06f,
                                    onClick = {
                                        PlatformAudioService.playSfx(SoundEffect.POP)
                                        showRedeemDialog = true
                                    }
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "🎫 Canjear código",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD9384A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
