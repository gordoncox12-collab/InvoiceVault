package com.gordoncox.invoicevault.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.AppSettingsEntity
import com.gordoncox.invoicevault.data.entity.BusinessEntity
import com.gordoncox.invoicevault.data.entity.InvoiceStatus
import com.gordoncox.invoicevault.data.entity.ThemeMode
import com.gordoncox.invoicevault.data.repo.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repo: VaultRepository) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity(activeBusinessId = null))
    val businesses = repo.businesses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeBusiness = combine(settings, businesses) { s, list ->
        list.firstOrNull { it.id == s.activeBusinessId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectBusiness(id: String) = viewModelScope.launch { repo.saveSettings(businessId = id) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.saveSettings(themeMode = mode) }
    fun setAccent(palette: AccentPalette) = viewModelScope.launch { repo.saveSettings(accent = palette) }

    fun invoices(businessId: String) = repo.invoices(businessId)
    fun customers(businessId: String) = repo.customers(businessId)
    fun transactions(businessId: String) = repo.transactions(businessId)

    fun outstanding(invoices: List<com.gordoncox.invoicevault.data.entity.InvoiceEntity>): Double =
        invoices.filter { it.status == InvoiceStatus.SENT.name || it.status == InvoiceStatus.OVERDUE.name }
            .sumOf { it.total }

    val repository: VaultRepository get() = repo
}
