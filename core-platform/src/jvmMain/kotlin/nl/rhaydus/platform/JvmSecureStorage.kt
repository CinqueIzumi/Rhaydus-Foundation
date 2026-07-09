package nl.rhaydus.platform

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.withContext
import nl.rhaydus.common.AppDispatchers
import nl.rhaydus.common.AppLog
import nl.rhaydus.common.runCatchingCancellable

/**
 * Desktop secure storage: each secret is held by [KSafe], keyed by `key`. KSafe encrypts the value
 * and custodies the encryption key in the OS secret store (macOS Keychain, Windows DPAPI, or Linux
 * Secret Service), falling back to a software key (with a warning) when no OS vault is available. The
 * desktop counterpart of the Android Keystore / iOS Keychain implementations; I/O runs on the IO
 * dispatcher.
 *
 * **Cross-app isolation is the caller's job here, unlike the mobile targets.** The OS secret store is
 * scoped to the *user*, not to the application, and this class passes `key` through to [KSafe] verbatim
 * without namespacing it. Separation between two rhaydus desktop apps therefore comes entirely from the
 * `appNamespace` on the [KSafe] the caller constructs and injects. Two apps that omit it, or pick the
 * same one, will read and overwrite each other's secrets.
 */
class JvmSecureStorage(
    private val ksafe: KSafe,
    private val dispatchers: AppDispatchers,
) : SecureStorage {
    override suspend fun read(key: String): String? = withContext(dispatchers.io) {
        runCatchingCancellable {
            ksafe.get<String?>(
                key,
                null,
            )
        }.getOrElse { error ->
            AppLog.e(
                error,
                "Failed to read secret from the OS secret store",
            )

            null
        }
    }

    override suspend fun write(
        key: String,
        value: String,
    ) {
        withContext(dispatchers.io) {
            ksafe.put(
                key,
                value,
            )
        }
    }

    override suspend fun delete(key: String) {
        withContext(dispatchers.io) {
            ksafe.delete(key)
        }
    }
}
