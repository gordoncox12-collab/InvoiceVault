package com.gordoncox.invoicevault

import android.app.Application
import com.gordoncox.invoicevault.di.AppContainer

class InvoiceVaultApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
