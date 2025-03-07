/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.EventListener;
import org.junit.jupiter.api.Test;

class ListenerRegistryTest {
    private final ExtendedTestEventListener extendedTestEventListener = new ExtendedTestEventListener() {};
    private final ListenerRegistry<TestEventListener> registry = ListenerRegistry.create();

    @Test
    void testCreateNewInstance() {
        assertNotNull(registry, "Intance of listener registry should not be null.");
    }

    @Test
    void testGetListenersMethod() {
        assertEquals(0, registry.streamListeners().count(), "Listener registry should not have any listeners.");
    }

    @Test
    void testRegisterMethod() {
        final var listenerRegistration = registry.register(extendedTestEventListener);
        assertEquals(extendedTestEventListener, listenerRegistration.getInstance(), "Listeners should be the same.");
    }

    interface TestEventListener extends EventListener {

    }

    interface ExtendedTestEventListener extends TestEventListener {

    }
}
