package com.swipehome.database.swipes



import com.swipehome.database.properties.Properties
import com.swipehome.database.users.Users
import java.time.OffsetDateTime
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Swipes: Table("swipes") {
    val id_swipe = integer("id_swipe").autoIncrement()
    val id_user = integer("id_user").references(Users.id_user)
    val id_property = integer("id_property").references(Properties.id_property)
    val action = varchar("action", 10)
    val created_at = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_swipe)

    fun insert(swipeDTO: SwipeDTO){
        transaction {
            Swipes.insert {
                it[id_user] = swipeDTO.id_user
                it[id_property] = swipeDTO.id_property
                it[action] = swipeDTO.action
            }
        }
    }

    fun fetchSwipesByID(searchID: Int): SwipeDTO? {
        return transaction {
            val resultRow = Swipes.selectAll().where { Swipes.id_swipe eq searchID }.singleOrNull()

            resultRow?.let {
                SwipeDTO(
                    id_swipe = it[id_swipe],
                    id_user = it[id_user],
                    id_property = it[id_property],
                    action = it[action],
                    created_at = it[created_at].toString()
                )
            }
        }
    }
    // Гарантує, що юзер може свайпнути конкретну квартиру тільки 1 раз
    init {
        uniqueIndex(id_user, id_property)
    }
}