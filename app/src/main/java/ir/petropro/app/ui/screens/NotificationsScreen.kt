package ir.petropro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.petropro.app.ui.NotificationsViewModel
import ir.petropro.app.ui.ViewModelFactory
import ir.petropro.app.ui.components.EmptyState
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.Pill
import ir.petropro.app.ui.components.SecondaryButton
import ir.petropro.app.ui.theme.PetroColors

@Composable
fun NotificationsScreen(factory: ViewModelFactory, onRoute: (String?) -> Unit) {
    val vm: NotificationsViewModel = viewModel(factory = factory)
    val items by vm.items.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh { } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("تازه‌سازی", onClick = { vm.refresh { } }, modifier = Modifier.weight(1f))
                if (items.any { !it.isRead }) SecondaryButton("همه را خوانده‌شده کن", onClick = { vm.markAllRead() }, modifier = Modifier.weight(1f))
            }
        }
        if (items.isEmpty()) item { EmptyState("اعلانی ندارید") }
        items(items, key = { it.id }) { n ->
            PetroCard(
                onClick = {
                    if (!n.isRead) vm.markRead(n.id)
                    onRoute(n.route)
                },
                padding = 14,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(n.title, fontWeight = FontWeight.Bold, color = PetroColors.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (!n.isRead) Pill("جدید", PetroColors.Yellow)
                }
                if (!n.body.isNullOrBlank()) Text(n.body, color = PetroColors.TextDim, fontSize = 13.sp)
                Text(n.createdAt.replace('T', ' ').take(16), color = PetroColors.TextMute, fontSize = 11.sp)
            }
        }
    }
}
