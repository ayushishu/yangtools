/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.InputSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.type.TypeDefinitions;

public class AugmentTest {
    private static final QNameModule FOO = QNameModule.create(
        XMLNamespace.of("urn:opendaylight.foo"), Revision.of("2013-10-13"));
    private static final QNameModule BAR = QNameModule.create(
        XMLNamespace.of("urn:opendaylight.bar"), Revision.of("2013-10-14"));
    private static final QNameModule BAZ = QNameModule.create(
        XMLNamespace.of("urn:opendaylight.baz"), Revision.of("2013-10-15"));

    private static final QName Q0 = QName.create(BAR, "interfaces");
    private static final QName Q1 = QName.create(BAR, "ifEntry");
    private static final QName Q2 = QName.create(BAZ, "augment-holder");

    @Test
    public void testAugmentParsing() throws Exception {
        final SchemaContext context = TestUtils.loadModules(getClass().getResource("/augment-test/augment-in-augment")
            .toURI());

        // foo.yang
        final Module module1 = TestUtils.findModule(context, "foo").get();
        Collection<? extends AugmentationSchemaNode> augmentations = module1.getAugmentations();
        assertEquals(1, augmentations.size());
        final AugmentationSchemaNode augment = augmentations.iterator().next();
        assertNotNull(augment);

        assertEquals(Absolute.of(Q0, Q1, Q2), augment.getTargetPath());

        final Collection<? extends DataSchemaNode> augmentChildren = augment.getChildNodes();
        assertEquals(4, augmentChildren.size());
        for (final DataSchemaNode dsn : augmentChildren) {
            TestUtils.checkIsAugmenting(dsn, false);
        }

        final LeafSchemaNode ds0ChannelNumber = (LeafSchemaNode) augment.getDataChildByName(QName.create(
                module1.getQNameModule(), "ds0ChannelNumber"));
        final LeafSchemaNode interfaceId = (LeafSchemaNode) augment.getDataChildByName(QName.create(
                module1.getQNameModule(), "interface-id"));
        final ContainerSchemaNode schemas = (ContainerSchemaNode) augment.getDataChildByName(QName.create(
                module1.getQNameModule(), "schemas"));
        final ChoiceSchemaNode odl = (ChoiceSchemaNode) augment.getDataChildByName(QName.create(
                module1.getQNameModule(), "odl"));

        assertNotNull(ds0ChannelNumber);
        assertNotNull(interfaceId);
        assertNotNull(schemas);
        assertNotNull(odl);

        // leaf ds0ChannelNumber
        assertEquals(QName.create(FOO, "ds0ChannelNumber"), ds0ChannelNumber.getQName());
        assertFalse(ds0ChannelNumber.isAugmenting());
        // type of leaf ds0ChannelNumber
        assertEquals(TypeDefinitions.STRING, ds0ChannelNumber.getType().getQName());

        // leaf interface-id
        assertEquals(QName.create(FOO, "interface-id"), interfaceId.getQName());
        assertFalse(interfaceId.isAugmenting());

        // container schemas
        assertEquals(QName.create(FOO, "schemas"), schemas.getQName());
        assertFalse(schemas.isAugmenting());

        // choice odl
        assertEquals(QName.create(FOO, "odl"), odl.getQName());
        assertFalse(odl.isAugmenting());

        // baz.yang
        final Module module3 = TestUtils.findModule(context, "baz").get();
        augmentations = module3.getAugmentations();
        assertEquals(3, augmentations.size());
        AugmentationSchemaNode augment1 = null;
        AugmentationSchemaNode augment2 = null;
        AugmentationSchemaNode augment3 = null;
        for (final AugmentationSchemaNode as : augmentations) {
            if (!as.getWhenCondition().isPresent()) {
                augment3 = as;
            } else if ("br:ifType='ds0'".equals(as.getWhenCondition().orElseThrow().toString())) {
                augment1 = as;
            } else if ("br:ifType='ds2'".equals(as.getWhenCondition().orElseThrow().toString())) {
                augment2 = as;
            }
        }
        assertNotNull(augment1);
        assertNotNull(augment2);
        assertNotNull(augment3);

        assertEquals(1, augment1.getChildNodes().size());
        final ContainerSchemaNode augmentHolder = (ContainerSchemaNode) augment1.getDataChildByName(QName.create(
                module3.getQNameModule(), "augment-holder"));
        assertNotNull(augmentHolder);

        assertEquals(1, augment2.getChildNodes().size());
        final ContainerSchemaNode augmentHolder2 = (ContainerSchemaNode) augment2.getDataChildByName(QName.create(
                module3.getQNameModule(), "augment-holder2"));
        assertNotNull(augmentHolder2);

        assertEquals(1, augment3.getChildNodes().size());
        final CaseSchemaNode pause = (CaseSchemaNode) augment3.getDataChildByName(QName.create(
                module3.getQNameModule(), "pause"));
        assertNotNull(pause);
    }

    @Test
    public void testAugmentResolving() throws Exception {
        final SchemaContext context = TestUtils.loadModules(getClass().getResource("/augment-test/augment-in-augment")
            .toURI());
        final Module module2 = TestUtils.findModule(context, "bar").get();
        final ContainerSchemaNode interfaces = (ContainerSchemaNode) module2.getDataChildByName(QName.create(
                module2.getQNameModule(), "interfaces"));
        final ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName(QName.create(
                module2.getQNameModule(), "ifEntry"));

        // baz.yang
        // augment "/br:interfaces/br:ifEntry" {
        final ContainerSchemaNode augmentHolder = (ContainerSchemaNode) ifEntry.getDataChildByName(QName.create(BAZ,
                "augment-holder"));
        TestUtils.checkIsAugmenting(augmentHolder, true);
        assertEquals(Q2, augmentHolder.getQName());

        // foo.yang
        // augment "/br:interfaces/br:ifEntry/bz:augment-holder"
        final LeafSchemaNode ds0ChannelNumber = (LeafSchemaNode) augmentHolder.getDataChildByName(QName.create(FOO,
                "ds0ChannelNumber"));
        final LeafSchemaNode interfaceId = (LeafSchemaNode) augmentHolder.getDataChildByName(QName.create(FOO,
                "interface-id"));
        final ContainerSchemaNode schemas = (ContainerSchemaNode) augmentHolder.getDataChildByName(QName.create(FOO,
                "schemas"));
        final ChoiceSchemaNode odl = (ChoiceSchemaNode) augmentHolder.getDataChildByName(QName.create(FOO, "odl"));

        assertNotNull(ds0ChannelNumber);
        assertNotNull(interfaceId);
        assertNotNull(schemas);
        assertNotNull(odl);

        // leaf ds0ChannelNumber
        assertEquals(QName.create(FOO, "ds0ChannelNumber"), ds0ChannelNumber.getQName());

        // leaf interface-id
        assertEquals(QName.create(FOO, "interface-id"), interfaceId.getQName());

        // container schemas
        assertEquals(QName.create(FOO, "schemas"), schemas.getQName());

        // choice odl
        assertEquals(QName.create(FOO, "odl"), odl.getQName());
    }

    @Test
    public void testAugmentedChoice() throws Exception {
        final SchemaContext context = TestUtils.loadModules(getClass().getResource("/augment-test/augment-in-augment")
            .toURI());
        final Module module2 = TestUtils.findModule(context, "bar").get();
        final ContainerSchemaNode interfaces = (ContainerSchemaNode) module2.getDataChildByName(QName.create(
                module2.getQNameModule(), "interfaces"));
        final ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName(QName.create(
                module2.getQNameModule(), "ifEntry"));
        final ContainerSchemaNode augmentedHolder = (ContainerSchemaNode) ifEntry.getDataChildByName(QName.create(
                BAZ, "augment-holder"));
        TestUtils.checkIsAugmenting(augmentedHolder, true);

        // foo.yang
        // augment "/br:interfaces/br:ifEntry/bz:augment-holder"
        final ChoiceSchemaNode odl = (ChoiceSchemaNode) augmentedHolder.getDataChildByName(QName.create(FOO, "odl"));
        assertNotNull(odl);
        final Collection<? extends CaseSchemaNode> cases = odl.getCases();
        assertEquals(4, cases.size());

        CaseSchemaNode id = null;
        CaseSchemaNode node1 = null;
        CaseSchemaNode node2 = null;
        CaseSchemaNode node3 = null;

        for (final CaseSchemaNode ccn : cases) {
            if ("id".equals(ccn.getQName().getLocalName())) {
                id = ccn;
            } else if ("node1".equals(ccn.getQName().getLocalName())) {
                node1 = ccn;
            } else if ("node2".equals(ccn.getQName().getLocalName())) {
                node2 = ccn;
            } else if ("node3".equals(ccn.getQName().getLocalName())) {
                node3 = ccn;
            }
        }

        assertNotNull(id);
        assertNotNull(node1);
        assertNotNull(node2);
        assertNotNull(node3);

        // case id
        assertEquals(QName.create(FOO, "id"), id.getQName());
        final Collection<? extends DataSchemaNode> idChildren = id.getChildNodes();
        assertEquals(1, idChildren.size());

        // case node1
        assertEquals(QName.create(FOO, "node1"), node1.getQName());
        final Collection<? extends DataSchemaNode> node1Children = node1.getChildNodes();
        assertTrue(node1Children.isEmpty());

        // case node2
        assertEquals(QName.create(FOO, "node2"), node2.getQName());
        final Collection<? extends DataSchemaNode> node2Children = node2.getChildNodes();
        assertTrue(node2Children.isEmpty());

        // case node3
        assertEquals(QName.create(FOO, "node3"), node3.getQName());
        final Collection<? extends DataSchemaNode> node3Children = node3.getChildNodes();
        assertEquals(1, node3Children.size());

        // test cases
        // case id child
        final LeafSchemaNode caseIdChild = (LeafSchemaNode) idChildren.iterator().next();
        assertNotNull(caseIdChild);
        assertEquals(QName.create(FOO, "id"), caseIdChild.getQName());

        // case node3 child
        final ContainerSchemaNode caseNode3Child = (ContainerSchemaNode) node3Children.iterator().next();
        assertNotNull(caseNode3Child);
        assertEquals(QName.create(FOO, "node3"), caseNode3Child.getQName());
    }

    @Test
    public void testAugmentRpc() throws Exception {
        final SchemaContext context = TestUtils.loadModules(getClass().getResource("/augment-test/rpc").toURI());
        final XMLNamespace NS_BAR = XMLNamespace.of("urn:opendaylight:bar");
        final XMLNamespace NS_FOO = XMLNamespace.of("urn:opendaylight:foo");
        final Revision revision = Revision.of("2013-10-11");
        final Module bar = TestUtils.findModule(context, "bar").get();
        final Collection<? extends RpcDefinition> rpcs = bar.getRpcs();
        assertEquals(2, rpcs.size());

        RpcDefinition submit = null;
        for (final RpcDefinition rpc : rpcs) {
            if ("submit".equals(rpc.getQName().getLocalName())) {
                submit = rpc;
                break;
            }
        }
        assertNotNull(submit);

        final QName submitQName = QName.create(NS_BAR, revision, "submit");
        assertEquals(submitQName, submit.getQName());
        final InputSchemaNode input = submit.getInput();
        final QName inputQName = QName.create(NS_BAR, revision, "input");
        assertEquals(inputQName, input.getQName());
        final ChoiceSchemaNode arguments = (ChoiceSchemaNode) input.getDataChildByName(QName.create(NS_BAR, revision,
                "arguments"));
        final QName argumentsQName = QName.create(NS_BAR, revision, "arguments");
        assertEquals(argumentsQName, arguments.getQName());
        assertFalse(arguments.isAugmenting());
        final Collection<? extends CaseSchemaNode> cases = arguments.getCases();
        assertEquals(3, cases.size());

        CaseSchemaNode attach = null;
        CaseSchemaNode create = null;
        CaseSchemaNode destroy = null;
        for (final CaseSchemaNode child : cases) {
            if ("attach".equals(child.getQName().getLocalName())) {
                attach = child;
            } else if ("create".equals(child.getQName().getLocalName())) {
                create = child;
            } else if ("destroy".equals(child.getQName().getLocalName())) {
                destroy = child;
            }
        }
        assertNotNull(attach);
        assertNotNull(create);
        assertNotNull(destroy);

        assertTrue(attach.isAugmenting());
        assertTrue(create.isAugmenting());
        assertTrue(destroy.isAugmenting());

        // case attach
        assertEquals(QName.create(NS_FOO, revision, "attach"), attach.getQName());
        final Collection<? extends DataSchemaNode> attachChildren = attach.getChildNodes();
        assertEquals(1, attachChildren.size());

        // case create
        assertEquals(QName.create(NS_FOO, revision, "create"), create.getQName());
        final Collection<? extends DataSchemaNode> createChildren = create.getChildNodes();
        assertEquals(1, createChildren.size());

        // case attach
        assertEquals(QName.create(NS_FOO, revision, "destroy"), destroy.getQName());
        final Collection<? extends DataSchemaNode> destroyChildren = destroy.getChildNodes();
        assertEquals(1, destroyChildren.size());
    }

    @Test
    public void testAugmentInUsesResolving() throws Exception {
        final SchemaContext context = TestUtils.loadModules(getClass().getResource("/augment-test/augment-in-uses")
            .toURI());
        assertEquals(1, context.getModules().size());

        final Module test = context.getModules().iterator().next();
        final DataNodeContainer links = (DataNodeContainer) test.getDataChildByName(QName.create(test.getQNameModule(),
                "links"));
        final DataNodeContainer link = (DataNodeContainer) links.getDataChildByName(QName.create(test.getQNameModule(),
                "link"));
        final DataNodeContainer nodes = (DataNodeContainer) link.getDataChildByName(QName.create(test.getQNameModule(),
                "nodes"));
        final ContainerSchemaNode node = (ContainerSchemaNode) nodes.getDataChildByName(QName.create(
                test.getQNameModule(), "node"));
        final Collection<? extends AugmentationSchemaNode> augments = node.getAvailableAugmentations();
        assertEquals(1, augments.size());
        assertEquals(1, node.getChildNodes().size());
        final LeafSchemaNode id = (LeafSchemaNode) node.getDataChildByName(QName.create(test.getQNameModule(), "id"));
        assertTrue(id.isAugmenting());
    }
}
