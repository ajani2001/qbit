package qbit.typing

import qbit.EmptyDb
import qbit.Scientists
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.assertThrows
import qbit.factoring.Factor
import qbit.index.Index
import qbit.index.IndexDb
import qbit.schema.schema
import qbit.test.model.*
import qbit.testSchema
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame


abstract class MappingTest(val factor: Factor) {

    @Test
    fun `Test simple entity mapping`() {
        val gids = Gid(0, 0).nextGids()

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }), testsSerialModule)

        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = TheSimplestEntity(null, "addr"),
                optTheSimplestEntity = TheSimplestEntity(null, "optAddr"),
                theSimplestEntities = listOf(TheSimplestEntity(null, "lstAddr"))
        )

        val facts = factor(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts), testsSerialModule)
        val se = db2.pullEntity(facts.entityFacts[user]!!.first().gid)!!

        val fullUser = typify(db::attr, se, MUser::class, testsSerialModule)
        assertEquals("optAddr", fullUser.optTheSimplestEntity!!.scalar)
        assertEquals("login", fullUser.login)
        assertEquals(listOf("str1", "str2"), fullUser.strs)
        assertEquals("addr", fullUser.theSimplestEntity.scalar)
        assertEquals("lstAddr", fullUser.theSimplestEntities[0].scalar)
    }

    @Test
    fun `Test entity graph with mulitple entity types mapping`() {

        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(MUser::class) {
                uniqueString(MUser::login)
            }
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }), testsSerialModule)

        val entity = TheSimplestEntity(null, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = entity,
                optTheSimplestEntity = entity,
                theSimplestEntities = listOf(entity)
        )
        val facts = factor(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts), testsSerialModule)

        val se = db2.pullEntity(facts.entityFacts[user]!!.first().gid)!!
        val fullUser = typify(db::attr, se, MUser::class, testsSerialModule)

        assertEquals(fullUser.theSimplestEntity, fullUser.optTheSimplestEntity)
        assertEquals(fullUser.optTheSimplestEntity, fullUser.theSimplestEntities[0])
        assertSame(fullUser.theSimplestEntity, fullUser.optTheSimplestEntity)
        assertSame(fullUser.optTheSimplestEntity, fullUser.theSimplestEntities[0])
    }

    @Ignore
    @Test
    fun `test multiple states of entity in entity graph is prohibited`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(MUser::class)
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }), testsSerialModule)

        val addr = TheSimplestEntity(1, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = addr,
                optTheSimplestEntity = addr,
                theSimplestEntities = listOf(addr)
        )
        val userWithAddr = user.copy(theSimplestEntity = user.theSimplestEntity.copy(scalar = "newAddr"))

        assertThrows<QBitException> {
            factor(userWithAddr, db::attr, gids)
        }
    }

    // Support of self-refefencing entitys is under question now
    @Ignore
    @Test
    fun `Test factoring of self-referencing entity`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(Scientist::class)
            entity(Country::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }), testsSerialModule)
        val s = Scientist(null, 1, "s", emptyList(), Country(null, "c", 0), null)
        s.reviewer = s

        val facts = factor(s, db::attr, gids)
        assertEquals(6, facts.size)
        assertEquals(Gid(0, 7), facts.first { it.attr == Scientists.reviewer.name }.value)
    }

    @Test
    fun `Test bomb schema generation`() {
        val attrs = schema(testsSerialModule) {
            entity(Bomb::class)
        }
            .associateBy { it.name }
        assertEquals(QBoolean.code, attrs.getValue("Bomb/bool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/bool").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/boolList").type, "Expected ${QBoolean.list()}, but got ${DataType.ofCode(attrs.getValue("Bomb/boolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/boolListOpt").type, "Expected ${QBoolean.list()}, but got ${DataType.ofCode(attrs.getValue("Bomb/boolListOpt").type)}")
        assertEquals(QBoolean.code, attrs.getValue("Bomb/bool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/bool").type)}")
        assertEquals(QBoolean.code, attrs.getValue("Bomb/mutBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutBool").type)}")
        assertEquals(QBoolean.code, attrs.getValue("Bomb/mutOptBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutOptBool").type)}")
        assertEquals(QBoolean.code, attrs.getValue("Bomb/optBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/optBool").type)}")
        assertEquals(QByte.code, attrs.getValue("Bomb/byte").type, "Expected ${QByte}, but got ${DataType.ofCode(attrs.getValue("Bomb/byte").type)}")
        assertEquals(QByte.code, attrs.getValue("Bomb/optByte").type, "Expected ${QByte}, but got ${DataType.ofCode(attrs.getValue("Bomb/optByte").type)}")
        assertEquals(QByte.list().code, attrs.getValue("Bomb/byteList").type, "Expected ${QByte.list()}, but got ${DataType.ofCode(attrs.getValue("Bomb/byteList").type)}")
        assertEquals(QByte.list().code, attrs.getValue("Bomb/byteListOpt").type, "Expected ${QBytes.list()}, but got ${DataType.ofCode(attrs.getValue("Bomb/byteListOpt").type)}")
        assertEquals(QBytes.code, attrs.getValue("Bomb/bytes").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue("Bomb/bytes").type)}")
        assertEquals(QBytes.code, attrs.getValue("Bomb/optBytes").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue("Bomb/optBytes").type)}")
        assertEquals(QInt.code, attrs.getValue("Bomb/int").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue("Bomb/int").type)}")
        assertEquals(QInt.code, attrs.getValue("Bomb/optInt").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue("Bomb/optInt").type)}")
        assertEquals(QLong.code, attrs.getValue("Bomb/long").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue("Bomb/long").type)}")
        assertEquals(QLong.code, attrs.getValue("Bomb/optLong").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue("Bomb/optLong").type)}")
        assertEquals(QRef.code, attrs.getValue("Bomb/country").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/country").type)}")
        assertEquals(QRef.code, attrs.getValue("Bomb/mutCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutCountry").type)}")
        assertEquals(QRef.code, attrs.getValue("Bomb/mutOptCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutOptCountry").type)}")
        assertEquals(QRef.code, attrs.getValue("Bomb/optBomb").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/optBomb").type)}")
        assertEquals(QRef.code, attrs.getValue("Bomb/optCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/optCountry").type)}")
        assertEquals(QString.code, attrs.getValue("Bomb/optStr").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue("Bomb/optStr").type)}")
        assertEquals(QString.code, attrs.getValue("Bomb/str").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue("Bomb/str").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/boolList").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/boolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/boolListOpt").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/boolListOpt").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/mutBoolList").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutBoolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue("Bomb/mutBoolListOpt").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutBoolListOpt").type)}")
        assertEquals(QBytes.list().code, attrs.getValue("Bomb/bytesList").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue("Bomb/bytesList").type)}")
        assertEquals(QBytes.list().code, attrs.getValue("Bomb/bytesListOpt").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue("Bomb/bytesListOpt").type)}")
        assertEquals(QInt.list().code, attrs.getValue("Bomb/intList").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue("Bomb/intList").type)}")
        assertEquals(QInt.list().code, attrs.getValue("Bomb/intListOpt").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue("Bomb/intListOpt").type)}")
        assertEquals(QLong.list().code, attrs.getValue("Bomb/longList").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue("Bomb/longList").type)}")
        assertEquals(QLong.list().code, attrs.getValue("Bomb/longListOpt").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue("Bomb/longListOpt").type)}")
        assertEquals(QRef.list().code, attrs.getValue("Bomb/countiesList").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/countiesList").type)}")
        assertEquals(QRef.list().code, attrs.getValue("Bomb/countriesListOpt").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/countriesListOpt").type)}")
        assertEquals(QRef.list().code, attrs.getValue("Bomb/mutCountriesList").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutCountriesList").type)}")
        assertEquals(QRef.list().code, attrs.getValue("Bomb/mutCountriesListOpt").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue("Bomb/mutCountriesListOpt").type)}")
        assertEquals(QString.list().code, attrs.getValue("Bomb/strList").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue("Bomb/strList").type)}")
        assertEquals(QString.list().code, attrs.getValue("Bomb/strListOpt").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue("Bomb/strListOpt").type)}")
    }
}

