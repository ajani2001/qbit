package qbit.api

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.*
import kotlin.random.Random
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1


@Serializable
sealed class Eid : Ref {

    abstract val value: Long

    override val id: Eid get() = this
}

@Serializable
data class Gid(override val value: Long) : Eid()

@Serializable
data class Tid(override val value: Long) : Eid()

@Serializable
data class Tombstone(override val id: Eid) : Entity {

    override fun tombstone(): Tombstone = this

}

fun tid() = Tid(Random.nextLong())

interface Ref {

    val id: Eid

    fun tombstone() = Tombstone(id)

}

interface Entity : Ref

class Record {

    inline fun <reified T> attr(name: String): T {
        TODO("Not yet implemented")
    }

}

object Qbit {

    suspend fun persist(vararg refs: Entity) {

    }

    fun query(sql: String): Flow<Record> {
        TODO("Not yet implemented")
    }

    fun <T> query(graphSpec: GraphSpec<T>): Flow<T> {
        TODO("Not yet implemented")
    }

}

@Serializer(forClass = Ref::class)
class RefSerializer() : KSerializer<Ref> {

    override fun deserialize(decoder: Decoder): Ref {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: Ref) {
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("id")

}

val refSerializer = RefSerializer()

class Schema

fun schema(builder: SchemaBuilder.() -> Unit): Schema {
    return Schema()
}

class SchemaBuilder {

    inline fun <reified T : Any> collection(name: String, serializer: KSerializer<T>, noinline builder: (CollectionBuilder<T>.() -> Unit)? = null) {
        TODO("Not yet implemented")
    }

    inline fun <reified T : Any> collection(name: String, serializer: KSerializer<T>, ex: T, noinline builder: (CollectionBuilder<T>.(T) -> Unit)? = null) {
        TODO("Not yet implemented")
    }

    fun migration(s: String, builder: MigrationBuilder.() -> Unit) {
        TODO("Not yet implemented")
    }

    fun view(s: String, serializer: Any) {
        TODO("Not yet implemented")
    }

}

class MigrationBuilder {
    fun addAttr(kProperty: KProperty1<*, *>) {
        TODO("Not yet implemented")
    }

    fun addAttrAlias(kProperty1: KProperty1<Category<*>, Ref?>, s: String) {
        TODO("Not yet implemented")
    }
}

class CollectionBuilder<T> {

    fun unique(vararg kProperty0: KProperty0<*>, ident: Boolean = false) {
        TODO("Not yet implemented")
    }

    fun unique(vararg kProperty1: KProperty1<*, *>, ident: Boolean = false) {
        TODO("Not yet implemented")
    }

}