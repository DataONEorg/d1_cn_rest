/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.ldap;

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
@Qualifier("ldapPopulation")
public class LdapPopulation {

    public static List<Node> testNodeList = new ArrayList<Node>();
    public static List<Subject> testSubjectList = new ArrayList<Subject>();
    public static Log log = LogFactory.getLog(LdapPopulation.class);

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
        // because we use a base DN, only need to supply the RDN
        DistinguishedName dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", sq1dId);

        DirContextAdapter context = new DirContextAdapter(dn);
        mapNodeToContext(sq1dMNNode, context);
        ldapTemplate.bind(dn, context, null);
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

        // because we use a base DN, only need to supply the RDN
        dn = new DistinguishedName();
        dn.add("dc","dataone");
        dn.add("cn", sqR1Id);

        context = new DirContextAdapter(dn);
        mapNodeToContext(sqR1MNNode, context);
        ldapTemplate.bind(dn, context, null);
        testNodeList.add(sqR1MNNode);
    }

    protected void mapNodeToContext(Node node, DirContextOperations context) {

        context.setAttributeValue("objectclass", "d1Node");
        context.setAttributeValue("d1NodeId", node.getIdentifier().getValue());
        context.setAttributeValue("d1NodeName", node.getName());
        context.setAttributeValue("d1NodeDescription", node.getDescription());
        context.setAttributeValue("d1NodeBaseURL", node.getBaseURL());
        context.setAttributeValue("d1NodeReplicate", Boolean.toString(node.isReplicate()).toUpperCase());
        context.setAttributeValue("d1NodeSynchronize", Boolean.toString(node.isSynchronize()).toUpperCase());
        context.setAttributeValue("d1NodeType", node.getType().toString());
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
    public void populateTestIdentities() {
        String testSubject1Value = "Frankenstein";
        Subject testSubject1 = new Subject();
        testSubject1.setValue(testSubject1Value);
        Person testPerson1 = new Person();
        testPerson1.setSubject(testSubject1);
        testPerson1.addGivenName("TheMonster");
        testPerson1.setFamilyName(testSubject1Value);
        testPerson1.addEmail("frankenstien@monster.info");
        // because we use a base DN, only need to supply the RDN
        DistinguishedName dn1 = new DistinguishedName();
        dn1.add("cn", testSubject1.getValue());

        DirContextAdapter context1 = new DirContextAdapter(dn1);
        mapPersonToContext(testPerson1, context1);
        ldapTemplate.bind(dn1, context1, null);
        testSubjectList.add(testSubject1);

        String testSubject2Value = "Dracula";
        Subject testSubject2 = new Subject();
        testSubject2.setValue(testSubject2Value);
        Person testPerson2 = new Person();
        testPerson2.setSubject(testSubject2);
        testPerson2.addGivenName("Vlad");
        testPerson2.setFamilyName(testSubject2Value);
        testPerson2.addEmail("dracula@monsters.info");

        DistinguishedName dn2 = new DistinguishedName();
        dn2.add("cn", testSubject2.getValue());

        DirContextAdapter context2 = new DirContextAdapter(dn2);
        mapPersonToContext(testPerson2, context2);
        ldapTemplate.bind(dn2, context2, null);
        testSubjectList.add(testSubject2);
    }

    protected void mapPersonToContext(Person person, DirContextOperations context) {
        context.setAttributeValues("objectclass", new String[]{"top", "person", "organizationalPerson", "inetOrgPerson", "d1Principal"});
        context.setAttributeValue("cn", person.getFamilyName());
        context.setAttributeValue("sn", person.getFamilyName());
        context.setAttributeValues("givenName", person.getGivenNameList().toArray());
        context.setAttributeValues("mail", person.getEmailList().toArray());
        context.setAttributeValue("isVerified", "FALSE");

    }

    public void deletePopulatedSubjects() {
        for (Subject subject : testSubjectList) {
            deleteSubject(subject);
        }
        testSubjectList.clear();
    }

    private void deleteSubject(Subject subject) {
        DistinguishedName dn = new DistinguishedName();
        dn.add("cn", subject.getValue());
        log.info("deleting : " + dn.toString());
        ldapTemplate.unbind(dn);
    }
}
