package xyz.sevive.arcaeaoffline.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomComboBox(
    options: List<Pair<String, Any>>,
    onSelectChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {},
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = { Icon(Icons.Default.ArrowDropDown, null) }
) {
    val labels = options.map { it.first }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    var expanded by remember { mutableStateOf(false) }
    val trailingIconRotateDegree by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "trailingIconRotateDegree"
    )

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier
    ) {
        TextField(
            readOnly = true,
            value = if (selectedIndex > -1) labels[selectedIndex] else "",
            onValueChange = { },
            label = { label() },
            leadingIcon = { leadingIcon() },
            trailingIcon = { Box(Modifier.rotate(trailingIconRotateDegree)) { trailingIcon() } },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.indices.forEach { i ->
                DropdownMenuItem(
                    onClick = {
                        selectedIndex = i
                        onSelectChanged(i)
                        expanded = false
                    },
                    text = { Text(text = labels[i]) },
                )
            }
        }
    }
}
