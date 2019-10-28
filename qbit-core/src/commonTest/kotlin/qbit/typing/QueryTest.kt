package qbit.typing

import qbit.Scientist
import qbit.Scientists
import qbit.query.GraphQuery
import kotlin.test.Test
import kotlin.test.assertTrue


class QueryTest {

    @Test
    fun `Graph query should fetch refs for mandatory properties`() {
        val q = GraphQuery(Scientist::class, emptyMap())
        assertTrue(q.shouldFetch(Scientists.country))
    }

}