/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.stmt.reactor;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Verify;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.util.RecursiveObjectLeaker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.YangVersion;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.meta.IdentifierNamespace;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.StatementParserMode;
import org.opendaylight.yangtools.yang.parser.spi.meta.DerivedNamespaceBehaviour;
import org.opendaylight.yangtools.yang.parser.spi.meta.ModelProcessingPhase;
import org.opendaylight.yangtools.yang.parser.spi.meta.MutableStatement;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.NamespaceStorageNode;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.Registry;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.StorageNodeType;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceNotAvailableException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.meta.SomeModifiersUnresolvedException;
import org.opendaylight.yangtools.yang.parser.spi.meta.StatementSupport;
import org.opendaylight.yangtools.yang.parser.spi.meta.StatementSupportBundle;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContextUtils;
import org.opendaylight.yangtools.yang.parser.spi.source.ModuleCtxToModuleQName;
import org.opendaylight.yangtools.yang.parser.spi.source.ModulesDeviatedByModules;
import org.opendaylight.yangtools.yang.parser.spi.source.ModulesDeviatedByModules.SupportedModules;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.source.SupportedFeaturesNamespace;
import org.opendaylight.yangtools.yang.parser.spi.source.SupportedFeaturesNamespace.SupportedFeatures;
import org.opendaylight.yangtools.yang.parser.spi.validation.ValidationBundlesNamespace;
import org.opendaylight.yangtools.yang.parser.spi.validation.ValidationBundlesNamespace.ValidationBundleType;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.SourceSpecificContext.PhaseCompletionProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BuildGlobalContext extends NamespaceStorageSupport implements Registry {
    private static final Logger LOG = LoggerFactory.getLogger(BuildGlobalContext.class);

    private static final ModelProcessingPhase[] PHASE_EXECUTION_ORDER = {
        ModelProcessingPhase.SOURCE_PRE_LINKAGE,
        ModelProcessingPhase.SOURCE_LINKAGE,
        ModelProcessingPhase.STATEMENT_DEFINITION,
        ModelProcessingPhase.FULL_DECLARATION,
        ModelProcessingPhase.EFFECTIVE_MODEL
    };

    private final Table<YangVersion, QName, StatementDefinitionContext<?, ?, ?>> definitions = HashBasedTable.create();
    private final Map<QName, StatementDefinitionContext<?, ?, ?>> modelDefinedStmtDefs = new HashMap<>();
    private final Map<Class<?>, NamespaceBehaviourWithListeners<?, ?, ?>> supportedNamespaces = new HashMap<>();
    private final List<MutableStatement> mutableStatementsToSeal = new ArrayList<>();
    private final ImmutableMap<ModelProcessingPhase, StatementSupportBundle> supports;
    private final Set<SourceSpecificContext> sources = new HashSet<>();
    private final ImmutableSet<YangVersion> supportedVersions;
    private final boolean enabledSemanticVersions;

    private Set<SourceSpecificContext> libSources = new HashSet<>();
    private ModelProcessingPhase currentPhase = ModelProcessingPhase.INIT;
    private ModelProcessingPhase finishedPhase = ModelProcessingPhase.INIT;

    BuildGlobalContext(final ImmutableMap<ModelProcessingPhase, StatementSupportBundle> supports,
            final ImmutableMap<ValidationBundleType, Collection<?>> supportedValidation,
            final StatementParserMode statementParserMode) {
        this.supports = requireNonNull(supports, "BuildGlobalContext#supports cannot be null");

        switch (statementParserMode) {
            case DEFAULT_MODE:
                enabledSemanticVersions = false;
                break;
            case SEMVER_MODE:
                enabledSemanticVersions = true;
                break;
            default:
                throw new IllegalArgumentException("Unhandled parser mode " + statementParserMode);
        }

        addToNamespace(ValidationBundlesNamespace.class, supportedValidation);

        this.supportedVersions = ImmutableSet.copyOf(supports.get(ModelProcessingPhase.INIT).getSupportedVersions());
    }

    boolean isEnabledSemanticVersioning() {
        return enabledSemanticVersions;
    }

    StatementSupportBundle getSupportsForPhase(final ModelProcessingPhase phase) {
        return supports.get(phase);
    }

    void addSource(final @NonNull StatementStreamSource source) {
        sources.add(new SourceSpecificContext(this, source));
    }

    void addLibSource(final @NonNull StatementStreamSource libSource) {
        checkState(!isEnabledSemanticVersioning(),
            "Library sources are not supported in semantic version mode currently.");
        checkState(currentPhase == ModelProcessingPhase.INIT,
                "Add library source is allowed in ModelProcessingPhase.INIT only");
        libSources.add(new SourceSpecificContext(this, libSource));
    }

    void setSupportedFeatures(final Set<QName> supportedFeatures) {
        addToNamespace(SupportedFeaturesNamespace.class, SupportedFeatures.SUPPORTED_FEATURES,
                    ImmutableSet.copyOf(supportedFeatures));
    }

    void setModulesDeviatedByModules(final SetMultimap<QNameModule, QNameModule> modulesDeviatedByModules) {
        addToNamespace(ModulesDeviatedByModules.class, SupportedModules.SUPPORTED_MODULES,
                    ImmutableSetMultimap.copyOf(modulesDeviatedByModules));
    }

    @Override
    public StorageNodeType getStorageNodeType() {
        return StorageNodeType.GLOBAL;
    }

    @Override
    public NamespaceStorageNode getParentNamespaceStorage() {
        return null;
    }

    @Override
    public NamespaceBehaviour.Registry getBehaviourRegistry() {
        return this;
    }

    @Override
    public <K, V, N extends IdentifierNamespace<K, V>> NamespaceBehaviourWithListeners<K, V, N> getNamespaceBehaviour(
            final Class<N> type) {
        NamespaceBehaviourWithListeners<?, ?, ?> potential = supportedNamespaces.get(type);
        if (potential == null) {
            final NamespaceBehaviour<K, V, N> potentialRaw = supports.get(currentPhase).getNamespaceBehaviour(type);
            if (potentialRaw != null) {
                potential = createNamespaceContext(potentialRaw);
                supportedNamespaces.put(type, potential);
            } else {
                throw new NamespaceNotAvailableException("Namespace " + type + " is not available in phase "
                        + currentPhase);
            }
        }

        Verify.verify(type.equals(potential.getIdentifier()));
        /*
         * Safe cast, previous checkState checks equivalence of key from which
         * type argument are derived
         */
        return (NamespaceBehaviourWithListeners<K, V, N>) potential;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <K, V, N extends IdentifierNamespace<K, V>> NamespaceBehaviourWithListeners<K, V, N> createNamespaceContext(
            final NamespaceBehaviour<K, V, N> potentialRaw) {
        if (potentialRaw instanceof DerivedNamespaceBehaviour) {
            final VirtualNamespaceContext derivedContext = new VirtualNamespaceContext(
                    (DerivedNamespaceBehaviour) potentialRaw);
            getNamespaceBehaviour(((DerivedNamespaceBehaviour) potentialRaw).getDerivedFrom()).addDerivedNamespace(
                    derivedContext);
            return derivedContext;
        }
        return new SimpleNamespaceContext<>(potentialRaw);
    }

    StatementDefinitionContext<?, ?, ?> getStatementDefinition(final YangVersion version, final QName name) {
        StatementDefinitionContext<?, ?, ?> potential = definitions.get(version, name);
        if (potential == null) {
            final StatementSupport<?, ?, ?> potentialRaw = supports.get(currentPhase).getStatementDefinition(version,
                    name);
            if (potentialRaw != null) {
                potential = new StatementDefinitionContext<>(potentialRaw);
                definitions.put(version, name, potential);
            }
        }
        return potential;
    }

    StatementDefinitionContext<?, ?, ?> getModelDefinedStatementDefinition(final QName name) {
        return modelDefinedStmtDefs.get(name);
    }

    void putModelDefinedStatementDefinition(final QName name, final StatementDefinitionContext<?, ?, ?> def) {
        modelDefinedStmtDefs.put(name, def);
    }

    private void executePhases() throws ReactorException {
        for (final ModelProcessingPhase phase : PHASE_EXECUTION_ORDER) {
            startPhase(phase);
            loadPhaseStatements();
            completePhaseActions();
            endPhase(phase);
        }
    }

    ReactorDeclaredModel build() throws ReactorException {
        executePhases();
        return transform();
    }

    EffectiveSchemaContext buildEffective() throws ReactorException {
        executePhases();
        return transformEffective();
    }

    private ReactorDeclaredModel transform() {
        checkState(finishedPhase == ModelProcessingPhase.EFFECTIVE_MODEL);
        final List<DeclaredStatement<?>> rootStatements = new ArrayList<>(sources.size());
        for (final SourceSpecificContext source : sources) {
            rootStatements.add(source.getRoot().buildDeclared());
        }
        return new ReactorDeclaredModel(rootStatements);
    }

    private SomeModifiersUnresolvedException propagateException(final SourceSpecificContext source,
            final RuntimeException cause) throws SomeModifiersUnresolvedException {
        final SourceIdentifier sourceId = createSourceIdentifier(source.getRoot());
        if (!(cause instanceof SourceException)) {
            /*
             * This should not be happening as all our processing should provide SourceExceptions.
             * We will wrap the exception to provide enough information to identify the problematic model,
             * but also emit a warning so the offending codepath will get fixed.
             */
            LOG.warn("Unexpected error processing source {}. Please file an issue with this model attached.",
                sourceId, cause);
        }

        throw new SomeModifiersUnresolvedException(currentPhase, sourceId, cause);
    }

    private static SourceIdentifier createSourceIdentifier(final StmtContext<?, ?, ?> root) {
        final QNameModule qNameModule = root.getFromNamespace(ModuleCtxToModuleQName.class, root);
        final String arg = root.coerceRawStatementArgument();
        if (qNameModule != null) {
            // creates SourceIdentifier for a module
            return RevisionSourceIdentifier.create(arg, qNameModule.getRevision());
        }

        // creates SourceIdentifier for a submodule
        return RevisionSourceIdentifier.create(arg, StmtContextUtils.getLatestRevision(root.declaredSubstatements()));
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private EffectiveSchemaContext transformEffective() throws ReactorException {
        checkState(finishedPhase == ModelProcessingPhase.EFFECTIVE_MODEL);
        final List<DeclaredStatement<?>> rootStatements = new ArrayList<>(sources.size());
        final List<EffectiveStatement<?, ?>> rootEffectiveStatements = new ArrayList<>(sources.size());

        try {
            for (final SourceSpecificContext source : sources) {
                final RootStatementContext<?, ?, ?> root = source.getRoot();
                try {
                    rootStatements.add(root.buildDeclared());
                    rootEffectiveStatements.add(root.buildEffective());
                } catch (final RuntimeException ex) {
                    throw propagateException(source, ex);
                }
            }
        } finally {
            RecursiveObjectLeaker.cleanup();
        }

        sealMutableStatements();
        return EffectiveSchemaContext.create(rootStatements, rootEffectiveStatements);
    }

    private void startPhase(final ModelProcessingPhase phase) {
        checkState(Objects.equals(finishedPhase, phase.getPreviousPhase()));
        startPhaseFor(phase, sources);
        startPhaseFor(phase, libSources);

        currentPhase = phase;
        LOG.debug("Global phase {} started", phase);
    }

    private static void startPhaseFor(final ModelProcessingPhase phase, final Set<SourceSpecificContext> sources) {
        for (final SourceSpecificContext source : sources) {
            source.startPhase(phase);
        }
    }

    private void loadPhaseStatements() throws ReactorException {
        checkState(currentPhase != null);
        loadPhaseStatementsFor(sources);
        loadPhaseStatementsFor(libSources);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private void loadPhaseStatementsFor(final Set<SourceSpecificContext> srcs) throws ReactorException {
        for (final SourceSpecificContext source : srcs) {
            try {
                source.loadStatements();
            } catch (final RuntimeException ex) {
                throw propagateException(source, ex);
            }
        }
    }

    private SomeModifiersUnresolvedException addSourceExceptions(final List<SourceSpecificContext> sourcesToProgress) {
        boolean addedCause = false;
        SomeModifiersUnresolvedException buildFailure = null;
        for (final SourceSpecificContext failedSource : sourcesToProgress) {
            final Optional<SourceException> optSourceEx = failedSource.failModifiers(currentPhase);
            if (optSourceEx.isEmpty()) {
                continue;
            }

            final SourceException sourceEx = optSourceEx.get();
            // Workaround for broken logging implementations which ignore
            // suppressed exceptions
            final Throwable cause = sourceEx.getCause() != null ? sourceEx.getCause() : sourceEx;
            if (LOG.isDebugEnabled()) {
                LOG.error("Failed to parse YANG from source {}", failedSource, sourceEx);
            } else {
                LOG.error("Failed to parse YANG from source {}: {}", failedSource, cause.getMessage());
            }

            final Throwable[] suppressed = sourceEx.getSuppressed();
            if (suppressed.length > 0) {
                LOG.error("{} additional errors reported:", suppressed.length);

                int count = 1;
                for (final Throwable t : suppressed) {
                    LOG.error("Error {}: {}", count, t.getMessage());
                    count++;
                }
            }

            if (!addedCause) {
                addedCause = true;
                final SourceIdentifier sourceId = createSourceIdentifier(failedSource.getRoot());
                buildFailure = new SomeModifiersUnresolvedException(currentPhase, sourceId, sourceEx);
            } else {
                buildFailure.addSuppressed(sourceEx);
            }
        }
        return buildFailure;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private void completePhaseActions() throws ReactorException {
        checkState(currentPhase != null);
        final List<SourceSpecificContext> sourcesToProgress = new ArrayList<>(sources);
        if (!libSources.isEmpty()) {
            checkState(currentPhase == ModelProcessingPhase.SOURCE_PRE_LINKAGE,
                    "Yang library sources should be empty after ModelProcessingPhase.SOURCE_PRE_LINKAGE, "
                            + "but current phase was %s", currentPhase);
            sourcesToProgress.addAll(libSources);
        }

        boolean progressing = true;
        while (progressing) {
            // We reset progressing to false.
            progressing = false;
            final Iterator<SourceSpecificContext> currentSource = sourcesToProgress.iterator();
            while (currentSource.hasNext()) {
                final SourceSpecificContext nextSourceCtx = currentSource.next();
                try {
                    final PhaseCompletionProgress sourceProgress = nextSourceCtx.tryToCompletePhase(currentPhase);
                    switch (sourceProgress) {
                        case FINISHED:
                            currentSource.remove();
                            // we were able to make progress in computation
                            progressing = true;
                            break;
                        case PROGRESS:
                            progressing = true;
                            break;
                        case NO_PROGRESS:
                            // Noop
                            break;
                        default:
                            throw new IllegalStateException("Unsupported phase progress " + sourceProgress);
                    }
                } catch (final RuntimeException ex) {
                    throw propagateException(nextSourceCtx, ex);
                }
            }
        }

        if (!libSources.isEmpty()) {
            final Set<SourceSpecificContext> requiredLibs = getRequiredSourcesFromLib();
            sources.addAll(requiredLibs);
            libSources = ImmutableSet.of();
            /*
             * We want to report errors of relevant sources only, so any others can
             * be removed.
             */
            sourcesToProgress.retainAll(sources);
        }

        if (!sourcesToProgress.isEmpty()) {
            final SomeModifiersUnresolvedException buildFailure = addSourceExceptions(sourcesToProgress);
            if (buildFailure != null) {
                throw buildFailure;
            }
        }
    }

    private Set<SourceSpecificContext> getRequiredSourcesFromLib() {
        checkState(currentPhase == ModelProcessingPhase.SOURCE_PRE_LINKAGE,
                "Required library sources can be collected only in ModelProcessingPhase.SOURCE_PRE_LINKAGE phase,"
                        + " but current phase was %s", currentPhase);
        final TreeBasedTable<String, Optional<Revision>, SourceSpecificContext> libSourcesTable = TreeBasedTable.create(
            String::compareTo, Revision::compare);
        for (final SourceSpecificContext libSource : libSources) {
            final SourceIdentifier libSourceIdentifier = requireNonNull(libSource.getRootIdentifier());
            libSourcesTable.put(libSourceIdentifier.getName(), libSourceIdentifier.getRevision(), libSource);
        }

        final Set<SourceSpecificContext> requiredLibs = new HashSet<>();
        for (final SourceSpecificContext source : sources) {
            collectRequiredSourcesFromLib(libSourcesTable, requiredLibs, source);
            removeConflictingLibSources(source, requiredLibs);
        }
        return requiredLibs;
    }

    private void collectRequiredSourcesFromLib(
            final TreeBasedTable<String, Optional<Revision>, SourceSpecificContext> libSourcesTable,
            final Set<SourceSpecificContext> requiredLibs, final SourceSpecificContext source) {
        for (final SourceIdentifier requiredSource : source.getRequiredSources()) {
            final SourceSpecificContext libSource = getRequiredLibSource(requiredSource, libSourcesTable);
            if (libSource != null && requiredLibs.add(libSource)) {
                collectRequiredSourcesFromLib(libSourcesTable, requiredLibs, libSource);
            }
        }
    }

    private static SourceSpecificContext getRequiredLibSource(final SourceIdentifier requiredSource,
            final TreeBasedTable<String, Optional<Revision>, SourceSpecificContext> libSourcesTable) {
        return requiredSource.getRevision().isPresent()
                ? libSourcesTable.get(requiredSource.getName(), requiredSource.getRevision())
                        : getLatestRevision(libSourcesTable.row(requiredSource.getName()));
    }

    private static SourceSpecificContext getLatestRevision(final SortedMap<Optional<Revision>,
            SourceSpecificContext> sourceMap) {
        return sourceMap != null && !sourceMap.isEmpty() ? sourceMap.get(sourceMap.lastKey()) : null;
    }

    // removes required library sources which would cause namespace/name conflict with one of the main sources
    // later in the parsing process. this can happen if we add a parent module or a submodule as a main source
    // and the same parent module or submodule is added as one of the library sources.
    // such situation may occur when using the yang-system-test artifact - if a parent module/submodule is specified
    // as its argument and the same dir is specified as one of the library dirs through -p option).
    private static void removeConflictingLibSources(final SourceSpecificContext source,
            final Set<SourceSpecificContext> requiredLibs) {
        final Iterator<SourceSpecificContext> requiredLibsIter = requiredLibs.iterator();
        while (requiredLibsIter.hasNext()) {
            final SourceSpecificContext currentReqSource = requiredLibsIter.next();
            if (source.getRootIdentifier().equals(currentReqSource.getRootIdentifier())) {
                requiredLibsIter.remove();
            }
        }
    }

    private void endPhase(final ModelProcessingPhase phase) {
        checkState(currentPhase == phase);
        finishedPhase = currentPhase;
        LOG.debug("Global phase {} finished", phase);
    }

    Set<SourceSpecificContext> getSources() {
        return sources;
    }

    public Set<YangVersion> getSupportedVersions() {
        return supportedVersions;
    }

    void addMutableStmtToSeal(final MutableStatement mutableStatement) {
        mutableStatementsToSeal.add(mutableStatement);
    }

    void sealMutableStatements() {
        for (final MutableStatement mutableStatement : mutableStatementsToSeal) {
            mutableStatement.seal();
        }
        mutableStatementsToSeal.clear();
    }
}
