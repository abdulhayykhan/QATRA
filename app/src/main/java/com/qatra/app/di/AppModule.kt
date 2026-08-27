package com.qatra.app.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import com.qatra.app.data.repository.AuthRepository
import com.qatra.app.data.repository.SeekerRepository
import com.qatra.app.data.repository.DonorRepository
import com.qatra.app.data.repository.AdminRepository
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.ui.SharedAuthViewModel
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.seeker.SeekerViewModel
import com.qatra.app.ui.donor.DonorViewModel
import com.qatra.app.ui.admin.AdminViewModel
import com.qatra.app.util.NetworkMonitor

val appModule = module {
    // Utilities
    single { NetworkMonitor(androidContext()) }

    // Sub-repositories
    single { AuthRepository() }
    single { SeekerRepository() }
    single { DonorRepository() }
    single { AdminRepository() }

    // Aggregate repository wired with all sub-repos
    single { QatraRepository(get(), get(), get(), get()) }

    // ViewModels
    viewModel { SharedAuthViewModel(get()) }
    viewModel { SeekerViewModel(get()) }
    viewModel { DonorViewModel(get()) }
    viewModel { AdminViewModel(get()) }
    viewModel { QatraViewModel(get(), get(), get(), get()) }
}
