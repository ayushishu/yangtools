/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.impl;

import java.util.Collection;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.tree.impl.node.TreeNode;

/**
 * Internal interface representing a modification action of a particular node. It is used by the validation code to
 * allow for a read-only view of the modification tree as we should never modify that during validation.
 */
abstract sealed class NodeModification implements Identifiable<PathArgument> permits ModifiedNode {
    /**
     * Get the type of modification.
     *
     * @return Operation type.
     */
    abstract LogicalOperation getOperation();

    /**
     * Get the original tree node to which the modification is to be applied.
     *
     * @return The original node, or {@link Optional#absent()} if the node is a new node.
     */
    // FIXME: we should not need this method
    final Optional<? extends TreeNode> getOriginal() {
        return Optional.ofNullable(original());
    }

    /**
     * Get the original tree node to which the modification is to be applied.
     *
     * @return The original node, or {@code null} if the node is a new node.
     */
    abstract @Nullable TreeNode original();

    /**
     * Get a read-only view of children nodes.
     *
     * @return Collection of all children nodes.
     */
    abstract Collection<? extends NodeModification> getChildren();

    /**
     * A shortcut to {@code getChildren().isEmpty()}.
     *
     * @return {@code} if {@link #getChildren()} is empty.
     */
    abstract boolean isEmpty();
}
