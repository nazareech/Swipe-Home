package com.swipehome.utils

import io.github.cdimascio.dotenv.dotenv
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {
    //
    private val dotenv = dotenv()

    // Дані для входу (краще зберігати у змінних оточення)
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private val SENDER_EMAIL = dotenv["SMTP_EMAIL"]  ?: throw Exception("SMTP_PASSWORD not found in .env")
    private val SENDER_PASSWORD = dotenv["SMTP_PASSWORD"] ?: throw Exception("SMTP_PASSWORD not found in .env")

    fun sendResetCode(recipientEmail: String, code: Int){
        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
        }

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "SwipeHome Support"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                subject = "Відновлення пароля  SwipeHome"

                // HTML-версія листа, щоб виглядало гарно
                val htmlContent = """
                    <h3>Привіт!</h3>
                    <p>Хтось запитав відновлення пароля для твого акаунта.</p>
                    <p>Твій код підтвердження: <b style="font-size: 20px; color: #4CAF50;">$code</b></p>
                    <p>Код дійсний 15 хвилин</p>
                    <p>Якщо це був не ти, просто проігноруй цей лист <p/>
                """.trimIndent()

                setContent(htmlContent, "text/html; charset=UTF-8")
            }

            Transport.send(message)
            print("Email send successfuly to ${recipientEmail}")

        }catch (e:Exception){
            print("Failed to send email ${e.localizedMessage}")
        }
    }
}