/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.spi;

import com.google.common.annotations.Beta;
import java.io.Serial;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.UnknownStatement;
import org.opendaylight.yangtools.yang.parser.spi.meta.StatementNamespace;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContextUtils;

/**
 * Statement local namespace, which holds direct schema node descendants. This corresponds to the contents of the schema
 * tree as exposed through {@link SchemaTreeAwareEffectiveStatement}.
 */
// FIXME: 7.0.0: this contract seems to fall on the reactor side of things rather than parser-spi. Consider moving this
//               into yang-(parser-)reactor-api.
@Beta
public final class SchemaTreeNamespace<D extends DeclaredStatement<QName>, E extends SchemaTreeEffectiveStatement<D>>
        extends StatementNamespace<QName, D, E> {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final @NonNull SchemaTreeNamespace<?, ?> INSTANCE = new SchemaTreeNamespace<>();

    private SchemaTreeNamespace() {
        super("schemaTree");
    }

    @SuppressWarnings("unchecked")
    public static <D extends DeclaredStatement<QName>, E extends SchemaTreeEffectiveStatement<D>>
            @NonNull SchemaTreeNamespace<D, E> instance() {
        return (SchemaTreeNamespace<D, E>) INSTANCE;
    }

    /**
     * Find statement context identified by interpreting specified {@link SchemaNodeIdentifier} starting at specified
     * {@link StmtContext}.
     *
     * @param root Search root context
     * @param identifier {@link SchemaNodeIdentifier} relative to search root
     * @return Matching statement context, if present.
     * @throws NullPointerException if any of the arguments is null
     */
    public static Optional<StmtContext<?, ?, ?>> findNode(final StmtContext<?, ?, ?> root,
            final SchemaNodeIdentifier identifier) {
        final Iterator<QName> iterator = identifier.getNodeIdentifiers().iterator();
        if (!iterator.hasNext()) {
            return Optional.of(root);
        }

        QName nextPath = iterator.next();
        StmtContext<?, ?, ?> current = root.getFromNamespace(SchemaTreeNamespace.INSTANCE, nextPath);
        if (current == null) {
            return Optional.ofNullable(tryToFindUnknownStatement(nextPath.getLocalName(), root));
        }
        while (current != null && iterator.hasNext()) {
            nextPath = iterator.next();
            final StmtContext<?, ?, ?> nextNodeCtx = current.getFromNamespace(SchemaTreeNamespace.INSTANCE, nextPath);
            if (nextNodeCtx == null) {
                return Optional.ofNullable(tryToFindUnknownStatement(nextPath.getLocalName(), current));
            }
            current = nextNodeCtx;
        }
        return Optional.ofNullable(current);
    }

    @SuppressWarnings("unchecked")
    private static StmtContext<?, ?, ?> tryToFindUnknownStatement(final String localName,
            final StmtContext<?, ?, ?> current) {
        final Collection<? extends StmtContext<?, ?, ?>> unknownSubstatements = StmtContextUtils.findAllSubstatements(
            current, UnknownStatement.class);
        for (final StmtContext<?, ?, ?> unknownSubstatement : unknownSubstatements) {
            if (localName.equals(unknownSubstatement.rawArgument())) {
                return unknownSubstatement;
            }
        }
        return null;
    }
}
