package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.model.Entity
import qbit.model.eq
import qbit.ns.Namespace
import qbit.model.DataType
import qbit.model.ScalarAttr
import qbit.storage.FileSystemStorage
import java.nio.file.Files

class SyncTest {

    @Test
    fun testSync() {
        val db1Storage = FileSystemStorage(Files.createTempDirectory("qbit-db1-"))
        val conn1 = qbit(db1Storage)
        val _attr1 = ScalarAttr(Namespace("user")["attr1"], DataType.QString)
        conn1.persist(_attr1)
        val _attr2 = ScalarAttr(Namespace("user")["attr2"], DataType.QString)
        conn1.persist(_attr2)

        val (id, head) = conn1.fork()

        val db2Storage = FileSystemStorage(Files.createTempDirectory("qbit-db2-"), db1Storage)
        val conn2 = LocalConn(id, db2Storage, head)
        conn2.fetch(conn1)

        val e1 = Entity(_attr1 eq "value1")
        val se1 = conn1.persist(e1).storedEntity()

        val e2 = Entity(_attr2 eq "value2")
        val se2 = conn2.persist(e2).storedEntity()

        conn2.sync(conn1)
        assertEquals("value1", conn1.db.pull(se1.eid)!![_attr1])
        assertEquals("value2", conn1.db.pull(se2.eid)!![_attr2])
        assertEquals("value1", conn2.db.pull(se1.eid)!![_attr1])
        assertEquals("value2", conn2.db.pull(se2.eid)!![_attr2])
    }

}
