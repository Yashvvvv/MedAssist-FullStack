package com.medassist.app.data

import com.medassist.app.core.auth.Session
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.model.BranchView
import com.medassist.app.core.repository.PharmacyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Which branch a staff member is working at. Billing and stock are per branch, so every
 * pharmacy screen reads [branch]. The choice is remembered; the first branch is the default.
 */
class PharmacyContext(
    private val repository: PharmacyRepository,
    private val preferences: AppPreferences,
    session: SessionManager,
    private val scope: CoroutineScope,
) {
    private val _branches = MutableStateFlow<List<BranchView>>(emptyList())
    val branches: StateFlow<List<BranchView>> = _branches.asStateFlow()

    val branch: StateFlow<BranchView?> = combine(_branches, preferences.branchId) { list, saved ->
        list.firstOrNull { it.id == saved && it.active } ?: list.firstOrNull { it.active }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        // Reload whenever the active pharmacy changes (login, switch org); forget it on logout.
        scope.launch {
            session.session
                .map { (it as? Session.SignedIn)?.takeIf { s -> s.isStaff }?.tokens?.orgId }
                .distinctUntilChanged()
                .collect { orgId ->
                    _branches.value = emptyList()
                    if (orgId != null) refresh()
                }
        }
    }

    fun refresh() {
        scope.launch { repository.branches().onSuccess { _branches.value = it } }
    }

    fun select(branchId: String) {
        scope.launch { preferences.setBranch(branchId) }
    }
}
