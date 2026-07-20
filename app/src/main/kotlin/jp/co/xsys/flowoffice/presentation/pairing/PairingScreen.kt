package jp.co.xsys.flowoffice.presentation.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme

private const val PAIRING_FORM_MAX_WIDTH_DP = 560

@Composable
fun PairingScreen(
    state: PairingUiState,
    onApiBaseUrlChange: (String) -> Unit,
    onClaimTokenChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onPair: () -> Unit,
    allowApiUrlEditing: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSubmitting = state.submission is PairingSubmissionState.Submitting

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = PAIRING_FORM_MAX_WIDTH_DP.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.pairing_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.pairing_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = onScanQr,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("pairing-scan"),
                ) {
                    Text(stringResource(R.string.pairing_scan_qr))
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.pairing_manual_heading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.pairing_manual_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.claimUrl,
                    onValueChange = onApiBaseUrlChange,
                    label = { Text(stringResource(R.string.activation_url)) },
                    enabled = allowApiUrlEditing && !isSubmitting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.claimToken,
                    onValueChange = onClaimTokenChange,
                    label = { Text(stringResource(R.string.pairing_claim_token)) },
                    supportingText = { Text(stringResource(R.string.pairing_claim_token_support)) },
                    enabled = !isSubmitting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.submission is PairingSubmissionState.Error) {
                    PairingError(messageResId = state.submission.messageResId)
                }
                if (state.submission is PairingSubmissionState.Success) {
                    PairingSuccess()
                }

                OutlinedButton(
                    onClick = onPair,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("pairing-submit"),
                ) {
                    if (isSubmitting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(stringResource(R.string.pairing_in_progress))
                        }
                    } else {
                        Text(stringResource(R.string.pairing_manual_action))
                    }
                }

                Text(
                    text = stringResource(R.string.pairing_admin_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PairingError(messageResId: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.pairing_error_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PairingSuccess() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.pairing_success_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.pairing_success_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Phone - Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PairingScreenPhonePreview() {
    FlowOfficeReaderTheme {
        PairingScreenPreviewContent()
    }
}

@Preview(name = "Tablet - Dark", showBackground = true, widthDp = 800, heightDp = 1000)
@Composable
private fun PairingScreenTabletDarkPreview() {
    FlowOfficeReaderTheme(darkTheme = true) {
        PairingScreenPreviewContent(
            state = PairingUiState(
                claimUrl = "https://office.example.jp/api/",
                submission = PairingSubmissionState.Error(
                    R.string.error_pairing_unauthorized,
                ),
            ),
        )
    }
}

@Composable
private fun PairingScreenPreviewContent(
    state: PairingUiState = PairingUiState(),
) {
    PairingScreen(
        state = state,
        onApiBaseUrlChange = {},
        onClaimTokenChange = {},
        onScanQr = {},
        onPair = {},
        allowApiUrlEditing = true,
    )
}
