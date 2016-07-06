/**
 * This work was created by participants in the DataONE project, and is jointly copyrighted by participating
 * institutions in DataONE. For more information on DataONE, see our web site at http://dataone.org.
 *
 * Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * $Id$
 */
package org.dataone.cn.ldap.v1;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.dataone.exceptions.MarshallingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.ServiceMethodRestriction;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.WhitespaceWildcardsFilter;

import org.springframework.stereotype.Component;

/**
 *
 * @author waltz
 */
@Component
@Qualifier("nodeLdapPopulation")
public class NodeLdapPopulation {

    public static List<Node> testNodeList = new ArrayList<Node>();
    public static Log log = LogFactory.getLog(NodeLdapPopulation.class);
    private String primarySubject = Settings.getConfiguration().getString("testIdentity.primarySubject");

    final static int SIZE = 16384;
    
    static {
        // Need this or context will lowercase all the rdn s
        System.setProperty(DistinguishedName.KEY_CASE_FOLD_PROPERTY, DistinguishedName.KEY_CASE_FOLD_NONE);
    }
    @Autowired
    @Qualifier("ldapTemplate")
    private LdapTemplate ldapTemplate;

    public void populateTestMNs() {
        try {

        Node sq1dMNNode = buildTestNode("/org/dataone/cn/resources/samples/v1/mnNodeTest1.xml");
        searchAndDestroyNode(sq1dMNNode.getIdentifier().getValue());       

        // because we use a base DN, only need to supply the RDN
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc", "dataone");
        dn.add("cn", sq1dMNNode.getIdentifier().getValue());

        DirContextAdapter context = new DirContextAdapter(dn);
        mapNodeToContext(sq1dMNNode, context);
        ldapTemplate.bind(dn, context, null);

        for (Service service : sq1dMNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc", "dataone");
            dnService.add("cn", sq1dMNNode.getIdentifier().getValue());
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sq1dMNNode.getIdentifier().getValue(), d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
        }

        testNodeList.add(sq1dMNNode);
               
        Node sqR1MNNode =  buildTestNode("/org/dataone/cn/resources/samples/v1/mnNodeTest3.xml");
        searchAndDestroyNode(sqR1MNNode.getIdentifier().getValue());
        
        // because we use a base DN, only need to supply the RDN
        dn = new DistinguishedName();
        dn.add("dc", "dataone");
        dn.add("cn", sqR1MNNode.getIdentifier().getValue());

        context = new DirContextAdapter(dn);
        mapNodeToContext(sqR1MNNode, context);
        ldapTemplate.bind(dn, context, null);
        for (Service service : sqR1MNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc", "dataone");
            dnService.add("cn", sqR1MNNode.getIdentifier().getValue());
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sqR1MNNode.getIdentifier().getValue(), d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
        }
        testNodeList.add(sqR1MNNode);
        
        

        Node sq1shMNNode =  buildTestNode("/org/dataone/cn/resources/samples/v1/mnNodeTest2.xml");
        searchAndDestroyNode(sq1shMNNode.getIdentifier().getValue());
       
        // because we use a base DN, only need to supply the RDN
        dn = new DistinguishedName();
        dn.add("dc", "dataone");
        dn.add("cn", sq1shMNNode.getIdentifier().getValue());

        context = new DirContextAdapter(dn);
        mapNodeToContext(sq1shMNNode, context);
        ldapTemplate.bind(dn, context, null);
        for (Service service : sq1shMNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc", "dataone");
            dnService.add("cn", sq1shMNNode.getIdentifier().getValue());
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sq1shMNNode.getIdentifier().getValue(), d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
        }
        testNodeList.add(sq1shMNNode);
        } catch (Exception ex) {
            ex.printStackTrace();
            deletePopulatedNodes();
        }
    }

    public void populateTestCN() {
        try {
        Node sqrmCNNode =  buildTestNode("/org/dataone/cn/resources/samples/v1/cnNodeTest1.xml");
        searchAndDestroyNode(sqrmCNNode.getIdentifier().getValue());
                Subject primeSubject = new Subject();
                primeSubject.setValue(primarySubject);
        sqrmCNNode.addSubject(primeSubject);
        // because we use a base DN, only need to supply the RDN
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc", "dataone");
        dn.add("cn", sqrmCNNode.getIdentifier().getValue());

        DirContextAdapter context = new DirContextAdapter(dn);
        mapNodeToContext(sqrmCNNode, context);
        ldapTemplate.bind(dn, context, null);

        for (Service service : sqrmCNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            log.info("sqrm adding service " + d1NodeServiceId);
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc", "dataone");
            dnService.add("cn", sqrmCNNode.getIdentifier().getValue());
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sqrmCNNode.getIdentifier().getValue(), d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
            if (service.getName().equalsIgnoreCase("CNIdentity")) {
                ServiceMethodRestriction restrict = new ServiceMethodRestriction();
                restrict.setMethodName("mapIdentity");
                restrict.addSubject(primeSubject);
                DistinguishedName dnServiceRestriction = new DistinguishedName();
                dnServiceRestriction.add("dc", "dataone");
                dnServiceRestriction.add("cn", sqrmCNNode.getIdentifier().getValue());
                dnServiceRestriction.add("d1NodeServiceId", d1NodeServiceId);
                dnServiceRestriction.add("d1ServiceMethodName", restrict.getMethodName());
                log.info("sqrm adding restriction " + restrict.getMethodName());
                context = new DirContextAdapter(dnServiceRestriction);
                mapServiceMethodRestriction(restrict, sqrmCNNode.getIdentifier().getValue(), d1NodeServiceId, context);
                ldapTemplate.bind(dnServiceRestriction, context, null);
            }
        }

        testNodeList.add(sqrmCNNode);
        } catch (Exception ex) {
            ex.printStackTrace();
            deletePopulatedNodes();
        }
    }
    
    protected void mapNodeToContext(Node node, DirContextOperations context) {

        context.setAttributeValue("objectclass", "device");
        context.setAttributeValue("objectclass", "d1Node");
        context.setAttributeValue("cn", node.getIdentifier().getValue());
        context.setAttributeValue("d1NodeId", node.getIdentifier().getValue());
        context.setAttributeValue("d1NodeName", node.getName());
        context.setAttributeValue("d1NodeDescription", node.getDescription());
        context.setAttributeValue("d1NodeBaseURL", node.getBaseURL());
        context.setAttributeValue("d1NodeReplicate", Boolean.toString(node.isReplicate()).toUpperCase());
        context.setAttributeValue("d1NodeSynchronize", Boolean.toString(node.isSynchronize()).toUpperCase());
        context.setAttributeValue("d1NodeType", node.getType().xmlValue());
        context.setAttributeValue("d1NodeState", node.getState().xmlValue());
        context.setAttributeValue("d1NodeApproved", Boolean.toString(Boolean.TRUE).toUpperCase());
        if ((node.getSubjectList() != null) && !(node.getSubjectList().isEmpty())) {
            context.setAttributeValue("subject", node.getSubject(0).getValue());
        }
        if (node.getType().compareTo(NodeType.MN) == 0) {
            if (node.isSynchronize() && node.getSynchronization() != null) {
                context.setAttributeValue("d1NodeSynSchdSec", node.getSynchronization().getSchedule().getSec());
                context.setAttributeValue("d1NodeSynSchdMin", node.getSynchronization().getSchedule().getMin());
                context.setAttributeValue("d1NodeSynSchdHour", node.getSynchronization().getSchedule().getHour());
                context.setAttributeValue("d1NodeSynSchdMday", node.getSynchronization().getSchedule().getMday());
                context.setAttributeValue("d1NodeSynSchdMon", node.getSynchronization().getSchedule().getMon());
                context.setAttributeValue("d1NodeSynSchdWday", node.getSynchronization().getSchedule().getWday());
                context.setAttributeValue("d1NodeSynSchdYear", node.getSynchronization().getSchedule().getYear());
                context.setAttributeValue("d1NodeLastHarvested", "1900-01-01T00:00:00Z");
                context.setAttributeValue("d1NodeLastCompleteHarvest", "1900-01-01T00:00:00Z");
            }
        }
        context.setAttributeValue("d1NodeContactSubject", node.getContactSubject(0).getValue());
    }

    protected void mapServiceToContext(org.dataone.service.types.v1.Service service, String nodeId, String nodeServiceId, DirContextOperations context) {
        context.setAttributeValue("objectclass", "d1NodeService");
        context.setAttributeValue("d1NodeServiceId", nodeServiceId);
        context.setAttributeValue("d1NodeId", nodeId);

        context.setAttributeValue("d1NodeServiceName", service.getName());
        context.setAttributeValue("d1NodeServiceVersion", service.getVersion());
        context.setAttributeValue("d1NodeServiceAvailable", Boolean.toString(service.getAvailable()).toUpperCase());
    }

    protected void mapServiceMethodRestriction(ServiceMethodRestriction restrict, String nodeId, String nodeServiceId, DirContextOperations context) {

        context.setAttributeValue("objectclass", "d1ServiceMethodRestriction");
        context.setAttributeValue("d1NodeServiceId", nodeServiceId);
        context.setAttributeValue("d1NodeId", nodeId);
        context.setAttributeValue("d1ServiceMethodName", restrict.getMethodName());

        if (restrict.getSubjectList() != null && !(restrict.getSubjectList().isEmpty())) {
            for (Subject subject : restrict.getSubjectList()) {
                context.setAttributeValue("d1AllowedSubject", subject.getValue());
            }
        }
    }

    public void deletePopulatedNodes() {
        for (Node node : testNodeList) {
            searchAndDestroyNode(node.getIdentifier().getValue());
        }
        testNodeList.clear();
    }

    /*
     * if a test fails, then LDAP testing artifacts are sometimes left. So, we clean them up before we attempt testing
     * again
     *
     */

    private void searchAndDestroyNode(String nodeId) {
        log.info("Testing search " + nodeId);
        // because we use a base DN, only need to supply the RDN
        DnContextMapper dnContextMapper = new DnContextMapper();
        AndFilter nodeFilter = new AndFilter();
        nodeFilter.and(new EqualsFilter("objectclass", "d1Node"));
        nodeFilter.and(new EqualsFilter("d1NodeId", nodeId));

        List nodeDnList = ldapTemplate.search(DistinguishedName.EMPTY_PATH, nodeFilter.encode(), dnContextMapper);
        if ((nodeDnList != null) && !(nodeDnList.isEmpty())) {
            AndFilter serviceRestrictionFilter = new AndFilter();
            serviceRestrictionFilter.and(new EqualsFilter("objectclass", "d1ServiceMethodRestriction"));
            serviceRestrictionFilter.and(new EqualsFilter("d1NodeId", nodeId));
            searchAndDestroySubElements(serviceRestrictionFilter);

            AndFilter serviceFilter = new AndFilter();
            serviceFilter.and(new EqualsFilter("objectclass", "d1NodeService"));
            serviceFilter.and(new EqualsFilter("d1NodeId", nodeId));
            searchAndDestroySubElements(serviceFilter);
            for (Object nodeDnInstance : nodeDnList) {
                DistinguishedName nodeDn = (DistinguishedName) nodeDnInstance;
                ldapTemplate.unbind(nodeDn);
            }
        }
    }

    private void searchAndDestroySubElements(Filter searchFilter) {
        DnContextMapper dnContextMapper = new DnContextMapper();
        List subDnList = ldapTemplate.search(DistinguishedName.EMPTY_PATH, searchFilter.encode(), dnContextMapper);
        if ((subDnList != null) && !(subDnList.isEmpty())) {
            for (Object subDnInstance : subDnList) {
                DistinguishedName subDn = (DistinguishedName) subDnInstance;
                ldapTemplate.unbind(subDn);
            }
        }
    }

    private static class DnContextMapper implements ContextMapper {

        public Object mapFromContext(Object ctx) {
            DirContextAdapter context = (DirContextAdapter) ctx;
            return new DistinguishedName(context.getDn());
        }
    }
    private Node buildTestNode(String resourcePath) throws IOException, InstantiationException, IllegalAccessException, MarshallingException {
        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();
        InputStream is = this.getClass().getResourceAsStream(resourcePath);

        BufferedInputStream bInputStream = new BufferedInputStream(is);
        byte[] barray = new byte[SIZE];
        int nRead = 0;
        while ((nRead = bInputStream.read(barray, 0, SIZE)) != -1) {
            mnNodeOutput.write(barray, 0, nRead);
        }
        bInputStream.close();
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(mnNodeOutput.toByteArray());
        Node testNode = TypeMarshaller.unmarshalTypeFromStream(Node.class, bArrayInputStream);
        return testNode;
    }
}
