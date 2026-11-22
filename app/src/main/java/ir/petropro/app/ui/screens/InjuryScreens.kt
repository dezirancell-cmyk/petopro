package ir.petropro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import ir.petropro.app.core.AppContainer
import ir.petropro.app.data.InjuryConversation
import ir.petropro.app.data.Loaded
import ir.petropro.app.domain.RoleTabs
import ir.petropro.app.network.InjuryMessageDto
import ir.petropro.app.network.InjuryReportDto
import ir.petropro.app.ui.components.EmptyState
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.Pill
import ir.petropro.app.ui.components.PrimaryButton
import ir.petropro.app.ui.components.SecondaryButton
import ir.petropro.app.ui.components.petroFieldColors
import ir.petropro.app.ui.theme.PetroColors
import kotlinx.coroutines.launch

private const val ONLINE_ONLY = "این بخش به اینترنت نیاز دارد. اتصال را بررسی و دوباره تلاش کنید."

@Composable
fun InjuryListScreen(container: AppContainer, role: String?, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<Loaded<List<InjuryReportDto>>?>(null) }
    var showNew by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { state = container.injuryRepository.list() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (RoleTabs.isAthleteRole(role)) PrimaryButton("گزارش جدید", onClick = { showNew = true }, modifier = Modifier.weight(1f))
                SecondaryButton("تازه‌سازی", onClick = { tick++ }, modifier = Modifier.weight(1f))
            }
        }
        when (val s = state) {
            null -> item { EmptyState("در حال دریافت…") }
            is Loaded.Offline -> item { EmptyState(ONLINE_ONLY) }
            is Loaded.Failure -> item { EmptyState(s.message) }
            is Loaded.Data -> if (s.value.isEmpty()) item { EmptyState("گزارشی ثبت نشده است") } else items(s.value, key = { it.id }) { r ->
                PetroCard(onClick = { onOpen(r.id) }, padding = 14) {
                    Text(
                        listOfNotNull(r.athleteName, r.exerciseName).joinToString(" — ").ifEmpty { "گزارش آسیب‌دیدگی" },
                        fontWeight = FontWeight.Bold, color = PetroColors.Text, fontSize = 16.sp,
                    )
                    Text(r.description, color = PetroColors.TextDim, fontSize = 13.sp, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("وضعیت: ${r.statusFa}", PetroColors.Brand)
                        if (r.unread) Pill("جدید", PetroColors.Yellow)
                    }
                }
            }
        }
    }

    if (showNew) {
        var exercise by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            containerColor = PetroColors.Panel,
            title = { Text("گزارش آسیب‌دیدگی", color = PetroColors.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(exercise, { if (it.length <= 200) exercise = it }, label = { Text("نام حرکت (اختیاری)") }, colors = petroFieldColors(), singleLine = true)
                    OutlinedTextField(description, { if (it.length <= 1000) description = it }, label = { Text("توضیح مشکل") }, colors = petroFieldColors(), minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (description.isBlank()) {
                        Toast.makeText(context, "متن را وارد کنید", Toast.LENGTH_SHORT).show()
                    } else {
                        showNew = false
                        scope.launch {
                            when (val r = container.injuryRepository.create(exercise, description.trim())) {
                                is Loaded.Data -> { Toast.makeText(context, "ارسال شد", Toast.LENGTH_SHORT).show(); tick++ }
                                is Loaded.Offline -> Toast.makeText(context, ONLINE_ONLY, Toast.LENGTH_LONG).show()
                                is Loaded.Failure -> Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) { Text("ارسال", color = PetroColors.Brand) }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("انصراف", color = PetroColors.TextDim) } },
        )
    }
}

@Composable
fun InjuryDetailScreen(container: AppContainer, reportId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myId = container.sessionStore.ownerUserId
    var tick by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<Loaded<InjuryConversation>?>(null) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(tick) {
        state = container.injuryRepository.conversation(reportId)
        val data = state
        if (data is Loaded.Data && data.value.messages.isNotEmpty()) listState.scrollToItem(data.value.messages.size)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val s = state) {
                null -> item { EmptyState("در حال دریافت…") }
                is Loaded.Offline -> item { EmptyState(ONLINE_ONLY) }
                is Loaded.Failure -> item { EmptyState(s.message) }
                is Loaded.Data -> {
                    s.value.report?.let { rep ->
                        item {
                            PetroCard(padding = 14) {
                                if (!rep.exerciseName.isNullOrBlank()) Text(rep.exerciseName, fontWeight = FontWeight.Bold, color = PetroColors.Text)
                                Text(rep.description, color = PetroColors.TextDim, fontSize = 13.sp)
                                Pill("وضعیت: ${rep.statusFa}", PetroColors.Brand)
                            }
                        }
                    }
                    items(s.value.messages, key = { it.id }) { m -> MessageBubble(m, mine = m.senderId == myId) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                input, { if (it.length <= 1000) input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("پیام شما…") }, colors = petroFieldColors(), maxLines = 4,
            )
            PrimaryButton("ارسال", enabled = !sending, onClick = {
                val text = input.trim()
                if (text.isEmpty()) {
                    Toast.makeText(context, "متن را وارد کنید", Toast.LENGTH_SHORT).show()
                } else {
                    sending = true
                    scope.launch {
                        when (val r = container.injuryRepository.send(reportId, text)) {
                            is Loaded.Data -> { input = ""; tick++ }
                            is Loaded.Offline -> Toast.makeText(context, ONLINE_ONLY, Toast.LENGTH_LONG).show()
                            is Loaded.Failure -> Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                        }
                        sending = false
                    }
                }
            })
        }
    }
}

@Composable
private fun MessageBubble(m: InjuryMessageDto, mine: Boolean) {
    PetroCard(padding = 12) {
        Text(if (mine) "شما" else m.senderName.orEmpty(), color = if (mine) PetroColors.Brand else PetroColors.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(m.message, color = PetroColors.Text, fontSize = 14.sp)
        Text(m.createdAt.replace('T', ' ').take(16), color = PetroColors.TextMute, fontSize = 11.sp)
    }
}
