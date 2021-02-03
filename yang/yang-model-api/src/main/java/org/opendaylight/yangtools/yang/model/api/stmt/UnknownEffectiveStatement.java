/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.api.stmt;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;

@Beta
// FIXME: evaluate usefulness of this
public interface UnknownEffectiveStatement<A, D extends UnknownStatement<A>> extends EffectiveStatement<A, D> {

}
