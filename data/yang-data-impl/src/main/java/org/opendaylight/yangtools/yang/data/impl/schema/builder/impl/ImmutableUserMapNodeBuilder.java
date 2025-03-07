/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.builder.impl;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.util.UnmodifiableCollection;
import org.opendaylight.yangtools.util.UnmodifiableMap;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UserMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.spi.node.AbstractNormalizedNode;

public class ImmutableUserMapNodeBuilder implements CollectionNodeBuilder<MapEntryNode, UserMapNode> {
    private static final int DEFAULT_CAPACITY = 4;

    private Map<NodeIdentifierWithPredicates, MapEntryNode> value;
    private NodeIdentifier nodeIdentifier;
    private boolean dirty;

    ImmutableUserMapNodeBuilder() {
        value = new LinkedHashMap<>(DEFAULT_CAPACITY);
        dirty = false;
    }

    private ImmutableUserMapNodeBuilder(final int sizeHint) {
        if (sizeHint >= 0) {
            value = new LinkedHashMap<>(sizeHint + sizeHint / 3);
        } else {
            value = new LinkedHashMap<>(DEFAULT_CAPACITY);
        }
        dirty = false;
    }

    private ImmutableUserMapNodeBuilder(final ImmutableUserMapNode node) {
        nodeIdentifier = node.name();
        value = node.children;
        dirty = true;
    }

    public static @NonNull CollectionNodeBuilder<MapEntryNode, UserMapNode> create() {
        return new ImmutableUserMapNodeBuilder();
    }

    public static @NonNull CollectionNodeBuilder<MapEntryNode, UserMapNode> create(final int sizeHint) {
        return new ImmutableUserMapNodeBuilder(sizeHint);
    }

    public static @NonNull CollectionNodeBuilder<MapEntryNode, UserMapNode> create(final UserMapNode node) {
        if (!(node instanceof ImmutableUserMapNode immutableNode)) {
            throw new UnsupportedOperationException("Cannot initialize from class " + node.getClass());
        }
        return new ImmutableUserMapNodeBuilder(immutableNode);
    }

    private void checkDirty() {
        if (dirty) {
            value = new LinkedHashMap<>(value);
            dirty = false;
        }
    }

    @Override
    public CollectionNodeBuilder<MapEntryNode, UserMapNode> withChild(final MapEntryNode child) {
        checkDirty();
        value.put(child.name(), child);
        return this;
    }

    @Override
    public CollectionNodeBuilder<MapEntryNode, UserMapNode> withoutChild(final PathArgument key) {
        checkDirty();
        value.remove(key);
        return this;
    }

    @Override
    public CollectionNodeBuilder<MapEntryNode, UserMapNode> withValue(final Collection<MapEntryNode> withValue) {
        // TODO replace or putAll ?
        for (final MapEntryNode mapEntryNode : withValue) {
            withChild(mapEntryNode);
        }

        return this;
    }

    @Override
    public CollectionNodeBuilder<MapEntryNode, UserMapNode> withNodeIdentifier(
            final NodeIdentifier withNodeIdentifier) {
        nodeIdentifier = withNodeIdentifier;
        return this;
    }

    @Override
    public UserMapNode build() {
        dirty = true;
        return new ImmutableUserMapNode(nodeIdentifier, value);
    }

    @Override
    public CollectionNodeBuilder<MapEntryNode, UserMapNode> addChild(
            final MapEntryNode child) {
        return withChild(child);
    }


    @Override
    public NormalizedNodeContainerBuilder<NodeIdentifier, PathArgument, MapEntryNode, UserMapNode> removeChild(
            final PathArgument key) {
        return withoutChild(key);
    }

    protected static final class ImmutableUserMapNode
            extends AbstractNormalizedNode<NodeIdentifier, UserMapNode> implements UserMapNode {
        private final Map<NodeIdentifierWithPredicates, MapEntryNode> children;

        ImmutableUserMapNode(final NodeIdentifier nodeIdentifier,
                         final Map<NodeIdentifierWithPredicates, MapEntryNode> children) {
            super(nodeIdentifier);
            this.children = children;
        }

        @Override
        public MapEntryNode childByArg(final NodeIdentifierWithPredicates child) {
            return children.get(child);
        }

        @Override
        public MapEntryNode childAt(final int position) {
            return Iterables.get(children.values(), position);
        }

        @Override
        public int size() {
            return children.size();
        }

        @Override
        public Collection<MapEntryNode> body() {
            return UnmodifiableCollection.create(children.values());
        }

        @Override
        public Map<NodeIdentifierWithPredicates, MapEntryNode> asMap() {
            return UnmodifiableMap.of(children);
        }

        @Override
        protected Class<UserMapNode> implementedType() {
            return UserMapNode.class;
        }

        @Override
        protected int valueHashCode() {
            // Order is important
            int hashCode = 1;
            for (MapEntryNode child : children.values()) {
                hashCode = 31 * hashCode + child.hashCode();
            }
            return hashCode;
        }

        @Override
        protected boolean valueEquals(final UserMapNode other) {
            final var otherChildren = other instanceof ImmutableUserMapNode immutableOther ? immutableOther.children
                : other.asMap();
            return Iterables.elementsEqual(children.values(), otherChildren.values());
        }
    }
}
