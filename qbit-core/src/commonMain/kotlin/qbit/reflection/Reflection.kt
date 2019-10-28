package qbit.reflection

import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.platform.BigDecimal
import qbit.platform.Instant
import qbit.platform.Instants
import qbit.platform.ZoneIds
import qbit.platform.ZonedDateTime
import qbit.platform.ZonedDateTimes
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

fun findMutableProperties(type: KClass<*>): List<KCallable<*>> {
    return findProperties(type).filterIsInstance<KMutableProperty1<*, *>>()
}

fun findProperties(type: KClass<*>): List<KCallable<*>> {
    val copys = type.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.name == "copy" && it.returnType.classifier == type }

    val constrs = type.constructors

    val copy = copys.sortedBy { it.parameters.size }
            .firstOrNull {
                constrs
                        .filter { c ->
                            // filter `this`
                            val explicitParams = it.parameters.filter { it.name != null }

                            val paramPairs = c.parameters.zip(explicitParams)
                            val allMatches = paramPairs.all {
                                it.first.name == it.second.name && it.first.type.classifier == it.second.type.classifier
                            }
                            c.parameters.size == explicitParams.size && allMatches
                        }
                        .any()
            }

    copy ?: throw QBitException("Could not determine properties for type $type. Is it data class?")

    val dataClassProps = copy.parameters.map { it.name }.toSet()
    return type.members
            .filterIsInstance<KProperty1<*, *>>()
            .filter { it.name in dataClassProps }
}

fun <T : Any> findPrimaryConstructor(type: KClass<T>): KFunction<T> {

    return type.constructors.filter {
        val props = findProperties(type).map { p -> p.name }.toSet()
        it.parameters.all { p -> p.name in props }
    }
            .maxBy { it.parameters.size }
            ?: throw QBitException("There is no primary constructor for type $type")
}

val defaults = hashMapOf<KClass<*>, Any>()

@Suppress("UNCHECKED_CAST")
fun <T : Any> default(type: KClass<T>): T =
        defaults.getOrPut(type) {
            when (type) {
                Boolean::class -> false
                String::class -> ""
                Byte::class -> 0.toByte()
                Int::class -> 0
                Long::class -> 0L
                List::class -> listOf<Any>()
                Instant::class -> Instants.ofEpochMilli(0)
                ZonedDateTime::class -> ZonedDateTimes.of(0, 1, 1, 0, 0, 0, 0, ZoneIds.of("UTC"))
                BigDecimal::class -> BigDecimal(0)
                ByteArray::class -> ByteArray(0)
                else -> {
                    val constr = type.constructors.first()
                    val args = constr.parameters.map {
                        if (it.type.isMarkedNullable) {
                            it to null
                        } else {
                            it to default(it.type.classifier as KClass<*>)
                        }
                    }
                            .toMap()
                    constr.callBy(args)
                }
            }
        } as T

fun KParameter.isId() =
        this.name == "id" && (this.type.classifier == Long::class || this.type.classifier == Gid::class)

fun setableProps(type: KClass<*>): List<KMutableProperty1<Any, Any>> {
    return findProperties(type).filterIsInstance<KMutableProperty1<Any, Any>>()
}

fun KClass<*>.propertyFor(attr: Attr<*>) =
        findProperties(this).firstOrNull { attr.name.endsWith(it.name) }

@Suppress("UNCHECKED_CAST")
fun <T : Any> getListElementClass(type: KType) = type.arguments[0].type!!.classifier as KClass<T>
