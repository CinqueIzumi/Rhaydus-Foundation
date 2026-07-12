package nl.rhaydus.platform

/**
 * Platform-backed secure storage for sensitive secrets, keyed by an arbitrary string. Secrets are
 * never persisted in plain preferences; each target keeps them in hardware-backed storage (Android
 * Keystore-encrypted file, iOS Keychain, desktop OS secret store). Common code orchestrates
 * loading/migration; this interface is the only piece that differs per platform.
 */
interface SecureStorage {
    suspend fun read(key: String): String?

    suspend fun write(
        key: String,
        value: String,
    )

    suspend fun delete(key: String)
}
