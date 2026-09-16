package za.co.invoicevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import za.co.invoicevault.data.db.BusinessEntity
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.ui.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    vm: MainViewModel,
    onEdit: (Long, Long) -> Unit,
    onFolder: (Long) -> Unit,
    onNew: (Long) -> Unit
) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedBusinessId.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Customers") }) },
        floatingActionButton = {
            selectedId?.let { FloatingActionButton(onClick = { onNew(it) }) { Icon(Icons.Default.Add, null) } }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(customers, key = { it.id }) { customer ->
                ListItem(
                    headlineContent = { Text(customer.name) },
                    supportingContent = { Text(listOf(customer.city, customer.email).filter { it.isNotBlank() }.joinToString(" · ")) },
                    trailingContent = {
                        IconButton(onClick = { onFolder(customer.id) }) { Icon(Icons.Default.Folder, contentDescription = "Folder") }
                    },
                    modifier = Modifier.clickable { onEdit(customer.id, customer.businessId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditScreen(
    vm: MainViewModel,
    customerId: Long,
    businessId: Long,
    onDone: () -> Unit
) {
    val repo = vm.repository()
    var customer by remember { mutableStateOf(CustomerEntity(businessId = businessId, name = "")) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(customerId) {
        if (customerId != 0L) repo.customer(customerId)?.let { customer = it }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customerId == 0L) "New customer" else "Edit customer") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(customer.name, { customer = customer.copy(name = it) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.contactPerson, { customer = customer.copy(contactPerson = it) }, label = { Text("Contact person") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.email, { customer = customer.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.phone, { customer = customer.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.addressLine1, { customer = customer.copy(addressLine1 = it) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.city, { customer = customer.copy(city = it) }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.province, { customer = customer.copy(province = it) }, label = { Text("Province") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.postalCode, { customer = customer.copy(postalCode = it) }, label = { Text("Postal code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.vatNumber, { customer = customer.copy(vatNumber = it) }, label = { Text("VAT number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customer.notes, { customer = customer.copy(notes = it) }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { repo.saveCustomer(customer); onDone() } }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessEditScreen(
    vm: MainViewModel,
    businessId: Long,
    onDone: () -> Unit
) {
    val repo = vm.repository()
    var business by remember {
        mutableStateOf(
            BusinessEntity(
                name = "",
                city = "Cape Town",
                province = "Western Cape",
                country = "South Africa",
                defaultCurrency = "ZAR"
            )
        )
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(businessId) {
        if (businessId != 0L) repo.business(businessId)?.let { business = it }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (businessId == 0L) "New business" else "Business profile") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(business.name, { business = business.copy(name = it) }, label = { Text("Legal name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.tradingName, { business = business.copy(tradingName = it) }, label = { Text("Trading name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.vatNumber, { business = business.copy(vatNumber = it) }, label = { Text("VAT number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.registrationNumber, { business = business.copy(registrationNumber = it) }, label = { Text("CIPC number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.email, { business = business.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.phone, { business = business.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.addressLine1, { business = business.copy(addressLine1 = it) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.city, { business = business.copy(city = it) }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.province, { business = business.copy(province = it) }, label = { Text("Province") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.postalCode, { business = business.copy(postalCode = it) }, label = { Text("Postal code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.bankName, { business = business.copy(bankName = it) }, label = { Text("Bank") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.accountName, { business = business.copy(accountName = it) }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.accountNumber, { business = business.copy(accountNumber = it) }, label = { Text("Account number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.branchCode, { business = business.copy(branchCode = it) }, label = { Text("Branch code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.invoicePrefix, { business = business.copy(invoicePrefix = it) }, label = { Text("Invoice prefix") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(business.defaultCurrency, { business = business.copy(defaultCurrency = it) }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    val id = repo.saveBusiness(business)
                    vm.selectBusiness(business.copy(id = id))
                    onDone()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessListScreen(vm: MainViewModel, onEdit: (Long) -> Unit, onNew: () -> Unit) {
    val businesses by vm.businesses.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Businesses") }) },
        floatingActionButton = { FloatingActionButton(onClick = onNew) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(8.dp)) {
            items(businesses, key = { it.id }) { business ->
                ListItem(
                    headlineContent = { Text(business.displayName) },
                    supportingContent = { Text("${business.city} · ${business.vatNumber}") },
                    modifier = Modifier.clickable { onEdit(business.id) }
                )
            }
        }
    }
}
