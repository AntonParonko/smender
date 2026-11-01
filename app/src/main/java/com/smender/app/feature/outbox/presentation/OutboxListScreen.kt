package com.smender.app.feature.outbox.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smender.app.R
import com.smender.core.domain.feature.bridge.domain.model.DeliveryChannel
import com.smender.core.domain.feature.bridge.domain.model.OutboxMessage
import com.smender.core.domain.feature.bridge.domain.model.OutboxStatus
import com.smender.app.ui.theme.AppBarBackground
import com.smender.app.ui.theme.Background
import com.smender.app.ui.theme.Blue600
import com.smender.app.ui.theme.Green500
import com.smender.app.ui.theme.SurfaceCard
import com.smender.app.ui.theme.TextPrimary
import com.smender.app.ui.theme.TextSecondary
import com.smender.app.ui.theme.White
import com.smender.app.ui.components.AnchoredConfirmationMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboxListScreen(
    title: String,
    messages: List<OutboxMessage>,
    onBackClick: () -> Unit,
    onDeleteClick: (Long) -> Unit,
    onClearSectionClick: (() -> Unit)? = null,
    clearButtonDescription: String,
    clearConfirmTitle: String,
    clearConfirmMessage: String
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, color = White, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBarBackground),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = White
                        )
                    }
                },
                actions = {
                    if (onClearSectionClick != null && messages.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showClearConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = clearButtonDescription,
                                    tint = White
                                )
                            }
                            AnchoredConfirmationMenu(
                                expanded = showClearConfirmation,
                                title = clearConfirmTitle,
                                message = clearConfirmMessage,
                                confirmText = stringResource(R.string.clear),
                                dismissText = stringResource(R.string.cancel),
                                onDismissRequest = { showClearConfirmation = false },
                                onConfirm = onClearSectionClick
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.list_empty), color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (message.channel) {
                                        DeliveryChannel.TELEGRAM -> stringResource(R.string.channel_telegram)
                                        DeliveryChannel.WHATSAPP -> stringResource(R.string.channel_whatsapp)
                                    },
                                    color = Blue600,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                IconButton(onClick = { onDeleteClick(message.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete_record),
                                        tint = colorResource(R.color.red_600)
                                    )
                                }
                            }
                            Text(
                                text = when (message.status) {
                                    OutboxStatus.SENT -> stringResource(R.string.status_sent)
                                    OutboxStatus.PENDING -> stringResource(R.string.status_pending)
                                    OutboxStatus.ERROR -> stringResource(R.string.status_error)
                                },
                                color = when (message.status) {
                                    OutboxStatus.SENT -> Green500
                                    OutboxStatus.PENDING -> TextSecondary
                                    OutboxStatus.ERROR -> colorResource(R.color.red_600)
                                },
                                fontSize = 12.sp
                            )
                            Text(
                                stringResource(R.string.from_sender, message.senderAddress),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(message.messageBody, color = TextPrimary, fontSize = 13.sp)
                            val lastError = message.lastError
                            if (!lastError.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.error_with_reason, lastError),
                                    color = colorResource(R.color.red_600),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
