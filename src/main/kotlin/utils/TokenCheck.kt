package com.swipehome.utils

import com.swipehome.database.tokens.Tokens
import com.swipehome.database.users.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TokenCheck {

    fun isValid(tokenToCheck: String): Boolean {
        if (tokenToCheck.isEmpty()) return false
        // Шукаємо токен у базі. Якщо знайдено — він валідний
        return transaction {
            Tokens.selectAll()
                .where { Tokens.token eq tokenToCheck }
                .singleOrNull() != null
        }
    }

    // Повертає логін користувача, якщо токен валідний, або null
    fun getLoginByToken(tokenToCheck: String): String? {
        if (tokenToCheck.isBlank()) return null

        return transaction {
            Tokens.selectAll()
                .where { Tokens.token eq tokenToCheck }
                .singleOrNull()?.get(Tokens.login)
        }
    }

    // Повертає IG користувача, якщо токен валідний, або null
    fun getIDByToken(tokenToCheck: String): Int? {
        if (tokenToCheck.isBlank()) return null

        return transaction {
            Tokens.selectAll()
                .where { Tokens.token eq tokenToCheck }
                .singleOrNull()?.get(Tokens.id_user)
        }
    }

    fun isTokenAdmin(tokenToCheck: String): Boolean {
        // Логіку для адміна додамо пізніше, коли ,буде зв'язаний токен з таблицею Users
        return true
    }

    fun isTokenOwner(tokenToCheck: String): Boolean {
        if (tokenToCheck.isBlank()) return false

        return transaction {
            // Явно вказуємо, які саме колонки з'єднувати
            val query = Tokens.innerJoin(
                otherTable = Users,
                additionalConstraint = { Tokens.id_user eq Users.id_user }
            )
                .select(Users.is_verified_owner) // Вибираємо тільки потрібну колонку
                .where { Tokens.token eq tokenToCheck }
                .singleOrNull()

            // Якщо запис знайдено, повертаємо значення колонки. Якщо ні — false
            query?.get(Users.is_verified_owner) ?: false
        }
    }
}