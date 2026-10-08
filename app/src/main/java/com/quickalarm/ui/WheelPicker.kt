package com.quickalarm.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A minimal snapping wheel picker. Shows [visibleCount] rows with the selected
 * value centered. Reports the selected value once the list settles on it.
 */
@Composable
fun WheelPicker(
    values: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: androidx.compose.ui.unit.Dp = 48.dp,
    visibleCount: Int = 3,
) {
    val half = visibleCount / 2
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // The centered item index equals the first visible item index because we pad
    // the list with `half` blank rows at the top.
    val centeredIndex = remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex).coerceIn(0, (values.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { !listState.isScrollInProgress to centeredIndex.value }
            .distinctUntilChanged()
            .collect { (settled, index) ->
                if (settled && index in values.indices) {
                    onValueChange(values[index])
                }
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Leading blank rows so the first real value can center.
            items(count = half, key = { "lead_$it" }) {
                Box(Modifier.height(itemHeight).fillMaxWidth())
            }
            items(items = values, key = { "v_$it" }) { value ->
                val isSelected = value == values.getOrNull(centeredIndex.value)
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'),
                        textAlign = TextAlign.Center,
                        style = if (isSelected) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            // Trailing blank rows so the last real value can center.
            items(count = half, key = { "trail_$it" }) {
                Box(Modifier.height(itemHeight).fillMaxWidth())
            }
        }
    }
}
