package com.aeladrin.currencyexchanger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aeladrin.currencyexchanger.R
import com.aeladrin.currencyexchanger.ui.theme.AppColor
import com.aeladrin.currencyexchanger.ui.theme.CurrencyExchangerTheme
import com.aeladrin.currencyexchanger.ui.utils.MoneyVisualTransformation
import com.aeladrin.currencyexchanger.utils.MoneyFormat

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CurrencyExchangerPage(
    viewModel: CurrencyExchangerViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ), // I use trailing commas, so it's easier to review changes
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text(
                text = stringResource(R.string.my_balances),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(16.dp),
            )
            BalancesRow(viewState.balances)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.currency_exchange),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(16.dp),
            )
            SellRow(
                currencies = viewState.sellCurrencies,
                onAmountChange = viewModel::onSellAmountChange,
                onCurrencyChange = viewModel::onSellCurrencyChange,
            )
            Divider(
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
            ReceiveRow(
                amount = viewState.receiveAmount,
                currencies = viewState.receiveCurrencies,
                onCurrencyChange = viewModel::onReceiveCurrencyChange,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = viewModel::onSubmitClick,
                modifier = Modifier
                    .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                    .fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.submit))
            }
        }
    }
}

@Composable
private fun BalancesRow(balances: Map<String, Double>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // it's scrollable, in case we have a lot of currencies and not enough space
            .horizontalScroll(rememberScrollState()),
        // distribute space between elements if the row isn't scrollable
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        balances.map {
            Text(
                text = "${MoneyFormat.format(it.value)} ${it.key}",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SellRow(
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.round_arrow_upward_24),
            contentDescription = stringResource(id = R.string.sell),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .drawBehind { drawCircle(color = AppColor.Red) }
                .padding(8.dp),
        )
        Text(text = stringResource(id = R.string.sell))
        Spacer(modifier = Modifier.weight(1f))

        // compose docs suggest to use remembered string for TextField
        // instead of state flow to avoid synchronization bugs
        var sellValue by rememberSaveable { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        // using basic text field to better handle width and skip decorations
        BasicTextField(
            value = sellValue,
            onValueChange = {
                // skip leading zeros input, required for MoneyVisualTransformation
                sellValue = if (it.startsWith("0")) "" else it
                onAmountChange(sellValue.toDoubleOrNull() ?: 0.0)
            },
            visualTransformation = MoneyVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            // Done button isn't removing cursor, do it manually
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            // by default text field is taking a lot of space
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(max = 150.dp),
        )

        CurrencyDropdown(
            values = currencies,
            onValueChange = onCurrencyChange,
        )
    }
}

// could reuse SellRow, but we don't need TextField here, let's KISS
@Composable
private fun ReceiveRow(
    amount: Double,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.round_arrow_downward_24),
            contentDescription = stringResource(id = R.string.receive),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .drawBehind { drawCircle(color = AppColor.Green) }
                .padding(8.dp),
        )
        Text(text = stringResource(id = R.string.receive))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(color = AppColor.Green),
        )
        CurrencyDropdown(
            values = currencies,
            onValueChange = onCurrencyChange,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CurrencyDropdown(
    values: List<String>,
    onValueChange: (String) -> Unit,
) {
    // I'm not repeating selected item in dropdown
    val selected = values.firstOrNull()
    val otherValues = values.drop(1)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        // TextField takes a lot of space by default, so we need to limit it
        modifier = Modifier.width(110.dp),
    ) {
        // hope they'll change this API, but for now we have to use read-only TextField here
        TextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.menuAnchor(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                // have to use a constraint, because ExpandedDropdownMenu is taking too much space
                .exposedDropdownSize()
        ) {
            otherValues.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(text = currency) },
                    onClick = {
                        onValueChange(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CurrencyExchangerTheme {
        CurrencyExchangerPage()
    }
}