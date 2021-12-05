/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.ri.stmt.impl.eff;

import com.google.common.collect.ImmutableList;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafListStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ReferenceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.StatusEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.TypeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.UnitsEffectiveStatement;
import org.opendaylight.yangtools.yang.model.ri.type.ConcreteTypeBuilder;
import org.opendaylight.yangtools.yang.model.ri.type.ConcreteTypes;
import org.opendaylight.yangtools.yang.model.spi.meta.AbstractDeclaredEffectiveStatement;
import org.opendaylight.yangtools.yang.model.spi.meta.EffectiveStatementMixins.DataSchemaNodeMixin;
import org.opendaylight.yangtools.yang.model.spi.meta.EffectiveStatementMixins.MustConstraintMixin;
import org.opendaylight.yangtools.yang.model.spi.meta.EffectiveStatementMixins.UserOrderedMixin;

abstract class AbstractLeafListEffectiveStatement
        extends AbstractDeclaredEffectiveStatement.Default<QName, LeafListStatement>
        implements LeafListEffectiveStatement, LeafListSchemaNode, UserOrderedMixin<QName, LeafListStatement>,
            DataSchemaNodeMixin<LeafListStatement>, MustConstraintMixin<QName, LeafListStatement> {
    private static final VarHandle TYPE;

    static {
        try {
            TYPE = MethodHandles.lookup().findVarHandle(AbstractLeafListEffectiveStatement.class, "type",
                TypeDefinition.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @NonNull Object substatements;
    private final int flags;

    @SuppressWarnings("unused")
    private volatile TypeDefinition<?> type;

    AbstractLeafListEffectiveStatement(final LeafListStatement declared, final int flags,
            final ImmutableList<? extends EffectiveStatement<?, ?>> substatements) {
        super(declared);
        this.substatements = maskList(substatements);
        this.flags = flags;
    }

    AbstractLeafListEffectiveStatement(final AbstractLeafListEffectiveStatement original, final int flags) {
        super(original);
        substatements = original.substatements;
        this.flags = flags;
    }

    @Override
    public final ImmutableList<? extends EffectiveStatement<?, ?>> effectiveSubstatements() {
        return unmaskList(substatements);
    }

    @Override
    public final int flags() {
        return flags;
    }

    @Override
    public final boolean isUserOrdered() {
        return userOrdered();
    }

    @Override
    public final LeafListEffectiveStatement asEffectiveStatement() {
        return this;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + argument() + "]";
    }

    @Override
    public final TypeDefinition<?> getType() {
        final var local = (TypeDefinition<?>) TYPE.getAcquire(this);
        return local != null ? local : loadType();
    }

    private TypeDefinition<?> loadType() {
        final TypeEffectiveStatement<?> typeStmt = findFirstEffectiveSubstatement(TypeEffectiveStatement.class).get();
        final ConcreteTypeBuilder<?> builder = ConcreteTypes.concreteTypeBuilder(typeStmt.getTypeDefinition(),
            getQName());
        for (final EffectiveStatement<?, ?> stmt : effectiveSubstatements()) {
            // NOTE: 'default' is omitted here on purpose
            if (stmt instanceof DescriptionEffectiveStatement) {
                builder.setDescription(((DescriptionEffectiveStatement)stmt).argument());
            } else if (stmt instanceof ReferenceEffectiveStatement) {
                builder.setReference(((ReferenceEffectiveStatement)stmt).argument());
            } else if (stmt instanceof StatusEffectiveStatement) {
                builder.setStatus(((StatusEffectiveStatement)stmt).argument());
            } else if (stmt instanceof UnitsEffectiveStatement) {
                builder.setUnits(((UnitsEffectiveStatement)stmt).argument());
            }
        }

        final var ret = builder.build();
        final var witness = (TypeDefinition<?>) TYPE.compareAndExchangeRelease(this, null, ret);
        return witness != null ? witness : ret;
    }
}
