// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

import com.github.ajalt.mordant.input.KeyboardEvent
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Every keyboard binding of an application, by context [C], resolving to its own action type [A].
 * The caller says which contexts are active, in precedence order. [layers] is copied and validated
 * when the map is built: chords are unique within a layer, every group reference resolves (a
 * sectionless layer's to exactly one titled layer), and every group not [HintGroup.bindingless] is
 * bound. Actions and chords must keep stable equality.
 */
public class KeyMap<C : Any, A : Any>(layers: Map<C, KeyLayer<A>>) {

    private val layers: Map<C, KeyLayer<A>> = snapshot(layers)

    init {
        validate()
    }

    /** Returns a declared layer, or [IllegalArgumentException] for an undeclared context. */
    private fun layer(context: C): KeyLayer<A> =
        layers[context] ?: throw IllegalArgumentException("No key layer declared for context $context")

    /**
     * The action bound to [event] by the first layer in [active] that binds its chord, else null.
     * Every context in [active] must be declared; an unknown active context is an argument error.
     */
    public fun resolve(active: List<C>, event: KeyboardEvent): A? {
        for (context in active) {
            val hit = layer(context).bindings.firstOrNull { it.chord == event }
            if (hit != null) return hit.action
        }
        return null
    }

    /**
     * [context]'s help section, one row per declared [HintGroup], in declaration order.
     * Sectionless contexts have no help section and cause [IllegalArgumentException].
     */
    public fun hints(context: C): KeySection {
        val layer = layer(context)
        val title = layer.title
            ?: throw IllegalArgumentException("Context $context declares no title and renders no help section")
        return KeySection(title, layer.hintGroups.map { KeyHint(it.label, it.description) })
    }

    /** Every chord bound to [action], across every layer. */
    public fun chordsFor(action: A): List<KeyboardEvent> =
        layers.values.flatMap { layer -> layer.bindings.filter { it.action == action }.map { it.chord } }

    private fun validate(): Unit {
        val titledOwners = linkedMapOf<String, MutableList<C>>()

        for ((context, layer) in layers) {
            validateLayer(context, layer)
            if (layer.title != null) {
                for (group in layer.hintGroups) {
                    titledOwners.getOrPut(group.id) { mutableListOf() }.add(context)
                }
            }
        }

        val creditedGroups = mutableSetOf<Pair<C, String>>()
        for ((context, layer) in layers) {
            for (binding in layer.bindings) {
                if (layer.title != null) {
                    require(layer.hintGroups.any { it.id == binding.hintGroup }) {
                        "Key layer '$context' binding for ${binding.chord} references hint group " +
                            "'${binding.hintGroup}', which is not declared by that titled layer"
                    }
                    creditedGroups += context to binding.hintGroup
                } else {
                    val owners = titledOwners[binding.hintGroup].orEmpty()
                    require(owners.size == 1) {
                        val ownerText = when {
                            owners.isEmpty() -> "no titled layer"
                            else -> "titled layers ${owners.joinToString()}"
                        }
                        "Sectionless key layer '$context' binding for ${binding.chord} references hint " +
                            "group '${binding.hintGroup}', but it resolves to $ownerText"
                    }
                    creditedGroups += owners.single() to binding.hintGroup
                }
            }
        }

        for ((context, layer) in layers) {
            for (group in layer.hintGroups) {
                require(group.bindingless || context to group.id in creditedGroups) {
                    "Key layer '$context' hint group '${group.id}' is orphaned; add a binding credit " +
                        "or set bindingless=true"
                }
            }
        }
    }

    private fun validateLayer(context: C, layer: KeyLayer<A>): Unit {
        val duplicateChords = layer.bindings.groupBy { it.chord }.filterValues { it.size > 1 }.keys
        require(duplicateChords.isEmpty()) {
            "Key layer '$context' contains duplicate exact chord(s): ${duplicateChords.joinToString()}"
        }

        val groupIds = layer.hintGroups.map { it.id }
        for (group in layer.hintGroups) {
            require(group.id.isNotBlank()) {
                "Key layer '$context' has a hint group with a blank id"
            }
        }
        require(groupIds.size == groupIds.toSet().size) {
            "Key layer '$context' contains duplicate hint group id(s): ${groupIds.joinToString()}"
        }
        for (binding in layer.bindings) {
            require(binding.hintGroup.isNotBlank()) {
                "Key layer '$context' binding for ${binding.chord} has a blank hint group reference"
            }
        }
    }

    private fun snapshot(source: Map<C, KeyLayer<A>>): Map<C, KeyLayer<A>> {
        val copied = LinkedHashMap<C, KeyLayer<A>>(source.size)
        for ((context, layer) in source) {
            copied[context] = KeyLayer(
                title = layer.title,
                bindings = Collections.unmodifiableList(ArrayList(layer.bindings)),
                hintGroups = Collections.unmodifiableList(ArrayList(layer.hintGroups)),
            )
        }
        return Collections.unmodifiableMap(copied)
    }
}
