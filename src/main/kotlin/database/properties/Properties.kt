package com.swipehome.database.properties

import com.swipehome.database.swipes.Swipes
import com.swipehome.features.properties.FetchPropertyRequest
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

object Properties: Table("properties") {
    val id_property = integer("id_property").autoIncrement()
    val id_owner = integer("id_owner")//.references(Users.id_user)
    val title = varchar("title", 25)
    val description = text("description")
    val localization = varchar("localization", 100)
    val price = double("price")
    val area = double("area")
    val rooms = integer("rooms")
    val category = varchar("category", 50)
    val subcategory = varchar("subcategory", 50)
    val parking = varchar("parking", 50)
    val pets_allowed = bool("pets_allowed")
    val elevator = bool("elevator")
    val furniture = bool("furniture")
    val building_type = varchar("building_type", 50)
    val status = varchar("status", 20)
    val created_at = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }


    override val primaryKey = PrimaryKey(id_property)

    fun insert(propertyDTO: PropertyDTO){
        transaction {
            Properties.insert {
                it[id_owner] = propertyDTO.id_owner
                it[title] = propertyDTO.title
                it[description] = propertyDTO.description
                it[localization] = propertyDTO.localization
                it[price] = propertyDTO.price
                it[area] = propertyDTO.area
                it[rooms] = propertyDTO.rooms
                it[category] = propertyDTO.category
                it[subcategory] = propertyDTO.subcategory
                it[parking] = propertyDTO.parking
                it[pets_allowed] = propertyDTO.pets_allowed
                it[elevator] = propertyDTO.elevator
                it[furniture] = propertyDTO.furniture
                it[building_type] = propertyDTO.building_type
                it[status] = propertyDTO.status
            }
        }
    }

    fun fetchPropertiesByID(searchIdProperty: Int): PropertyDTO? {
        return transaction {
            val resultRow = Properties.selectAll().where { id_property eq searchIdProperty }.singleOrNull()
            resultRow?.let { rowToDTO(it) }
        }
    }

    fun fetchAllProperties() {
        return transaction {
            val resultRow = Properties.selectAll().singleOrNull()
            resultRow?.let { rowToDTO(it) }
        }
    }

    fun fetchUnswipedProperties(request: FetchPropertyRequest, userID: Int): List<PropertyDTO> {
        return transaction {
            // Виключаємо вже переклянуті картки
            // шувкаємо id_property, які цей користував вже свайпнув
            val swipedPropertiesSubQuery = Swipes
                .select(Swipes.id_property)
                .where { Swipes.id_user eq userID }

            // відключаємо власні оголошення користувачі
            // Робимо ОДИН спільний базовий запит з двома залізобетонними умовами:
            //    - Оголошення НЕ належить поточному юзеру (id_owner neq userID)
            //    - Оголошення НЕ було свайпнуте цим юзером (id_property notInSubQuery ...)
            val query = Properties.selectAll().where {
                (Properties.id_owner neq userID) and (Properties.id_property notInSubQuery swipedPropertiesSubQuery)
            }

            // Динамічні фільтри
            request.category?.let { query.andWhere { Properties.category eq it } }
            request.subcategory?.let { query.andWhere { Properties.subcategory eq it } }
            request.localization?.let { query.andWhere { Properties.localization eq it } }

            request.priceMin?.let { query.andWhere { Properties.price greaterEq it } }
            request.priceMax?.let { query.andWhere { Properties.price lessEq it } }

            request.areaMin?.let { query.andWhere { Properties.area greaterEq it } }
            request.areaMax?.let { query.andWhere { Properties.area lessEq it } }

            // Логіка Min та Max для кімнат
            request.roomsMin?.let { query.andWhere { Properties.rooms greaterEq it } }
            request.roomsMax?.let { query.andWhere { Properties.rooms lessEq it } }

            request.parking?.let { query.andWhere { Properties.parking eq it } }
            request.petsAllowed?.let { query.andWhere { Properties.pets_allowed eq it } }
            request.elevator?.let { query.andWhere { Properties.elevator eq it } }
            request.furniture?.let { query.andWhere { Properties.furniture eq it } }
            request.buildingType?.let { query.andWhere { Properties.building_type eq it } }

            // Пагінація — поділ списку на окремі частини
            query.limit(request.limit).offset(request.offset.toLong())

            val propertiesList = query.map { rowToDTO(it) }

            val propertyIds = propertiesList.mapNotNull { it.id_property }

            val imagesMap: Map<Int, Map<String, Boolean>> = if (propertyIds.isNotEmpty()) {
                PropertyImages.selectAll()
                    .where { PropertyImages.id_property inList propertyIds }
                    .groupBy { it[PropertyImages.id_property] }
                    .mapValues { (_, rows) ->
                        rows.associate { it[PropertyImages.image_url] to it[PropertyImages.is_main] }
                    }
            } else {
                emptyMap()
            }
            propertiesList.map { dto ->
                dto.copy(images_map = imagesMap[dto.id_property] ?: emptyMap())
            }
        }
    }

    fun fetchMatchedProperties(userID: Int): List<PropertyDTO> {
        return transaction {
            // Об'єднуємо Properties та Swipes за ID оголошення
            val query = Properties.innerJoin(
                otherTable = Swipes,
                additionalConstraint = { Properties.id_property eq Swipes.id_property }
            )
                .selectAll()
                // Фільтруємо: свайп належить цьому юзеру І дія є "right" (лайк)
                .where { (Swipes.id_user eq userID) and (Swipes.action eq "right") }
            // Мапимо результат у список DTO за допомогою твоєї готової функції rowToDTO
            query.map { row -> rowToDTO(row) }
        }
    }

    // Зручна функція, щоб не дублювати код конвертації з ResultRow у PropertyDTO
    fun rowToDTO(row: ResultRow): PropertyDTO {
        return PropertyDTO(
            id_property = row[id_property],
            id_owner = row[id_owner],
            title = row[title],
            description = row[description],
            localization = row[localization],
            price = row[price],
            area = row[area],
            rooms = row[rooms],
            category = row[category],
            subcategory = row[subcategory],
            parking = row[parking],
            pets_allowed = row[pets_allowed],
            elevator = row[elevator],
            furniture = row[furniture],
            building_type = row[building_type],
            status = row[status],
            created_at = row[created_at].toString()
        )
    }
}