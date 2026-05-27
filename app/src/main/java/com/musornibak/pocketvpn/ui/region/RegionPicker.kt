package com.musornibak.pocketvpn.ui.region

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions

@Composable
fun RegionPicker(
    selected: Region,
    onDismiss: () -> Unit,
    onPick: (Region) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val filteredCountries = remember(query) {
        if (query.isBlank()) Regions.COUNTRIES
        else Regions.COUNTRIES.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Text(
                            "Choose region",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                    }

                    SearchField(query, onChange = { query = it })

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (query.isBlank()) {
                            item {
                                SectionHeader("Presets")
                            }
                            items(Regions.PRESETS, key = { it.code }) { r ->
                                RegionRow(r, r.code == selected.code) { onPick(r) }
                            }
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                                SectionHeader("All countries")
                            }
                        }
                        items(filteredCountries, key = { it.code }) { r ->
                            RegionRow(r, r.code == selected.code) { onPick(r) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search country…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun RegionRow(region: Region, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(region.flag, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                region.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            val sub = when {
                region.isPreset && region.code == "auto" -> "Random fast exit"
                region.isPreset && region.code == "fast_eu" -> "DE · NL · SE"
                region.isPreset && region.code == "avoid_14" -> "Skip 14-eyes countries"
                region.approxRelays > 0 -> "~${region.approxRelays} relays"
                else -> ""
            }
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        if (region.isFast) {
            Text("⚡", fontSize = 14.sp)
        }
        if (selected) {
            Icon(
                Icons.Default.Check, null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
