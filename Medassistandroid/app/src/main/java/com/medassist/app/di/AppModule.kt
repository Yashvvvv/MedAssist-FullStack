package com.medassist.app.di

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.medassist.app.BuildConfig
import com.medassist.app.core.auth.AuthGateway
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.auth.TokenStore
import com.medassist.app.core.network.MedAssistHttpClient
import com.medassist.app.core.repository.AuthRepository
import com.medassist.app.core.repository.DiscoveryRepository
import com.medassist.app.core.repository.InsightsRepository
import com.medassist.app.core.repository.OrdersRepository
import com.medassist.app.core.repository.PharmacyRepository
import com.medassist.app.core.repository.SalesRepository
import com.medassist.app.core.repository.StockRepository
import com.medassist.app.data.AppPreferences
import com.medassist.app.data.KeystoreCipher
import com.medassist.app.data.KeystoreTokenStore
import com.medassist.app.data.LocationProvider
import com.medassist.app.data.PharmacyContext
import com.medassist.app.data.PhotoReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient

/**
 * The object graph. SessionManager and the authenticated HttpClient depend on each other
 * (the client asks the session for tokens; the session refreshes over HTTP), so the session
 * talks to the network through [AuthGateway], backed by a separate client without the Auth plugin.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        KeystoreTokenStore(context.sessionStore, KeystoreCipher())

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): AppPreferences = AppPreferences(context.settingsStore)

    @Provides
    @Singleton
    @PlainClient
    fun providePlainClient(@ApplicationContext context: Context): HttpClient =
        MedAssistHttpClient.create(BuildConfig.API_BASE_URL, enableLogging = BuildConfig.DEBUG, hasNetwork = context.networkCheck())

    @Provides
    @Singleton
    fun provideAuthGateway(@PlainClient plain: HttpClient): AuthGateway =
        // The gateway only needs the plain client; the authenticated one is unused for refresh/logout.
        AuthRepository(plain, plain)

    @Provides
    @Singleton
    fun provideSession(store: TokenStore, gateway: AuthGateway, @ApplicationScope scope: CoroutineScope): SessionManager =
        SessionManager(store, gateway, scope)

    @Provides
    @Singleton
    fun provideHttpClient(
        @ApplicationContext context: Context,
        session: SessionManager,
        @ApplicationScope scope: CoroutineScope,
    ): HttpClient {
        val client = MedAssistHttpClient.create(
            BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.DEBUG,
            session = session,
            hasNetwork = context.networkCheck(),
        )
        // Ktor caches the first loaded token. Drop it on every login/logout/switch, or the previous
        // user's token keeps being sent.
        scope.launch {
            session.session.drop(1).collect {
                if (BuildConfig.DEBUG) Log.d("Session", "Session changed: ${it::class.simpleName}")
                client.authProvider<BearerAuthProvider>()?.clearToken()
            }
        }
        return client
    }

    @Provides
    @Singleton
    fun provideAuthRepository(client: HttpClient, @PlainClient plain: HttpClient) = AuthRepository(client, plain)

    @Provides @Singleton fun providePharmacyRepository(client: HttpClient) = PharmacyRepository(client)
    @Provides @Singleton fun provideStockRepository(client: HttpClient) = StockRepository(client)
    @Provides @Singleton fun provideSalesRepository(client: HttpClient) = SalesRepository(client)
    @Provides @Singleton fun provideOrdersRepository(client: HttpClient) = OrdersRepository(client)
    @Provides @Singleton fun provideDiscoveryRepository(client: HttpClient) = DiscoveryRepository(client)
    @Provides @Singleton fun provideInsightsRepository(client: HttpClient) = InsightsRepository(client)

    @Provides
    @Singleton
    fun providePharmacyContext(
        repository: PharmacyRepository,
        preferences: AppPreferences,
        session: SessionManager,
        @ApplicationScope scope: CoroutineScope,
    ) = PharmacyContext(repository, preferences, session, scope)

    @Provides @Singleton fun provideLocation(@ApplicationContext context: Context) = LocationProvider(context)
    @Provides @Singleton fun providePhotoReader(@ApplicationContext context: Context) = PhotoReader(context)

    /** activeNetwork is null in airplane mode: retries then can't succeed, so they're skipped. */
    private fun Context.networkCheck(): () -> Boolean {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        return { connectivity?.activeNetwork != null }
    }
}
