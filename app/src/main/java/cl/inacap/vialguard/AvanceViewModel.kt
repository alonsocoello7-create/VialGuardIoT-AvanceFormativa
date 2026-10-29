package cl.inacap.vialguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AvanceState(
    val username: String = "operador", val password: String = "",
    val currentUser: String? = null, val busy: Boolean = false, val message: String = "",
    val accountExists: Boolean = false
)

/** Autenticación local protegida. Firebase queda pendiente de integración. */
class AvanceViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalCredentialStore(application)
    private val mutable = MutableStateFlow(AvanceState(accountExists = store.hasAccount()))
    val state = mutable.asStateFlow()
    fun username(value: String) { mutable.update { it.copy(username = value, message = "") } }
    fun password(value: String) { mutable.update { it.copy(password = value, message = "") } }
    fun login() {
        val s = mutable.value
        if (s.busy) return
        mutable.update { it.copy(busy = true, password = "", message = "") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (s.accountExists) store.login(s.username, s.password) else store.create(s.username, s.password)
            }
            result.onSuccess { user -> mutable.update { it.copy(currentUser = user, busy = false,
                accountExists = true, message = "Sesión local autenticada") } }
                .onFailure { error -> mutable.update { it.copy(busy = false,
                    message = error.message ?: "No se pudo abrir el registro protegido.") } }
        }
    }
    fun logout() { mutable.value = AvanceState(accountExists = store.hasAccount()) }
}
