/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.builder.impl;

import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.nodes.AbstractImmutableDataContainerNode;

public class ImmutableUnkeyedListEntryNodeBuilder
        extends AbstractImmutableDataContainerNodeBuilder<NodeIdentifier, UnkeyedListEntryNode> {

    protected ImmutableUnkeyedListEntryNodeBuilder() {

    }

    protected ImmutableUnkeyedListEntryNodeBuilder(final int sizeHint) {
        super(sizeHint);
    }

    protected ImmutableUnkeyedListEntryNodeBuilder(final ImmutableUnkeyedListEntryNode node) {
        super(node);
    }

    public static @NonNull DataContainerNodeBuilder<NodeIdentifier, UnkeyedListEntryNode> create() {
        return new ImmutableUnkeyedListEntryNodeBuilder();
    }

    public static @NonNull DataContainerNodeBuilder<NodeIdentifier, UnkeyedListEntryNode> create(
            final int sizeHint) {
        return new ImmutableUnkeyedListEntryNodeBuilder(sizeHint);
    }

    public static @NonNull DataContainerNodeBuilder<NodeIdentifier, UnkeyedListEntryNode> create(
            final UnkeyedListEntryNode node) {
        if (!(node instanceof ImmutableUnkeyedListEntryNode immutableNode)) {
            throw new UnsupportedOperationException("Cannot initialize from class " + node.getClass());
        }
        return new ImmutableUnkeyedListEntryNodeBuilder(immutableNode);
    }

    @Override
    public UnkeyedListEntryNode build() {
        return new ImmutableUnkeyedListEntryNode(getNodeIdentifier(), buildValue());
    }

    protected static final class ImmutableUnkeyedListEntryNode
            extends AbstractImmutableDataContainerNode<NodeIdentifier, UnkeyedListEntryNode>
            implements UnkeyedListEntryNode {
        ImmutableUnkeyedListEntryNode(final NodeIdentifier nodeIdentifier, final Map<NodeIdentifier, Object> children) {
            super(children, nodeIdentifier);
        }

        @Override
        protected Class<UnkeyedListEntryNode> implementedType() {
            return UnkeyedListEntryNode.class;
        }
    }
}
