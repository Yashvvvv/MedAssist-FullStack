package com.medassist.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.auth.Session
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.model.MembershipView
import com.medassist.app.core.repository.AuthRepository
import com.medassist.app.data.AppPreferences
import com.medassist.app.data.ThemeMode
import com.medassist.app.ui.common.message
import com.medassist.app.core.model.toApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide: who is signed in (decides pharmacy vs patient mode), theme, sign-out, switching pharmacy. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val auth: AuthRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    val session: StateFlow<Session> = sessionManager.session
    val theme: StateFlow<ThemeMode> = preferences.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private val _memberships = MutableStateFlow<List<MembershipView>>(emptyList())
    val memberships: StateFlow<List<MembershipView>> = _memberships.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadMemberships() {
        viewModelScope.launch { auth.me().onSuccess { _memberships.value = it.memberships } }
    }

    /** [orgId] null = continue as a customer (patient mode) without leaving any pharmacy. */
    fun switchTo(orgId: String?) {
        viewModelScope.launch {
            auth.switchOrg(orgId)
                .onSuccess { sessionManager.signIn(it) }
                .onFailure { _message.value = it.toApiError().message() }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setTheme(mode) }
    }

    fun signOut() {
        viewModelScope.launch {
            preferences.setBranch(null)
            sessionManager.signOut()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
