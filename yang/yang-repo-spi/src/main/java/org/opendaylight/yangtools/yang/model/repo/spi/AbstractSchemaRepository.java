/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.repo.spi;

import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFailedFluentFuture;

import com.google.common.annotations.Beta;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link SchemaRepository} implementations. It handles registration and lookup of schema
 * sources, subclasses need only to provide their own {@link #createEffectiveModelContextFactory()} implementation.
 */
@Beta
public abstract class AbstractSchemaRepository implements SchemaRepository, SchemaSourceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSchemaRepository.class);

    /*
     * Source identifier -> representation -> provider map. We usually are looking for
     * a specific representation of a source.
     */
    @GuardedBy("this")
    private final Map<SourceIdentifier, ListMultimap<Class<? extends SchemaSourceRepresentation>,
            AbstractSchemaSourceRegistration<?>>> sources = new HashMap<>();

    /*
     * Schema source listeners.
     */
    @GuardedBy("this")
    private final List<SchemaListenerRegistration> listeners = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static <T extends SchemaSourceRepresentation> ListenableFuture<T> fetchSource(
            final SourceIdentifier id, final Iterator<AbstractSchemaSourceRegistration<?>> it) {
        final AbstractSchemaSourceRegistration<?> reg = it.next();

        return Futures.catchingAsync(((SchemaSourceProvider<T>)reg.getProvider()).getSource(id), Throwable.class,
            input -> {
                LOG.debug("Failed to acquire source from {}", reg, input);

                if (it.hasNext()) {
                    return fetchSource(id, it);
                }

                throw new MissingSchemaSourceException("All available providers exhausted", id, input);
            }, MoreExecutors.directExecutor());
    }

    @Override
    public <T extends SchemaSourceRepresentation> ListenableFuture<T> getSchemaSource(final SourceIdentifier id,
            final Class<T> representation) {
        final ArrayList<AbstractSchemaSourceRegistration<?>> sortedSchemaSourceRegistrations;

        synchronized (this) {
            final ListMultimap<Class<? extends SchemaSourceRepresentation>, AbstractSchemaSourceRegistration<?>> srcs =
                sources.get(id);
            if (srcs == null) {
                return immediateFailedFluentFuture(new MissingSchemaSourceException(
                    "No providers registered for source " + id, id));
            }

            sortedSchemaSourceRegistrations = Lists.newArrayList(srcs.get(representation));
        }

        // TODO, remove and make sources keep sorted multimap (e.g. ArrayListMultimap with SortedLists)
        sortedSchemaSourceRegistrations.sort(SchemaProviderCostComparator.INSTANCE);

        final Iterator<AbstractSchemaSourceRegistration<?>> regs = sortedSchemaSourceRegistrations.iterator();
        if (!regs.hasNext()) {
            return immediateFailedFluentFuture(new MissingSchemaSourceException(
                        "No providers for source " + id + " representation " + representation + " available", id));
        }

        final ListenableFuture<T> fetchSourceFuture = fetchSource(id, regs);
        // Add callback to notify cache listeners about encountered schema
        Futures.addCallback(fetchSourceFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                for (final SchemaListenerRegistration listener : listeners) {
                    listener.getInstance().schemaSourceEncountered(result);
                }
            }

            @Override
            @SuppressWarnings("checkstyle:parameterName")
            public void onFailure(final Throwable t) {
                LOG.trace("Skipping notification for encountered source {}, fetching source failed", id, t);
            }
        }, MoreExecutors.directExecutor());

        return fetchSourceFuture;
    }

    private synchronized <T extends SchemaSourceRepresentation> void addSource(final PotentialSchemaSource<T> source,
            final AbstractSchemaSourceRegistration<T> reg) {
        ListMultimap<Class<? extends SchemaSourceRepresentation>, AbstractSchemaSourceRegistration<?>> map =
            sources.get(source.getSourceIdentifier());
        if (map == null) {
            map = ArrayListMultimap.create();
            sources.put(source.getSourceIdentifier(), map);
        }

        map.put(source.getRepresentation(), reg);

        final Collection<PotentialSchemaSource<?>> reps = Collections.singleton(source);
        for (SchemaListenerRegistration l : listeners) {
            l.getInstance().schemaSourceRegistered(reps);
        }
    }

    private synchronized <T extends SchemaSourceRepresentation> void removeSource(final PotentialSchemaSource<?> source,
            final SchemaSourceRegistration<?> reg) {
        final Multimap<Class<? extends SchemaSourceRepresentation>, AbstractSchemaSourceRegistration<?>> m =
            sources.get(source.getSourceIdentifier());
        if (m != null) {
            m.remove(source.getRepresentation(), reg);

            for (SchemaListenerRegistration l : listeners) {
                l.getInstance().schemaSourceUnregistered(source);
            }

            if (m.isEmpty()) {
                sources.remove(source.getSourceIdentifier());
            }
        }
    }

    @Override
    public <T extends SchemaSourceRepresentation> SchemaSourceRegistration<T> registerSchemaSource(
            final SchemaSourceProvider<? super T> provider, final PotentialSchemaSource<T> source) {
        final PotentialSchemaSource<T> src = source.cachedReference();

        final AbstractSchemaSourceRegistration<T> ret = new AbstractSchemaSourceRegistration<>(provider, src) {
            @Override
            protected void removeRegistration() {
                removeSource(src, this);
            }
        };

        addSource(src, ret);
        return ret;
    }

    @Override
    public SchemaListenerRegistration registerSchemaSourceListener(final SchemaSourceListener listener) {
        final SchemaListenerRegistration ret = new AbstractSchemaListenerRegistration(listener) {
            @Override
            protected void removeRegistration() {
                listeners.remove(this);
            }
        };

        synchronized (this) {
            final Collection<PotentialSchemaSource<?>> col = new ArrayList<>();
            for (Multimap<Class<? extends SchemaSourceRepresentation>, AbstractSchemaSourceRegistration<?>> m
                    : sources.values()) {
                for (AbstractSchemaSourceRegistration<?> r : m.values()) {
                    col.add(r.getInstance());
                }
            }

            // Notify first, so translator-type listeners, who react by registering a source
            // do not cause infinite loop.
            listener.schemaSourceRegistered(col);
            listeners.add(ret);
        }
        return ret;
    }

    private static final class SchemaProviderCostComparator implements Comparator<AbstractSchemaSourceRegistration<?>>,
            Serializable {
        static final SchemaProviderCostComparator INSTANCE = new SchemaProviderCostComparator();

        @java.io.Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final AbstractSchemaSourceRegistration<?> o1, final AbstractSchemaSourceRegistration<?> o2) {
            return o1.getInstance().getCost() - o2.getInstance().getCost();
        }

        @java.io.Serial
        private Object readResolve() {
            return INSTANCE;
        }
    }
}
