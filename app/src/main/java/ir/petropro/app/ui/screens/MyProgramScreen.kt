package ir.petropro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.petropro.app.ui.Navigator
import ir.petropro.app.ui.ProgramViewModel
import ir.petropro.app.ui.Screen
import ir.petropro.app.ui.ViewModelFactory
import ir.petropro.app.ui.components.EmptyState
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.theme.PetroColors

@Composable
fun MyProgramScreen(factory: ViewModelFactory, navigator: Navigator) {
    val vm: ProgramViewModel = viewModel(factory = factory)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val program = ui.program
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (program == null) {
            item { EmptyState("هنوز برنامه‌ای برای شما ثبت نشده است. پس از اتصال به اینترنت، همگام‌سازی کنید.") }
        } else {
            item {
                PetroCard {
                    Text(program.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PetroColors.Brand)
                    if (!program.notes.isNullOrBlank()) Text(program.notes, color = PetroColors.TextDim, fontSize = 13.sp)
                }
            }
            items(ui.days, key = { it.day.id }) { row ->
                PetroCard(onClick = { navigator.push(Screen.Workout(row.day.id)) }) {
                    Text(row.day.dayName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PetroColors.Text)
                    Text("${row.itemCount} حرکت", color = PetroColors.TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}
