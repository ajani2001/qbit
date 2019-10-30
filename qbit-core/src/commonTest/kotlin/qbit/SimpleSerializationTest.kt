package qbit

import kotlinx.io.core.EOFException
import kotlinx.io.core.ExperimentalIoApi
import kotlinx.io.core.Input
import qbit.api.gid.Gid
import qbit.api.model.*
import qbit.api.system.DbUuid
import qbit.api.gid.Iid
import qbit.platform.*
import qbit.serialization.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

val random = Random(1)

@UseExperimental(ExperimentalIoApi::class)
class SimpleSerializationTest {

    private val intValues: List<Int> = listOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE, Byte.MAX_VALUE.toInt(), Byte.MIN_VALUE.toInt())
    private val longValues: List<Long> = listOf(Long.MAX_VALUE, Long.MIN_VALUE, *(intValues.map { it.toLong() }.toTypedArray()))
    private val decimalValues: List<BigDecimal> = listOf(
            BigDecimal(Long.MIN_VALUE).minus(BigDecimal(1)),
            BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1)),
            *longValues.map { BigDecimal(it) }.toTypedArray())

    @Test
    fun testReadLong() {
        testValues(longValues, ::serializeLong, ::readLong)
    }

    @Test
    fun testReadInt() {
        testValues(intValues, ::serializeInt, ::readInt)
    }

    @Test
    fun testDeserializeLong() {
        testValues(longValues, { serialize(it) }, { deserialize(it, QLong) as Long })
    }

    @Test
    fun testMaxInt() {
        assertEquals(Int.MAX_VALUE, deserialize(serialize(Int.MAX_VALUE).asInput(), QInt))
    }

    @Test
    fun testSerializeInt() {
        val zeroRes = serializeInt(0)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), zeroRes)

        val maxRes = serializeInt(Int.MAX_VALUE)
        assertArrayEquals(byteArrayOf(127, -1, -1, -1), maxRes)

        val minRes = serializeInt(Int.MIN_VALUE)
        assertArrayEquals(byteArrayOf(-128, 0, 0, 0), minRes)
    }

    @Test
    fun testDeserializeInt() {
        testValues(intValues, { serialize(it) }, { deserialize(it, QInt) as Int })
    }

    @Test
    fun testDeSerializeDecimal() {
        testValues(decimalValues, { serialize(it) }, { deserialize(it, QDecimal) as BigDecimal })
    }

    private fun <T> testValues(values: List<T>, s: (T) -> ByteArray, r: (Input) -> T) {
        for (v in values) {
            assertEquals(v, r(s(v).asInput()))
        }
    }

    @Test
    fun testBoolean() {
        assertEquals(true, deserialize(serialize(true).asInput(), QBoolean))
        assertEquals(false, deserialize(serialize(false).asInput(), QBoolean))
    }

    @Test
    fun testN() {
        assertEquals(nullHash, Hash(deserialize(serialize(NodeRef(nullHash)).asInput(), QBytes) as ByteArray))
        val randomBytes = Hash(randomBytes(HASH_LEN, random))
        assertEquals(randomBytes, Hash(deserialize(serialize(NodeRef(randomBytes)).asInput(), QBytes) as ByteArray))

        val fewBytes = Hash(randomBytes(HASH_LEN - 1, random))
        try {
            deserialize(byteArray(QBytes.code, serializeInt(HASH_LEN), fewBytes.bytes).asInput(), QBytes)
            fail("eof error expected")
        } catch (e: DeserializationException) {
            assertTrue(e.cause is EOFException)
        }
    }

    @Test
    fun testByteArray() {
        val random = randomBytes(random.nextInt(1025), random)
        assertArrayEquals(random, deserialize(serialize(random).asInput(), QBytes) as ByteArray)

        val twoBytes = byteArrayOf(QBytes.code, 0, 0, 0, 3, 0, 0)
        try {
            deserialize(twoBytes.asInput(), QBytes)
            fail("eof error expected")
        } catch (e : DeserializationException) {
            assertTrue(e.cause is EOFException)
        }
    }

    @Test
    fun testString() {
        val random = randomString(random.nextInt(1025), random)
        assertEquals(random, deserialize(serialize(random).asInput(), QString))
    }

    @Test
    fun testHighUnicodeString() {
        val hieroglyphs = "ライン 线 שו"
        assertEquals(hieroglyphs, deserialize(serialize(hieroglyphs).asInput(), QString))
    }

    @Test
    fun testRoot() {
        val iid = Iid(1, 4)
        val root = Root(null, DbUuid(iid), currentTimeMillis(), NodeData(arrayOf(Eav(Gid(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(SimpleSerialization.serializeNode(root).asInput())
        assertEquals(root.hash, res.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trxes[0].gid, res.data.trxes[0].gid)
        assertEquals(root.data.trxes[0].attr, res.data.trxes[0].attr)
        assertEquals(root.data.trxes[0].value, res.data.trxes[0].value)
    }

    @Test
    fun testLeaf() {
        val iid = Iid(0, 4)
        val root = Leaf(null, NodeRef(Hash(randomBytes(HASH_LEN, random))), DbUuid(iid), currentTimeMillis(), NodeData(arrayOf(Eav(Gid(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(SimpleSerialization.serializeNode(root).asInput()) as Leaf
        assertEquals(root.hash, res.hash)
        assertEquals(root.parent.hash, res.parent.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trxes[0].gid, res.data.trxes[0].gid)
        assertEquals(root.data.trxes[0].attr, res.data.trxes[0].attr)
        assertEquals(root.data.trxes[0].value, res.data.trxes[0].value)
    }

    @Test
    fun testMerge() {
        val iid = Iid(0, 4)
        val root = Merge(null, NodeRef(Hash(randomBytes(HASH_LEN, random))), NodeRef(Hash(randomBytes(HASH_LEN, random))), DbUuid(iid), currentTimeMillis(), NodeData(arrayOf(Eav(Gid(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(SimpleSerialization.serializeNode(root).asInput()) as Merge
        assertEquals(root.parent1.hash, res.parent1.hash)
        assertEquals(root.parent2.hash, res.parent2.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trxes[0].gid, res.data.trxes[0].gid)
        assertEquals(root.data.trxes[0].attr, res.data.trxes[0].attr)
        assertEquals(root.data.trxes[0].value, res.data.trxes[0].value)
    }

    @Test
    fun testZonedDateTime() {
        val zdt = ZonedDateTimes.now()
        val outZdt = deserialize(serialize(zdt).asInput())
        assertEquals(zdt, outZdt)

        val azdt = zdt.withZoneSameInstant(ZoneIds.of("Europe/Paris"))
        assertEquals(azdt, deserialize(serialize(azdt).asInput()))
    }

}