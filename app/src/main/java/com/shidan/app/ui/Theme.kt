package com.shidan.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 配色照搬网页版：纸一样的米底、松针绿、梅子红。
 * 松针绿是主色（按钮、我的分数），梅子红只留给"删除"和分歧提示，别处不出现。
 */

private val PaperLight = Color(0xFFEFEDE5)
private val CardLight = Color(0xFFFAF9F4)
private val Card2Light = Color(0xFFF3F1E9)
private val InkLight = Color(0xFF1B2420)
private val InkSoftLight = Color(0xFF5E665F)
private val LineLight = Color(0xFFDBD7CB)
private val PineLight = Color(0xFF2E5E4E)
private val PlumLight = Color(0xFF8C2F39)
private val GoldLight = Color(0xFF8A6614)

private val PaperDark = Color(0xFF121715)
private val CardDark = Color(0xFF1B211E)
private val Card2Dark = Color(0xFF232A26)
private val InkDark = Color(0xFFE9E6DC)
private val InkSoftDark = Color(0xFFA0A79E)
private val LineDark = Color(0xFF2D3531)
private val PineDark = Color(0xFF77C0A2)
private val PlumDark = Color(0xFFDC8189)
private val GoldDark = Color(0xFFD9B45B)

/** Material 的色板装不下"第二层卡片背景""分割线"这些，单开一个 */
data class Palette(
    val paper: Color,
    val card: Color,
    val card2: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val pine: Color,
    val plum: Color,
    val gold: Color,
    val onAccent: Color
)

val LocalPalette = staticCompositionLocalOf {
    Palette(
        PaperLight, CardLight, Card2Light, InkLight, InkSoftLight,
        LineLight, PineLight, PlumLight, GoldLight, PaperLight
    )
}

@Composable
fun ShidanTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (dark) {
        Palette(
            paper = PaperDark, card = CardDark, card2 = Card2Dark,
            ink = InkDark, inkSoft = InkSoftDark, line = LineDark,
            pine = PineDark, plum = PlumDark, gold = GoldDark,
            onAccent = PaperDark
        )
    } else {
        Palette(
            paper = PaperLight, card = CardLight, card2 = Card2Light,
            ink = InkLight, inkSoft = InkSoftLight, line = LineLight,
            pine = PineLight, plum = PlumLight, gold = GoldLight,
            onAccent = PaperLight
        )
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.pine,
            onPrimary = palette.onAccent,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.card2,
            onSurfaceVariant = palette.inkSoft,
            error = palette.plum,
            outline = palette.line
        )
    } else {
        lightColorScheme(
            primary = palette.pine,
            onPrimary = palette.onAccent,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.card2,
            onSurfaceVariant = palette.inkSoft,
            error = palette.plum,
            outline = palette.line
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
