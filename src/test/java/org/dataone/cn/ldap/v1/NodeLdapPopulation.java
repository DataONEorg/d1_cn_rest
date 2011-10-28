/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.ldap.v1;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author waltz
 */
@Component
@Qualifier("nodeLdapPopulation")
public class NodeLdapPopulation {

    public static List<Node> testNodeList = new ArrayList<Node>();
    public static List<Subject> testSubjectList = new ArrayList<Subject>();
    public static Log log = LogFactory.getLog(NodeLdapPopulation.class);

    static {
        // Need this or context will lowercase all the rdn s
        System.setProperty(DistinguishedName.KEY_CASE_FOLD_PROPERTY, DistinguishedName.KEY_CASE_FOLD_NONE);
    }
    @Autowired
    @Qualifier("ldapTemplate")
    private LdapTemplate ldapTemplate;

    public void populateTestMNs() {
        Node sq1dMNNode = new Node();
        String sq1dId = "sq1d";
        NodeReference sq1dNodeReference = new NodeReference();
        sq1dNodeReference.setValue(sq1dId);
        sq1dMNNode.setIdentifier(sq1dNodeReference);
        sq1dMNNode.setName("squid");
        sq1dMNNode.setDescription("this is a squid test");
        sq1dMNNode.setBaseURL("https://my.squid.test/mn");
        sq1dMNNode.setReplicate(false);
        sq1dMNNode.setSynchronize(false);
        sq1dMNNode.setState(NodeState.UP);
        sq1dMNNode.setType(NodeType.MN);
        Subject sq1dSubject = new Subject();
        sq1dSubject.setValue("cn="+sq1dId+",dc=dataone,dc=org");
        sq1dMNNode.addSubject(sq1dSubject);

        Subject sq1dContactSubject = new Subject();
        sq1dContactSubject.setValue("CN=Dracula,DC=cilogon,DC=org");
        sq1dMNNode.addContactSubject(sq1dContactSubject);
        
        Services sq1dservices = new Services();
        Service sq1dcoreService = new Service();
        sq1dcoreService.setName("MNCore");
        sq1dcoreService.setVersion("v1");
        sq1dcoreService.setAvailable(Boolean.TRUE);

        Service sq1dreadService = new Service();
        sq1dreadService.setName("MNRead");
        sq1dreadService.setVersion("v1");
        sq1dreadService.setAvailable(Boolean.TRUE);
        sq1dservices.addService(sq1dcoreService);
        sq1dservices.addService(sq1dreadService);
        sq1dMNNode.setServices(sq1dservices);
        // because we use a base DN, only need to supply the RDN
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", sq1dId);

        DirContextAdapter context = new DirContextAdapter(dn);
        mapNodeToContext(sq1dMNNode, context);
        ldapTemplate.bind(dn, context, null);

        for (Service service : sq1dMNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc","dataone");
            dnService.add("cn", sq1dId);
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sq1dId, d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
        }



        testNodeList.add(sq1dMNNode);


        Node sqR1MNNode = new Node();
        String sqR1Id = "sqR1";
        NodeReference sqR1NodeReference = new NodeReference();
        sqR1NodeReference.setValue(sqR1Id);
        sqR1MNNode.setIdentifier(sqR1NodeReference);
        sqR1MNNode.setName("squirrel");
        sqR1MNNode.setDescription("this is a squirrel test");
        sqR1MNNode.setBaseURL("https://my.squirrel.test/mn");
        sqR1MNNode.setReplicate(false);
        sqR1MNNode.setSynchronize(false);
        sqR1MNNode.setState(NodeState.UP);
        sqR1MNNode.setType(NodeType.MN);
        Subject sqR1Subject = new Subject();
        sqR1Subject.setValue("cn="+sqR1Id+",dc=dataone,dc=org");
        sqR1MNNode.addSubject(sqR1Subject);

        Subject sqR1ContactSubject = new Subject();
        sqR1ContactSubject.setValue("CN=Frankenstein,DC=cilogon,DC=org");
        sqR1MNNode.addContactSubject(sqR1ContactSubject);
        
        Services sqR1services = new Services();
        Service sqR1coreService = new Service();
        sqR1coreService.setName("MNCore");
        sqR1coreService.setVersion("v1");
        sqR1coreService.setAvailable(Boolean.TRUE);

        Service sqR1readService = new Service();
        sqR1readService.setName("MNRead");
        sqR1readService.setVersion("v1");
        sqR1readService.setAvailable(Boolean.TRUE);
        sqR1services.addService(sqR1coreService);
        sqR1services.addService(sqR1readService);
        sqR1MNNode.setServices(sqR1services);

        // because we use a base DN, only need to supply the RDN
        dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", sqR1Id);

        context = new DirContextAdapter(dn);
        mapNodeToContext(sqR1MNNode, context);
        ldapTemplate.bind(dn, context, null);
        for (Service service : sqR1MNNode.getServices().getServiceList()) {
            String d1NodeServiceId = service.getName() + "-" + service.getVersion();
            DistinguishedName dnService = new DistinguishedName();
            dnService.add("dc","dataone");
            dnService.add("cn", sqR1Id);
            dnService.add("d1NodeServiceId", d1NodeServiceId);
            context = new DirContextAdapter(dnService);
            mapServiceToContext(service, sqR1Id, d1NodeServiceId, context);
            ldapTemplate.bind(dnService, context, null);
        }
        testNodeList.add(sqR1MNNode);
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
        context.setAttributeValue("subject", node.getSubject(0).getValue());
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
    public void deletePopulatedMns() {
        for (Node node : testNodeList) {
            if ((node.getServices() != null) && (!node.getServices().getServiceList().isEmpty())) {
                for (Service service : node.getServices().getServiceList()) {
                    deleteNodeService(node, service);
                }
            }
            deleteNode(node);
        }
        testNodeList.clear();
    }

    private void deleteNode(Node node) {
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", node.getIdentifier().getValue());
        log.info("deleting : " + dn.toString());
        ldapTemplate.unbind(dn);
    }
    private void deleteNodeService(Node node, Service service) {
        String d1NodeServiceId = service.getName() + "-" + service.getVersion();
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", node.getIdentifier().getValue());
        dn.add("d1NodeServiceId",d1NodeServiceId);
        log.info("deleting : " + dn.toString());
        ldapTemplate.unbind(dn);
    }
}
