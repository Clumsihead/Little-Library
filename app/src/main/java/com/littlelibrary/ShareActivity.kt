package com.littlelibrary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** The screen that pops up when you tap Share in Instagram / YouTube and pick "Little Library". */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdgeLight()

        val shared = ShareParser.parse(intent)
        if (shared == null) {
            Toast.makeText(this, "That doesn't look like a link I can shelve", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = LibraryDb.get(this)
        setContent {
            LibraryTheme {
                ShareSheet(
                    shared = shared,
                    db = db,
                    onDone = { message ->
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareSheet(
    shared: SharedContent,
    db: LibraryDb,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(shared.title) }
    var input by remember { mutableStateOf("") }
    val chosen = remember { mutableStateListOf<String>() }
    var allTags by remember { mutableStateOf<List<TagCount>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        allTags = db.allTags()
        runCatching { focus.requestFocus() }
    }

    fun addTag(raw: String) {
        val t = TagUtils.normalize(raw)
        if (t.isNotEmpty() && t !in chosen) chosen.add(t)
        input = ""
    }

    fun save() {
        if (saving) return
        saving = true
        val pending = TagUtils.normalize(input)
        if (pending.isNotEmpty() && pending !in chosen) chosen.add(pending)
        val tags = chosen.toList()
        scope.launch {
            val result = db.save(shared.url, title.trim(), shared.source, tags)
            val message = when {
                result == SaveResult.UPDATED -> "Already in your library — shelves updated"
                tags.isEmpty() -> "Tucked away in your library"
                else -> "Shelved under " + tags.joinToString(", ") { "#$it" }
            }
            onDone(message)
        }
    }

    val normalized = TagUtils.normalize(input)
    val suggestions = TagUtils.suggest(input, allTags, chosen.toSet())
    val exactExists = normalized.isNotEmpty() &&
        (normalized in chosen || allTags.any { it.name == normalized })

    val noRipple = remember { MutableInteractionSource() }
    val noRipple2 = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = noRipple, indication = null) { onDismiss() }
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Cozy.Paper)
                // swallow taps so touching the sheet doesn't dismiss it
                .clickable(interactionSource = noRipple2, indication = null) { }
                .navigationBarsPadding(),
        ) {
            ShelfPlank()
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Shelve this",
                        color = Cozy.Walnut,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    SourceStamp(shared.source)
                }

                Text(
                    shared.url.removePrefix("https://").removePrefix("http://").removePrefix("www."),
                    color = Cozy.InkSoft,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                CozyTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Give it a name (optional)",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Text(
                    "Which shelf does it belong on?",
                    color = Cozy.Walnut,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )

                if (chosen.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        chosen.toList().forEach { tag ->
                            SpineChip(
                                label = "$tag  ✕",
                                color = Cozy.spineColor(tag),
                                selected = true,
                                onClick = { chosen.remove(tag) },
                            )
                        }
                    }
                }

                CozyTextField(
                    value = input,
                    onValueChange = { v ->
                        if (v.endsWith(",") || v.endsWith("\n")) addTag(v.dropLast(1)) else input = v
                    },
                    placeholder = "Type a tag, e.g. recipes",
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (normalized.isNotEmpty()) addTag(input) else save()
                    }),
                )

                if (suggestions.isNotEmpty() || (normalized.isNotEmpty() && !exactExists)) {
                    Text(
                        if (normalized.isEmpty()) "From your shelves" else "Already on your shelves",
                        color = Cozy.InkSoft,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        suggestions.forEach { s ->
                            SpineChip(
                                label = s.name,
                                color = Cozy.spineColor(s.name),
                                count = s.count,
                                onClick = { addTag(s.name) },
                            )
                        }
                        if (normalized.isNotEmpty() && !exactExists) {
                            SpineChip(
                                label = "＋ new shelf “$normalized”",
                                color = Cozy.WalnutLight,
                                onClick = { addTag(normalized) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = Cozy.InkSoft, fontFamily = FontFamily.Serif, fontSize = 15.sp)
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { save() },
                    enabled = !saving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cozy.Burgundy,
                        contentColor = Cozy.Card,
                    ),
                ) {
                    Text(
                        "Place on the shelf",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
