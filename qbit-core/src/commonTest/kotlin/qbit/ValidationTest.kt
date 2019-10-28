package qbit

import qbit.Scientists.extId
import qbit.api.model.Fact
import qbit.api.gid.Gid
import qbit.api.QBitException
import qbit.api.firstInstanceEid
import qbit.api.gid.nextGids
import qbit.trx.validate
import kotlin.test.Test
import kotlin.test.assertFailsWith


class ValidationTest {

    @Test
    fun `Addition of unique attribute value with different gid should lead to QbitException`() {
        assertFailsWith<QBitException> {
            val db = dbOf(Gid(0, 0).nextGids(), *(bootstrapSchema.values + testSchema).toTypedArray()).with(eCodd.toFacts())
            validate(db, listOf(Fact(Gid(pChen.id!!), extId, eCodd.externalId)))
        }
    }

    @Test
    fun `Subsequent storing of unique value for the same entity should not be treated as uniqueness violation`() {
        val db = dbOf(Gid(0, firstInstanceEid).nextGids(), *(bootstrapSchema.values + testSchema).toTypedArray()).with(eCodd.toFacts())
        validate(db, listOf(Fact(Gid(eCodd.id!!), extId, eCodd.externalId)))
    }

    @Test
    fun testCreateAndUseAttr() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(Gid(0, 0).nextGids(), *bootstrapSchema.values.toTypedArray())
        validate(db, listOf(Fact(Gid(0, 1), attr, "unique")), listOf(attr))
    }

    @Test
    fun testMultipleFactsForScalarAttr() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("scalar", true)
            val db = dbOf(Gid(0, 0).nextGids(), attr)
            validate(db, listOf(Fact(Gid(0, 1), attr, "scalar1"),
                    Fact(Gid(0, 1), attr, "scalar2")))
        }
    }

}
