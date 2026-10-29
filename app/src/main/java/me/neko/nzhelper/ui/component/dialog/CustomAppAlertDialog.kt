package me.neko.nzhelper.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CustomAppAlertDialog(
    onDismissRequest: () -> Unit,
    iconVector: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    confirmIcon: ImageVector,
    dismissText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { onDismissRequest() },
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = confirmIcon,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = confirmText
                )
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = dismissText
                    )
                }
                val tertiaryAction = onTertiary
                if (tertiaryText != null && tertiaryAction != null) {
                    TextButton(
                        onClick = {
                            tertiaryAction()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = tertiaryText
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}