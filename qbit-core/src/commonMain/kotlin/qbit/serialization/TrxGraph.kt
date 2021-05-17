package qbit.serialization

import kotlinx.coroutines.flow.*
import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid

class NodeData(val trxes: Array<out Eav>)

sealed class Node<out H : Hash?>(val hash: H)

class NodeRef(hash: Hash) : Node<Hash>(hash)

sealed class NodeVal<out H : Hash?>(hash: H, val source: DbUuid, val timestamp: Long, val data: NodeData) :
    Node<H>(hash)

class Root<out H : Hash?>(hash: H, source: DbUuid, timestamp: Long, data: NodeData) :
    NodeVal<H>(hash, source, timestamp, data)

class Leaf<out H : Hash?>(hash: H, val parent: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) :
    NodeVal<H>(hash, source, timestamp, data)

class Merge<out H : Hash?>(
        hash: H,
        val base: Node<Hash>,
        val parent1: Node<Hash>,
        val parent2: Node<Hash>,
        source: DbUuid,
        timestamp: Long,
        data: NodeData
) :
    NodeVal<H>(hash, source, timestamp, data)

internal suspend fun FlowCollector<NodeVal<Hash>>.impl(
    base: Node<Hash>?, head: Node<Hash>,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>?
) {
    when (head) {
        base -> return
        is Root -> return
        is Leaf -> {
            impl(base, head.parent, resolveNode)
            emit(head)
        }
        is Merge -> {
            // nodes should be emitted in order of ascending distance from root
            // so if there is no base in merge branches, then nodes before merge base should be emitted
            // therefore it's necessary to check is branches flows contains base before emitting them
            val firstBranch = flow { impl(head.base, head.parent1, resolveNode) }
            val secondBranch = flow { impl(head.base, head.parent2, resolveNode) }
            val notFoundInFirstBranch = firstBranch.firstOrNull { it == base } == null
            val notFoundInSecondBranch = !notFoundInFirstBranch &&
                    secondBranch.firstOrNull { it == base } == null
            if (notFoundInFirstBranch && notFoundInSecondBranch) {
                // emit nodes before split, if branches do not contain base
                impl(base, head.base, resolveNode)
            }
            if (notFoundInSecondBranch) {
                // emit first branch, if head not in second branch (i.e. in first branch or before split)
                emitAll(firstBranch)
            }
            if (notFoundInFirstBranch) {
                // emit second branch, if head not in second branch (i.e. in first branch or before split)
                emitAll(secondBranch)
            }
            // emit merge node always
            emit(head)
        }
        is NodeRef -> impl(
            base,
            resolveNode(head) ?: throw QBitException("Corrupted trx graph, cannot resolve $head"),
            resolveNode
        )
    }
}