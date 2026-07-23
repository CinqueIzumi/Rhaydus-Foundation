package nl.rhaydus.platform

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.rhaydus.common.AppDispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidSecureStorageTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var dispatchers: AppDispatchers

    private lateinit var cipher: Cipher
    private lateinit var secretKey: SecretKey
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator

    private val fakeIv = ByteArray(12) { it.toByte() }

    @BeforeEach
    fun setUp() {
        context = mockk {
            every {
                filesDir
            } returns tempDir
        }

        val dispatcher = UnconfinedTestDispatcher()
        dispatchers = AppDispatchers(
            main = dispatcher,
            io = dispatcher,
            default = dispatcher,
        )

        secretKey = mockk()
        keyStore = mockk(relaxed = true)
        keyGenerator = mockk(relaxed = true)
        // A single relaxed cipher, mocked as an identity transform, serves both encrypt and decrypt:
        // doFinal() echoes back whatever bytes it was given, so write-then-read round-trips without
        // needing to track which mode the cipher was init()'d with.
        cipher = mockk(relaxed = true)

        mockkStatic(Cipher::class)
        mockkStatic(KeyStore::class)
        mockkStatic(KeyGenerator::class)

        every {
            KeyStore.getInstance("AndroidKeyStore")
        } returns keyStore
        every { keyStore.getKey(
            "rhaydus_secure_storage",
            null,
        ) } returns secretKey

        every {
            Cipher.getInstance("AES/GCM/NoPadding")
        } returns cipher

        every {
            cipher.iv
        } returns fakeIv

        val bytesSlot = slot<ByteArray>()
        every {
            cipher.doFinal(capture(bytesSlot))
        } answers { bytesSlot.captured }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun buildStorage(): AndroidSecureStorage = AndroidSecureStorage(
        context = context,
        dispatchers = dispatchers,
    )

    @Nested
    inner class Read {
        @Test
        fun `returns the round-tripped value after write then read`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            storage.write(
                "my-key",
                "my-value",
            )

            // ----- Act -----
            val result = storage.read("my-key")

            // ----- Assert -----
            result shouldBe "my-value"
        }

        @Test
        fun `returns null when no file exists for the key`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()

            // ----- Act -----
            val result = storage.read("missing-key")

            // ----- Assert -----
            result shouldBe null
        }

        @Test
        fun `returns null and deletes the file when the ciphertext cannot be decrypted`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            val corruptedFile = File(
                tempDir,
                "secure_corrupted.enc",
            )
            corruptedFile.outputStream().use { output ->
                output.write(fakeIv.size)
                output.write(fakeIv)
                output.write(byteArrayOf(1, 2, 3, 4))
            }
            every {
                cipher.doFinal(any())
            } throws IllegalStateException("bad tag")

            // ----- Act -----
            val result = storage.read("corrupted")

            // ----- Assert -----
            result shouldBe null
            corruptedFile.exists() shouldBe false
        }
    }

    @Nested
    inner class Write {
        @Test
        fun `creates a secure enc file on disk after write`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            val storageFile = File(
                tempDir,
                "secure_my_45_key.enc",
            )

            // ----- Act -----
            storage.write(
                "my-key",
                "my-value",
            )

            // ----- Assert -----
            storageFile.exists() shouldBe true
        }

        @Test
        fun `sanitizes a non-alphanumeric character in the key into its escaped file name`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            val storageFile = File(
                tempDir,
                "secure_api_95_key.enc",
            )

            // ----- Act -----
            storage.write(
                "api_key",
                "my-value",
            )

            // ----- Assert -----
            storageFile.exists() shouldBe true
        }

        @Test
        fun `writing one key does not disturb a value already stored under another key`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            storage.write(
                "key-a",
                "value-a",
            )

            // ----- Act -----
            storage.write(
                "key-b",
                "value-b",
            )

            // ----- Assert -----
            storage.read("key-a") shouldBe "value-a"
            storage.read("key-b") shouldBe "value-b"
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `removes the file from disk after delete`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            storage.write(
                "key-to-delete",
                "value",
            )
            val storageFile = File(
                tempDir,
                "secure_key_45_to_45_delete.enc",
            )

            // ----- Act -----
            storage.delete("key-to-delete")

            // ----- Assert -----
            storageFile.exists() shouldBe false
        }

        @Test
        fun `read returns null for a key after it has been deleted`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            storage.write(
                "key-to-delete",
                "value",
            )
            storage.delete("key-to-delete")

            // ----- Act -----
            val result = storage.read("key-to-delete")

            // ----- Assert -----
            result shouldBe null
        }

        @Test
        fun `deleting one key does not disturb a value stored under another key`() = runTest {
            // ----- Arrange -----
            val storage = buildStorage()
            storage.write(
                "key-a",
                "value-a",
            )
            storage.write(
                "key-b",
                "value-b",
            )

            // ----- Act -----
            storage.delete("key-a")

            // ----- Assert -----
            storage.read("key-a") shouldBe null
            storage.read("key-b") shouldBe "value-b"
        }
    }
}
