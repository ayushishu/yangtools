/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.builder.impl;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.valid.DataNodeContainerValidator;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;

public final class SchemaAwareImmutableContainerNodeBuilder extends ImmutableContainerNodeBuilder {
    private final DataNodeContainerValidator validator;

    SchemaAwareImmutableContainerNodeBuilder(final ContainerLike schema) {
        this.validator = new DataNodeContainerValidator(schema);
        super.withNodeIdentifier(NodeIdentifier.create(schema.getQName()));
    }

    SchemaAwareImmutableContainerNodeBuilder(final ContainerLike schema, final ImmutableContainerNode node) {
        super(node);
        this.validator = new DataNodeContainerValidator(schema);
        super.withNodeIdentifier(NodeIdentifier.create(schema.getQName()));
    }

    @Override
    public DataContainerNodeBuilder<NodeIdentifier, ContainerNode> withNodeIdentifier(
            final NodeIdentifier withNodeIdentifier) {
        throw new UnsupportedOperationException("Node identifier created from schema");
    }

    @Override
    public DataContainerNodeBuilder<NodeIdentifier, ContainerNode> withChild(final DataContainerChild child) {
        validator.validateChild(child.getIdentifier());
        return super.withChild(child);
    }

    @Override
    public ContainerNode build() {
        // TODO check when statements... somewhere
        return super.build();
    }
}
