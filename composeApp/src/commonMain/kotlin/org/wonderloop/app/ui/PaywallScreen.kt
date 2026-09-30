package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.data.SubscriptionPlan
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.platform.PlatformBillingService
import org.wonderloop.app.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class PaywallPlan(val id: String) {
    ANNUAL_STAR("wonderloop_annual_star_15"),
    ANNUAL_BASIC("wonderloop_annual_basic_6"),
    MONTHLY_PRO("wonderloop_pro_15"),
    MONTHLY_BASIC("wonderloop_basic_6"),
    PACK_FIVE("wonderloop_pack_5")
}

private enum class SelectedTier {
    CREATOR_PRO,
    STORYTELLER_BASIC,
    PACK_FIVE
}

@Composable
fun PaywallScreen(
    api: WonderLoopApi,
    userId: String,
    onPurchaseCompleted: () -> Unit,
    onCloseClick: () -> Unit
) {
    var isAnnual by remember { mutableStateOf(true) }
    var selectedTier by remember { mutableStateOf(SelectedTier.CREATOR_PRO) }
    var isProcessing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showRedeemDialog by remember { mutableStateOf(false) }

    val selectedPlan = when (selectedTier) {
        SelectedTier.CREATOR_PRO -> if (isAnnual) PaywallPlan.ANNUAL_STAR else PaywallPlan.MONTHLY_PRO
        SelectedTier.STORYTELLER_BASIC -> if (isAnnual) PaywallPlan.ANNUAL_BASIC else PaywallPlan.MONTHLY_BASIC
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
                SubscriptionManager.redeemCode("SHIPATON2026")
                feedbackMessage = AppStrings.vipActivated
                onPurchaseCompleted()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF5))
    ) {
        // Scrollable Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Brand Pill ("✨ WonderLoop Pro") + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFF1CF))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = AppStrings.paywallBrandBadge,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB87800)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFEBE4))
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B6480)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hero Star Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1CF))
            ) {
                GoldStarOutlineIcon(
                    color = Color(0xFFB87800),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Subtitle
            Text(
                text = AppStrings.paywallTitle,
                fontFamily = TitleFont(),
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2B2340),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.paywallSubtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B6480),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6 Real Benefits in Compact 2x3 Grid (1 line each)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactBenefitPill(
                        icon = "✨",
                        label = AppStrings.gridBenefitStories,
                        modifier = Modifier.weight(1f)
                    )
                    CompactBenefitPill(
                        icon = "🎨",
                        label = AppStrings.gridBenefitWatercolors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactBenefitPill(
                        icon = "🎙️",
                        label = AppStrings.gridBenefitNarration,
                        modifier = Modifier.weight(1f)
                    )
                    CompactBenefitPill(
                        icon = "🔄",
                        label = AppStrings.gridBenefitRollover,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactBenefitPill(
                        icon = "👨‍👩‍👧‍👦",
                        label = AppStrings.gridBenefitProfiles,
                        modifier = Modifier.weight(1f)
                    )
                    CompactBenefitPill(
                        icon = "🛡️",
                        label = AppStrings.gridBenefitNoAds,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Toggle: Anual / Mensual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFEDE8E1))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Anual Tab
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAnnual) Color.White else Color.Transparent,
                    shadowElevation = if (isAnnual) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .clickable {
                            isAnnual = true
                            if (selectedTier == SelectedTier.PACK_FIVE) {
                                selectedTier = SelectedTier.CREATOR_PRO
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = AppStrings.paywallToggleAnnual,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAnnual) Color(0xFF2B2340) else Color(0xFF6B6480)
                        )
                    }
                }

                // Mensual Tab
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (!isAnnual) Color.White else Color.Transparent,
                    shadowElevation = if (!isAnnual) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .clickable {
                            isAnnual = false
                            if (selectedTier == SelectedTier.PACK_FIVE) {
                                selectedTier = SelectedTier.CREATOR_PRO
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = AppStrings.paywallToggleMonthly,
                            fontSize = 13.sp,
                            fontWeight = if (!isAnnual) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (!isAnnual) Color(0xFF2B2340) else Color(0xFF6B6480)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subscription Card 1: Plan Creador Mágico (Familiar - 15 cuentos/mes)
            val isCreatorSelected = selectedTier == SelectedTier.CREATOR_PRO
            val creatorMainPrice = if (isAnnual) {
                if (AppStrings.isEnglish) "$5.83" else "5,83€"
            } else {
                if (AppStrings.isEnglish) "$7.99" else "7,99€"
            }
            val creatorBillingNote = if (isAnnual) {
                AppStrings.paywallAnnualEquivalent
            } else {
                if (AppStrings.isEnglish) "Billed monthly" else "Facturado mensualmente"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isCreatorSelected) 2.dp else 1.5.dp,
                        color = if (isCreatorSelected) Color(0xFFD9384A) else Color(0xFFDCD3C8)
                    ),
                    shadowElevation = if (isCreatorSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedTier = SelectedTier.CREATOR_PRO }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.planProTitle,
                                fontFamily = TitleFont(),
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2B2340)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppStrings.planProSub,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6B6480)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = creatorMainPrice,
                                    fontFamily = TitleFont(),
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCreatorSelected) Color(0xFFD9384A) else Color(0xFF2B2340)
                                )
                                Text(
                                    text = if (AppStrings.isEnglish) "/mo" else "/mes",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B6480),
                                    modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                                )
                            }
                            Text(
                                text = creatorBillingNote,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6B6480)
                            )
                        }

                        PlanRadioIndicator(isSelected = isCreatorSelected)
                    }
                }

                // Floating "MÁS POPULAR" Pill Badge
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD9384A))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = AppStrings.paywallMostPopularRibbon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subscription Card 2: Plan Cuentacuentos (6 cuentos/mes)
            val isBasicSelected = selectedTier == SelectedTier.STORYTELLER_BASIC
            val basicMainPrice = if (isAnnual) {
                if (AppStrings.isEnglish) "$3.33" else "3,33€"
            } else {
                if (AppStrings.isEnglish) "$3.99" else "3,99€"
            }
            val basicBillingNote = if (isAnnual) {
                AppStrings.paywallBasicAnnualEquivalent
            } else {
                if (AppStrings.isEnglish) "Billed monthly" else "Facturado mensualmente"
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(
                    width = if (isBasicSelected) 2.dp else 1.5.dp,
                    color = if (isBasicSelected) Color(0xFFD9384A) else Color(0xFFDCD3C8)
                ),
                shadowElevation = if (isBasicSelected) 3.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { selectedTier = SelectedTier.STORYTELLER_BASIC }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.planBasicTitle,
                            fontFamily = TitleFont(),
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2B2340)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AppStrings.planBasicSub,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B6480)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = basicMainPrice,
                                fontFamily = TitleFont(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBasicSelected) Color(0xFFD9384A) else Color(0xFF2B2340)
                            )
                            Text(
                                text = if (AppStrings.isEnglish) "/mo" else "/mes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B6480),
                                modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                            )
                        }
                        Text(
                            text = basicBillingNote,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B6480)
                        )
                    }

                    PlanRadioIndicator(isSelected = isBasicSelected)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Standalone Pack Section ("SIN SUSCRIPCIÓN · PAGO ÚNICO")
            Text(
                text = AppStrings.sectionPacks,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6B6480),
                letterSpacing = 0.6.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            val isPackSelected = selectedTier == SelectedTier.PACK_FIVE

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(
                    width = if (isPackSelected) 2.dp else 1.5.dp,
                    color = if (isPackSelected) Color(0xFFD9384A) else Color(0xFFDCD3C8)
                ),
                shadowElevation = if (isPackSelected) 3.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { selectedTier = SelectedTier.PACK_FIVE }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = AppStrings.planPackTitle,
                                fontFamily = TitleFont(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2B2340)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE4F6F0))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSubscriber) AppStrings.planPackBadgeMember else AppStrings.planPackBadgeRegular,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0E7F64)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${AppStrings.planPackSub} · ${if (isSubscriber) AppStrings.paywallPackMemberSavings else AppStrings.paywallPackRegularHint}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B6480)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (isSubscriber) AppStrings.planPackPriceMember else AppStrings.planPackPriceRegular,
                                fontFamily = TitleFont(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPackSelected) Color(0xFFD9384A) else Color(0xFF2B2340)
                            )
                            Text(
                                text = if (AppStrings.isEnglish) " one-time" else " pago único",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B6480),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    PlanRadioIndicator(isSelected = isPackSelected)
                }
            }

            if (feedbackMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE4F6F0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0E7F64),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Fixed Bottom Purchase Bar
        Surface(
            color = Color(0xFFFFFBF5),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = Color(0xFFE8E2DA),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action CTA Pill
                Button(
                    onClick = {
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
                                if (err != "USER_CANCELLED") {
                                    feedbackMessage = if (AppStrings.isEnglish) "Purchase error: $err" else "Error en el proceso de compra: $err"
                                }
                            }
                        )
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9384A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        val ctaText = when (selectedPlan) {
                            PaywallPlan.ANNUAL_STAR -> AppStrings.ctaAnnual
                            PaywallPlan.ANNUAL_BASIC -> AppStrings.ctaBasicAnnual
                            PaywallPlan.MONTHLY_PRO -> AppStrings.ctaPro
                            PaywallPlan.MONTHLY_BASIC -> AppStrings.ctaBasic
                            PaywallPlan.PACK_FIVE -> if (isSubscriber) AppStrings.ctaPackMember else AppStrings.ctaPackRegular
                        }
                        Text(
                            text = ctaText,
                            fontFamily = TitleFont(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Guarantee / Legal Line
                Text(
                    text = if (selectedPlan == PaywallPlan.PACK_FIVE) {
                        AppStrings.paywallOneTimeGuarantee
                    } else {
                        AppStrings.paywallCancelGuarantee
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B6480),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Compact Links Row: Restaurar compras & Canjear código
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.restorePurchases,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6B6480),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isProcessing = true
                                PlatformBillingService.restorePurchases(
                                    onSuccess = { hasActive ->
                                        isProcessing = false
                                        if (hasActive) {
                                            feedbackMessage = AppStrings.restoreSuccess
                                            onPurchaseCompleted()
                                        } else {
                                            feedbackMessage = AppStrings.restoreNotFound
                                        }
                                    },
                                    onError = { err ->
                                        isProcessing = false
                                        feedbackMessage = if (AppStrings.isEnglish) "Restore error: $err" else "Error al restaurar: $err"
                                    }
                                )
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Text(
                        text = AppStrings.redeemCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6B6480),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRedeemDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactBenefitPill(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3EFE8))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 13.5.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF2B2340),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlanRadioIndicator(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFFD9384A) else Color(0xFFD5CFC7),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9384A))
            )
        }
    }
}

@Composable
private fun GoldStarOutlineIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = size.minDimension * 0.44f
        val innerRadius = outerRadius * 0.44f
        val path = Path()
        for (i in 0 until 10) {
            val angleDeg = -90.0 + i * 36.0
            val angleRad = Math.toRadians(angleDeg)
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = cx + (r * cos(angleRad)).toFloat()
            val y = cy + (r * sin(angleRad)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
