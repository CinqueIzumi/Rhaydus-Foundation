package nl.rhaydus.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import nl.rhaydus.common.AppDispatchers
import nl.rhaydus.common.AppLog
import nl.rhaydus.common.runCatchingCancellable

/**
 * Android secure storage: each secret is encrypted with a single AES/GCM key held in the Android
 * Keystore, and the ciphertext (IV size byte + IV + ciphertext) is written to a per-key file under
 * `filesDir` (`secure_<sanitized-key>.enc`). The Keystore custodies the encryption key; only the
 * ciphertext ever touches disk.
 */
class AndroidSecureStorage(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : SecureStorage {
    // `by lazy` is SYNCHRONIZED, so the Keystore key is loaded-or-generated exactly once even when
    // several secrets are touched concurrently on a cold start (racing generateKey() on one alias).
    private val secretKey: SecretKey by lazy { loadOrGenerateKey() }

    override suspend fun read(key: String): String? = withContext(dispatchers.io) {
        val file = storageFile(key)

        if (file.exists().not()) return@withContext null

        runCatchingCancellable {
            file.inputStream().use { input ->
                val ivSize = input.read()
                val iv = ByteArray(ivSize).also { input.read(it) }
                val ciphertext = input.readBytes()

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    GCMParameterSpec(
                        GCM_TAG_LENGTH_BITS,
                        iv,
                    ),
                )

                String(
                    cipher.doFinal(ciphertext),
                    Charsets.UTF_8,
                )
            }
        }.getOrElse { error ->
            AppLog.e(
                error,
                "Failed to decrypt secret - discarding ciphertext",
            )

            file.delete()

            null
        }
    }

    override suspend fun write(
        key: String,
        value: String,
    ) {
        withContext(dispatchers.io) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
            )

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            storageFile(key).outputStream().use { output ->
                output.write(iv.size)
                output.write(iv)
                output.write(ciphertext)
            }
        }
    }

    override suspend fun delete(key: String) {
        withContext(dispatchers.io) {
            storageFile(key).delete()
        }
    }

    private fun storageFile(key: String): File = File(
        context.filesDir,
        "$FILE_PREFIX${key.sanitizedForFileName()}$FILE_SUFFIX",
    )

    // Keeps the on-disk file name to a safe alphanumeric set; distinct keys still map to distinct files
    // because non-alphanumeric characters are escaped to their code point rather than collapsed.
    private fun String.sanitizedForFileName(): String = buildString {
        this@sanitizedForFileName.forEach { char ->
            if (char.isLetterOrDigit()) append(char) else append("_${char.code}_")
        }
    }

    private fun loadOrGenerateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        val existing = keyStore.getKey(
            KEY_ALIAS,
            null,
        ) as? SecretKey

        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        generator.init(spec)

        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "rhaydus_secure_storage"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val FILE_PREFIX = "secure_"
        const val FILE_SUFFIX = ".enc"
    }
}
