package qbit.model

import qbit.api.gid.Iid
import qbit.api.QBitException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IidTest {

    @Test
    fun test1lvlFork() {
        val iid = Iid(0, 4)
        assertEquals(0b0001, iid.fork(1).value)
        assertEquals(0b0010, iid.fork(2).value)
        assertEquals(0b0011, iid.fork(3).value)
    }

    @Test
    fun test2lvlFork() {
        val iid = Iid(0b0001, 4)
        assertEquals(0b0001_0001, iid.fork(1).value)
        assertEquals(0b0010_0001, iid.fork(2).value)
        assertEquals(0b0011_0001, iid.fork(3).value)
    }

    @Test
    fun testOverflow() {
        assertFailsWith<IllegalArgumentException> {
            Iid(0, 4).fork(16)
        }
    }

    @Test
    fun testNegative() {
        assertFailsWith<IllegalArgumentException> {
            Iid(0, 4).fork(0)
        }
    }

}