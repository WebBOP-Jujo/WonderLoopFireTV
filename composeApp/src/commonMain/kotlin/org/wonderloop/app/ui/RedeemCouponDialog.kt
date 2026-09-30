package org.wonderloop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.wonderloop.app.data.WonderLoopApi
import org.wonderloop.app.theme.*

private val CouponInkDark = Color(0xFF2B2340)
private val CouponInkMuted = Color(0xFF6B6480)
private val CouponCoral = Color(0xFFD9384A)
private val CouponCoralSoft = Color(0xFFFDECE9)
private val CouponMint = Color(0xFF0E7F64)
private val CouponGoldSoft = Color(0xFFFFF1CF)
private val CouponCardBorder = Color(0xFFEDE8E1)

@Composable
fun RedeemCouponDialog(
    api: WonderLoopApi,
    userId: String,
    onDismiss: () -> Unit,
    onCouponRedeemed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var codeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, CouponCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CouponGoldSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎟️", fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = AppStrings.redeemDialogTitle,
                    fontFamily = TitleFont(),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CouponInkDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AppStrings.redeemDialogPrompt,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CouponInkMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "Ej. SHIPATON2026",
                            color = CouponInkMuted.copy(alpha = 0.65f),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CouponCoral,
                        unfocusedBorderColor = CouponCardBorder,
                        focusedContainerColor = Color(0xFFFFFBF5),
                        unfocusedContainerColor = Color(0xFFFFFBF5),
                        focusedTextColor = CouponInkDark,
                        unfocusedTextColor = CouponInkDark,
                        cursorColor = CouponCoral
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CouponCoralSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = CouponCoral,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = successMessage ?: "",
                        fontSize = 13.sp,
                        color = CouponMint,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (codeInput.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val res = api.redeemPromoCode(userId, codeInput.trim())
                                    isLoading = false
                                    if (res.success) {
                                        successMessage = res.message
                                        onCouponRedeemed()
                                    } else {
                                        errorMessage = res.message
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = AppStrings.couponValidationFailed
                                }
                            }
                        }
                    },
                    enabled = !isLoading && codeInput.isNotBlank(),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouponCoral,
                        contentColor = Color.White,
                        disabledContainerColor = CouponCoral.copy(alpha = 0.45f),
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = AppStrings.redeemButton,
                            fontFamily = TitleFont(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.5.dp, CouponCardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = CouponInkMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = AppStrings.close,
                        fontFamily = TitleFont(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CouponInkMuted
                    )
                }
            }
        }
    }
}
