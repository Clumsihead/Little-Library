package com.littlelibrary

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ComponentActivity.edgeToEdgeLight() {
    val clear = android.graphics.Color.TRANSPARENT
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(clear, clear),
        navigationBarStyle = SystemBarStyle.light(clear, clear),
    )
}

/** A book spine seen end-on: coloured cloth, two gold bands, title in cream. Used for every tag. */
@Composable
fun SpineChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selected: Boolean = false,
    height: Dp = 36.dp,
    textSize: TextUnit = 14.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(color)
            .then(if (selected) Modifier.border(2.dp, Cozy.Gold, shape) else Modifier)
            .clickable(onClick = onClick)
            .drawBehind {
                val inset = 8.dp.toPx()
                val stroke = 1.2.dp.toPx()
                val band = Cozy.Gold.copy(alpha = 0.7f)
                drawLine(band, Offset(inset, 0f), Offset(inset, size.height), stroke)
                drawLine(band, Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), stroke)
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = Cozy.Card,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = textSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                color = Cozy.Card.copy(alpha = 0.75f),
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * A book standing upright on the shelf: the tag name runs down the spine, a count sits
 * on a little label at the foot. Longer tag names make taller books, and every tag keeps
 * its own colour and width. The open shelf is pulled out a little and edged in gold.
 */
@Composable
fun BookSpine(
    label: String,
    color: Color,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    onClick: () -> Unit,
) {
    val h = height ?: (62f + label.length * 7.5f + (label.hashCode() and 15)).coerceIn(124f, 232f).dp
    val w = width ?: (44 + ((label.hashCode() shr 4) and 3) * 4).dp
    val lift by animateDpAsState(if (selected) 16.dp else 0.dp, label = "lift")
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

    Box(modifier.padding(bottom = lift)) {
        Column(
            modifier = Modifier
                .width(w)
                .height(h)
                .clip(shape)
                .background(color)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.20f),
                        ),
                    ),
                )
                .then(if (selected) Modifier.border(2.dp, Cozy.Gold, shape) else Modifier)
                .clickable(onClick = onClick)
                .drawBehind {
                    val band = Cozy.Gold.copy(alpha = 0.8f)
                    val stroke = 1.5.dp.toPx()
                    fun line(y: Float) = drawLine(band, Offset(0f, y), Offset(size.width, y), stroke)
                    line(8.dp.toPx())
                    line(13.dp.toPx())
                    line(size.height - 34.dp.toPx())
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .requiredWidth(h - 62.dp)
                        .rotate(90f),
                    color = Cozy.Card,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    color = Cozy.Card.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun ShelfPlank(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Brush.verticalGradient(listOf(Cozy.WalnutLight, Cozy.Walnut))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent))),
        )
    }
}

/** The little red "RETURNED" style stamp library cards have. */
@Composable
fun SourceStamp(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        modifier = modifier
            .rotate(-3f)
            .border(1.5.dp, Cozy.Burgundy.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Cozy.Burgundy.copy(alpha = 0.85f),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    )
}

@Composable
fun CozyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cozy.Ink),
        cursorBrush = SolidColor(Cozy.Burgundy),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Cozy.Card)
                    .border(1.dp, Cozy.InkSoft.copy(alpha = 0.4f), shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Cozy.InkSoft,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                    )
                }
                inner()
            }
        },
    )
}

private fun prettyUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('?')

/** One saved link, drawn like a ruled library index card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndexCard(
    item: SavedItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape)
            .clip(shape)
            .background(Cozy.Card)
            .border(1.dp, Cozy.PaperDeep, shape)
            .clickable(onClick = onOpen)
            .drawBehind {
                drawLine(Cozy.Burgundy.copy(alpha = 0.35f), Offset(0f, 50.dp.toPx()), Offset(size.width, 50.dp.toPx()), 1.5.dp.toPx())
                var y = 76.dp.toPx()
                while (y < size.height - 8.dp.toPx()) {
                    drawLine(Cozy.InkSoft.copy(alpha = 0.14f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    y += 26.dp.toPx()
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceStamp(item.source)
            Spacer(Modifier.weight(1f))
            Text(
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(item.savedAt)),
                color = Cozy.InkSoft,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
            )
            Text(
                "✕",
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                color = Cozy.InkSoft,
                fontSize = 14.sp,
            )
        }
        Text(
            text = item.title.ifBlank { prettyUrl(item.url) },
            color = Cozy.Ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (item.title.isNotBlank()) {
            Text(
                text = prettyUrl(item.url),
                color = Cozy.InkSoft,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                item.tags.forEach { tag ->
                    SpineChip(
                        label = tag,
                        color = Cozy.spineColor(tag),
                        height = 28.dp,
                        textSize = 12.sp,
                        onClick = { onTagClick(tag) },
                    )
                }
            }
        }
    }
}
