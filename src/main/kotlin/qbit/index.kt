package qbit

import qbit.collections.BTree

private fun loadFacts(graph: Graph, head: NodeVal<Hash>, untilDbUuid: DbUuid?): ArrayList<Fact> {
    if (untilDbUuid != null && head.source == untilDbUuid) {
        return arrayListOf()
    }
    return when (head) {
        is Root -> head.data.trx.toCollection(ArrayList())
        is Leaf -> with(loadFacts(graph, graph.resolveNode(head.parent), untilDbUuid)) {
            addAll(head.data.trx.toCollection(ArrayList()))
            this
        }
        is Merge -> {
            val p1 = graph.resolveNode(head.parent1)
            val first = loadFacts(graph, p1, untilDbUuid)
            val second = loadFacts(graph, graph.resolveNode(head.parent2), p1.source)
            first.addAll(second)
            first
        }
    }
}

fun Index(graph: Graph, head: NodeVal<Hash>): Index {
    return Index().add(loadFacts(graph, head, null))
}

fun Index(facts: List<Fact>): Index =
        Index(BTree(eavtCmp), BTree(avetCmp))
                .add(facts)

val eidCmp = Comparator<Fact> { o1, o2 -> o1.eid.compareTo(o2.eid) }
val attrCmp = Comparator<Fact> { o1, o2 -> o1.attr.compareTo(o2.attr) }
val valueCmp = Comparator<Fact> { o1, o2 -> compareValues(o1, o2) }

fun composeComparators(vararg cmps: Comparator<Fact>): Comparator<Fact> = Comparator { o1: Fact, o2: Fact ->
    cmps.asSequence()
            .map { it.compare(o1, o2) }
            .dropWhile { it == 0 }
            .firstOrNull() ?: 0
}

fun eidPattern(eid: EID) = { other: Fact -> other.eid.compareTo(eid) }

fun attrPattern(attr: String) = { fact: Fact -> fact.attr.compareTo(attr) }

fun eidAttrPattern(eid: EID, attr: String) = composeComparable(eidPattern(eid), attrPattern(attr))

fun valuePattern(value: Any) = { fact: Fact -> compareValues(fact.value, value) }

fun attrValuePattern(attr: String, value: Any) = composeComparable(attrPattern(attr), valuePattern(value))

fun composeComparable(vararg cmps: (Fact) -> Int) = { fact: Fact ->
    cmps.asSequence()
            .map { it(fact) }
            .dropWhile { it == 0 }
            .firstOrNull() ?: 0
}

val eavtCmp = composeComparators(eidCmp, attrCmp)

val avetCmp = composeComparators(attrCmp, valueCmp, eidCmp)

fun compareValues(f1: Fact, f2: Fact) =
        compareValues(f1.value, f2.value)

fun compareValues(v1: Any, v2: Any): Int {
    val type = DataType.of(v1) ?: throw IllegalArgumentException("Unsupported type: $v1")
    return type.compare(v1, v2)
}

class Index(
        val eavt: BTree<Fact> = BTree(eavtCmp),
        val avet: BTree<Fact> = BTree(avetCmp)
) {

    fun add(facts: List<Fact>): Index {
        var newEavt = eavt
        var newAvet = avet

        // add only last values
        val distinctFacts = facts
                .reversed()
                .distinctBy { it.eid to it.attr }

        for (f in distinctFacts) {
            val toRemove = newEavt.select(eidAttrPattern(f.eid, f.attr)).asSequence().firstOrNull()
            if (toRemove != null) {
                newEavt = newEavt.remove(toRemove)
                newAvet = newAvet.remove(toRemove)
            }
        }
        newEavt = newEavt.addAll(distinctFacts)
        newAvet = newAvet.addAll(distinctFacts)

        return Index(newEavt, newAvet)
    }

    fun add(fact: Fact): Index {
        return add(listOf(fact))
    }

    fun entityById(eid: EID): Map<String, Any>? {
        try {
            val facts = eavt.select(eidPattern(eid))
            val grouped = facts
                    .asSequence()
                    .groupBy { it.attr }
            val mapped = grouped
                    .mapValues { it.value.last().value }
            return mapped
                    .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun eidsByPred(pred: QueryPred): Set<EID> {
        return avet.select {
            if (it.attr == pred.attrName) {
                pred.compareTo(it.value)
            } else {
                it.attr.compareTo(pred.attrName)
            }
        }
                .asSequence()
                .map { it.eid }
                .toSet()
    }

}
