/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.ri.stmt.impl.eff;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafStatement;

public final class RegularLeafEffectiveStatement extends AbstractLeafEffectiveStatement {
    public RegularLeafEffectiveStatement(final LeafStatement declared, final QName argument, final int flags,
            final ImmutableList<? extends EffectiveStatement<?, ?>> substatements) {
        super(declared, argument, flags, substatements);
    }

    public RegularLeafEffectiveStatement(final AbstractLeafEffectiveStatement originalEffective, final QName argument,
            final int flags) {
        super(originalEffective, argument, flags);
    }
}
