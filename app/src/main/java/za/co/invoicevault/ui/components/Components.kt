package za.co.invoicevault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import za.co.invoicevault.data.InvoiceStatus
import za.co.invoicevault.data.formatMoney

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

@Composable
fun IvField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines
    )
}

@Composable
fun StatusChip(status: InvoiceStatus, onClick: (() -> Unit)? = null) {
    val colors = when (status) {
        InvoiceStatus.DRAFT -> Color(0xFF6B7280) to Color(0xFFE5E7EB)
        InvoiceStatus.SENT -> Color(0xFF1D4E89) to Color(0xFFD6E8F5)
        InvoiceStatus.PAID -> Color(0xFF166534) to Color(0xFFDCFCE7)
        InvoiceStatus.OVERDUE -> Color(0xFF9A3412) to Color(0xFFFED7AA)
    }
    AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(status.name) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.second,
            labelColor = colors.first
        )
    )
}

@Composable
fun MoneyText(amount: Double, currency: String = "ZAR", modifier: Modifier = Modifier) {
    Text(
        text = formatMoney(amount, currency),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}

@Composable
fun TwoPane(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
