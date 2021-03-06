package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import views.util.LabelledCheckBox

@Composable
@Preview
fun CheckboxEntry(entry: ResultRow, idColumn: Column<EntityID<Int>>, checkedColumn: Column<Boolean>, labelColumn: Column<String>,
                  checkedCheckboxIds: MutableList<Int>) {
    val playlistDbId = entry[idColumn].value

    Row(verticalAlignment = Alignment.CenterVertically) {
        var isIncludedInResults by remember { mutableStateOf(entry[checkedColumn]) }

        LabelledCheckBox(isIncludedInResults, label = entry[labelColumn], onCheckedChange = { checked ->
            if (checked) {
                checkedCheckboxIds.add(playlistDbId)
            } else {
                checkedCheckboxIds.remove(playlistDbId)
            }

            isIncludedInResults = checked
            entry[checkedColumn] = checked
        })
    }
}

@Composable
@Preview
fun CheckboxView(ButtonRow: @Composable (checkedEntries: MutableList<Int>) -> Unit, entries: List<ResultRow>, idColumn: Column<EntityID<Int>>,
                 checkedColumn: Column<Boolean>, labelColumn: Column<String>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val checkedEntries by remember {
            mutableStateOf(entries.filter { it[checkedColumn] }.map { it[idColumn].value }.toMutableList())
        }

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            items(entries) { playlist ->
                CheckboxEntry(playlist, idColumn, checkedColumn, labelColumn, checkedEntries)
            }
        }

        Row {
            ButtonRow(checkedEntries)
        }
    }
}
