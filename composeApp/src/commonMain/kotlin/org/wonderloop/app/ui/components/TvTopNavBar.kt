package org.wonderloop.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wonderloop.app.theme.*

private val NavCream = Color(0xFFFFFBF5)
private val NavPaper = Color(0xFFFFFFFF)
private val NavBorder = Color(0xFFEDE8E1)
private val NavInkDark = Color(0xFF2B2340)
private val NavInkMuted = Color(0xFF6B6480)
private val NavCoral = Color(0xFFD9384A)
private val NavCoralSoft = Color(0xFFFDECE9)
private val NavMint = Color(0xFF0E7F64)
private val NavMintSoft = Color(0xFFE4F6F0)

enum class TvNavTab {
    HOME,
    CHARACTERS,
    STORIES
}

@Composable
fun TvTopNavBar(
    selectedTab: TvNavTab,
    childName: String,
    childAvatar: String,
    backendOnline: Boolean?,
    serverAddress: String,
    onTabSelected: (TvNavTab) -> Unit,
    onSwitchProfileClick: () -> Unit,
    onLanguageToggleClick: () -> Unit,
    onParentsClick: () -> Unit,
    onServerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = NavPaper.copy(alpha = 0.95f),
        border = BorderStroke(1.5.dp, NavBorder),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Brand Logo + Connectivity Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WonderLoop TV",
                        fontFamily = TitleFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavCoral
                    )
                }

                // Server Status Pill (D-Pad clickable to view/change IP)
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (backendOnline == false) NavCoralSoft else NavMintSoft,
                    border = BorderStroke(1.dp, if (backendOnline == false) NavCoral.copy(alpha = 0.4f) else NavMint.copy(alpha = 0.3f)),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.05f,
                        onClick = onServerClick
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (backendOnline == false) "🔴" else "🟢",
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (backendOnline == false) "Sin IA" else serverAddress.replace("http://", "").replace(":3000", ""),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (backendOnline == false) NavCoral else NavMint
                        )
                    }
                }
            }

            // Center: Navigation Tabs (D-Pad navigables)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvNavTabPill(
                    icon = "🏠",
                    title = if (AppStrings.isEnglish) "Living Room" else "Salón",
                    isSelected = selectedTab == TvNavTab.HOME,
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )
                TvNavTabPill(
                    icon = "🦸",
                    title = if (AppStrings.isEnglish) "Heroes" else "Mis Héroes",
                    isSelected = selectedTab == TvNavTab.CHARACTERS,
                    onClick = { onTabSelected(TvNavTab.CHARACTERS) }
                )
                TvNavTabPill(
                    icon = "📚",
                    title = if (AppStrings.isEnglish) "Stories" else "Biblioteca",
                    isSelected = selectedTab == TvNavTab.STORIES,
                    onClick = { onTabSelected(TvNavTab.STORIES) }
                )
            }

            // Right: Child Profile Pill + Language Toggle + Parental Lock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Child Profile Switcher
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = NavCream,
                    border = BorderStroke(1.dp, NavBorder),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.05f,
                        onClick = onSwitchProfileClick
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NavCoralSoft)
                        ) {
                            Text(text = childAvatar, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = childName,
                            fontFamily = TitleFont(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NavInkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "▾", fontSize = 11.sp, color = NavInkMuted)
                    }
                }

                // Bilingual Toggle
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = NavCream,
                    border = BorderStroke(1.dp, NavBorder),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.05f,
                        onClick = onLanguageToggleClick
                    )
                ) {
                    Text(
                        text = if (AppStrings.isEnglish) "🇬🇧 EN" else "🇪🇸 ES",
                        fontFamily = TitleFont(),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavInkDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Parental Lock
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = NavCream,
                    border = BorderStroke(1.dp, NavBorder),
                    modifier = Modifier.tvFocusable(
                        shape = RoundedCornerShape(50.dp),
                        focusedScale = 1.05f,
                        onClick = onParentsClick
                    )
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvNavTabPill(
    icon: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isSelected) NavCoral else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, NavBorder),
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = Modifier.tvFocusable(
            shape = RoundedCornerShape(50.dp),
            focusedScale = 1.06f,
            onClick = onClick
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(text = icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = TitleFont(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else NavInkDark
            )
        }
    }
}
