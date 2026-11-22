package com.littlelibrary

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdgeLight()
        val db = LibraryDb.get(this)
        setContent { LibraryTheme { LibraryScreen(db) } }
    }
}

fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LibraryScreen(db: LibraryDb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<SavedItem>>(emptyList()) }
    var shelves by remember { mutableStateOf<List<TagCount>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<SavedItem?>(null) }

    fun refresh() {
        scope.launch {
            val tags = db.allTags().filter { it.count > 0 }
            // If the open shelf has just been emptied, step back to "All".
            val shelf = selected?.takeIf { s -> tags.any { it.name == s } }
            items = db.items(shelf)
            shelves = tags
            totalCount = db.itemCount()
            if (shelf != selected) selected = shelf
        }
    }

    // Reloads whenever you come back to the app (e.g. after sharing something) or switch shelf.
    LifecycleResumeEffect(selected) {
        refresh()
        onPauseOrDispose { }
    }

    val visible = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            items
        } else {
            items.filter { i ->
                i.title.lowercase().contains(q) ||
                    i.url.lowercase().contains(q) ||
                    i.source.lowercase().contains(q) ||
                    i.tags.any { it.contains(q) }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Cozy.Paper),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
                    Text(
                        "The Little Library",
                        color = Cozy.Walnut,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    )
                    Text(
                        text = if (totalCount == 0) {
                            "A quiet corner for the things you want to come back to."
                        } else {
                            "$totalCount keepsakes on ${shelves.size} shelves"
                        },
                        color = Cozy.InkSoft,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item {
                CozyTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search the catalogue…",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                )
            }

            item {
                Column(Modifier.padding(top = 22.dp)) {
                    // The books stand upright against a recessed back panel, resting on the plank.
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Cozy.PaperDeep.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        item {
                            BookSpine(
                                label = "All",
                                color = Cozy.Walnut,
                                count = totalCount,
                                selected = selected == null,
                                width = 56.dp,
                                height = 150.dp,
                            ) { selected = null }
                        }
                        items(shelves, key = { it.name }) { tag ->
                            val isSelected = selected == tag.name
                            BookSpine(
                                label = tag.name,
                                color = Cozy.spineColor(tag.name),
                                count = tag.count,
                                selected = isSelected,
                            ) { selected = if (isSelected) null else tag.name }
                        }
                    }
                    ShelfPlank()
                }
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = if (selected == null) {
                            "The shelves are still bare.\n\nOpen Instagram or YouTube, tap Share on anything you like, and choose “Little Library” to place your first book here."
                        } else {
                            "Nothing on this shelf yet."
                        },
                        color = Cozy.InkSoft,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    )
                }
            } else if (visible.isEmpty()) {
                item {
                    Text(
                        "Nothing in the catalogue matches “${query.trim()}”.",
                        color = Cozy.InkSoft,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    )
                }
            } else {
                item {
                    Text(
                        text = if (selected == null) "Recently shelved" else "On the “$selected” shelf",
                        color = Cozy.Walnut,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                    )
                }
                items(visible, key = { it.id }) { item ->
                    IndexCard(
                        item = item,
                        onOpen = { openLink(context, item.url) },
                        onDelete = { pendingDelete = item },
                        onTagClick = { selected = it },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Cozy.Card,
            title = {
                Text("Take it off the shelf?", color = Cozy.Walnut, fontStyle = FontStyle.Italic)
            },
            text = {
                Text(
                    item.title.ifBlank { item.url },
                    color = Cozy.Ink,
                    maxLines = 3,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        db.delete(item.id)
                        refresh()
                    }
                }) { Text("Remove", color = Cozy.Burgundy) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep it", color = Cozy.InkSoft) }
            },
        )
    }
}
