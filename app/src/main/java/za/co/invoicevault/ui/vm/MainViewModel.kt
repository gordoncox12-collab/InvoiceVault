package za.co.invoicevault.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.invoicevault.InvoiceVaultApp
import za.co.invoicevault.data.db.BusinessEntity
import za.co.invoicevault.domain.AccentPalette
import za.co.invoicevault.domain.ThemeMode

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as InvoiceVaultApp).container.repository

    val settings = repo.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), za.co.invoicevault.data.prefs.UserSettings())
    val businesses = repo.businesses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = repo.templates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedBusinessId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers = selectedBusinessId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.customers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val invoices = selectedBusinessId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.invoices(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repo.bootstrap()
            val current = repo.currentBusiness()
            if (selectedBusinessId.value == null) selectedBusinessId.value = current?.id
        }
    }

    fun selectBusiness(business: BusinessEntity) {
        selectedBusinessId.value = business.id
        viewModelScope.launch { repo.settings.setDefaultBusiness(business.id) }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.settings.setTheme(mode) }
    fun setAccent(palette: AccentPalette) = viewModelScope.launch { repo.settings.setAccent(palette) }

    fun repository() = repo
}
