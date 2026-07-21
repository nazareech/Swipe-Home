package com.swipehome.utils

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val CRYPTO_ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BIT = 12

    // Секретний ключ для AES-256 має містити РІВНО 32 байти (32 символи)
    // У реальному проєкті цей ключ НІКОЛИ не зберігають у коді,
    // а беруть зі змінних оточення: System.getenv("AES_SECRET_KEY")
    private const val MY_SECRET_KEY_STRING = "SwipeHomeSuperSecretKey12345678900"
    private val secretKey = SecretKeySpec(MY_SECRET_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")

    // Шифруємо повідомлення
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(CRYPTO_ALGORITHM)

        // IV (Initialization Vector) - це унікальна "сіль" для кожного повідомлення
        // Завдяки їй два однакових слова "Привіт" будуть зашифровані по-різному
        val iv = ByteArray(IV_LENGTH_BIT)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Зшиваємо IV та сам зашифрований текст разом, інакше ми не зможемо його розшифрувати
        val ivAndCipherText = iv + cipherText

        // Перетворюємо на байти у зручний рядок Base64 для збереження в БД
        return Base64.getEncoder().encodeToString(ivAndCipherText)
    }

    // Розшифровка повідомлень
    fun decrypt(cipherTextBase64: String): String {
        return try {
            val decoded = Base64.getDecoder().decode(cipherTextBase64)

            // Відрізаємо перші 12 байтів (це наш IV)
            val iv = decoded.copyOfRange(0, IV_LENGTH_BIT)
            // Все інше зашифрований текст
            val cipherText = decoded.copyOfRange(IV_LENGTH_BIT, decoded.size)

            val cipher = Cipher.getInstance(CRYPTO_ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val plainText = cipher.doFinal(cipherText)

            String(plainText, Charsets.UTF_8)
        } catch (e: Exception){
            // Якщо виникає помилка розшифровки (наприклад, повідомлення
            // було збережене до того, як було додано шифрування), ми просто повертаємо
            // оригінальний текст. Це вбереже додаток від крашу через старі дані.
            cipherTextBase64
        }
    }
}