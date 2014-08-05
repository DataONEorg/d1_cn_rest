/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.web.node.v1;

import com.hazelcast.config.Config;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.auth.X509CertificateGenerator;
import org.dataone.cn.ldap.v1.NodeLdapPopulation;
import org.dataone.cn.ldap.v1.SubjectLdapPopulation;
import org.dataone.cn.rest.web.node.v1.NodeController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.util.TypeMarshaller;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import org.dataone.configuration.Settings;
import org.dataone.service.cn.impl.v1.CNIdentityLDAPImpl;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.types.v1.*;

/**
 *
 * @author rwaltz
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/mockObject-dispatcher.xml", "classpath:/org/dataone/cn/resources/web/mockObject-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class NodeRegistryTestCase {

    /** the servlet */
    public static Log log = LogFactory.getLog(NodeRegistryTestCase.class);
    private NodeController testController;
    private NodeLdapPopulation cnLdapPopulation;
    private SubjectLdapPopulation subjectLdapPopulation;
    private X509CertificateGenerator x509CertificateGenerator;

    private Config hzConfig;
    final static int SIZE = 16384;
    @Resource
    public void setCNLdapPopulation(NodeLdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }
    @Resource
    public void setTestController(NodeController testController) {
        this.testController = testController;
    }
    @Resource
    public void setCNLdapPopulation(SubjectLdapPopulation subjectLdapPopulation) {
        this.subjectLdapPopulation = subjectLdapPopulation;
    }
    @Resource
    public void setX509CertificateGenerator(X509CertificateGenerator x509CertificateGenerator) {
        this.x509CertificateGenerator = x509CertificateGenerator;
    }
    @Before
    public void before() throws Exception {
        subjectLdapPopulation.searchAndDestroyIdentity();
        cnLdapPopulation.populateTestMNs();
        cnLdapPopulation.populateTestCN();
        subjectLdapPopulation.populateTestIdentities();
    }
    @After
    public void after() throws Exception {
        cnLdapPopulation.deletePopulatedNodes();
        subjectLdapPopulation.searchAndDestroyIdentity();
    }
    @Autowired
    @Qualifier("mnNodeResource")
    private ClassPathResource mnNodeResource;

    public void setValidMnNodeResource(ClassPathResource mnValidNodeResource) {
        this.mnNodeResource = mnNodeResource;
    }
    @Autowired
    @Qualifier("mnInvalidNodeResource")
    private ClassPathResource mnInvalidNodeResource;
    @Autowired
    @Qualifier("cnNodeRegistryV1")
    private NodeRegistryService nodeRegistryService;   
    @Autowired
    @Qualifier("cnIdentity")
    private CNIdentityLDAPImpl  cnIdentity;    
    public void setInvalidMnNodeResource(ClassPathResource mnInvalidNodeResource) {
        this.mnInvalidNodeResource = mnInvalidNodeResource;
    }
    @Resource
    public void setHzConfig(Config hzConfig) {
        this.hzConfig = hzConfig;
    }
    @Test
    public void testValidMetacatControllerGetNodeList() throws Exception {
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/node/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeList nodeList = null;
        try {
            ModelAndView mav = testController.getNodeList(request, response);
            nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.v1.NodeList");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration " + ex);
        }

        assertNotNull(nodeList);
        assertTrue(nodeList.getNodeList().size() > 0);
    }
    @Test
    public void testRegisterNode() throws Exception {

        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();

        BufferedInputStream bInputStream = new BufferedInputStream(mnNodeResource.getInputStream());
        byte[] barray = new byte[SIZE];
        int nRead = 0;

        while ((nRead = bInputStream.read(barray, 0, SIZE)) != -1) {
            mnNodeOutput.write(barray, 0, nRead);
        }
        bInputStream.close();
        String nodeString = new String(mnNodeOutput.toByteArray());
        log.info(nodeString);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());
        
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("POST");
        request.setContextPath("/Mock/node/");
        request.addFile(mockNodeFile);
 
        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            ModelAndView mav = testController.register(request, response);
            nodeReference = (NodeReference) mav.getModel().get("org.dataone.service.types.v1.NodeReference");

            //IMap<NodeReference, Node> hzNodes = testController.getHzclient().getMap("hzNodes");
            //hzNodes.evict(nodeReference);
            assertNotNull(nodeReference);
            assertFalse(nodeReference.getValue().isEmpty());

        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {
            if (nodeReference != null) {
                ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(mnNodeOutput.toByteArray());
                Node mnNode = TypeMarshaller.unmarshalTypeFromStream(Node.class,  bArrayInputStream);
                mnNode.getIdentifier().setValue(nodeReference.getValue());
                cnLdapPopulation.testNodeList.add(mnNode);
            }
            x509CertificateGenerator.cleanUpFiles();
        }
    }
    
    @Test
    public void testRegisterBadNode() throws Exception {
        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};
        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();

        BufferedInputStream bInputStream = new BufferedInputStream(mnInvalidNodeResource.getInputStream());
        byte[] barray = new byte[SIZE];
        int nRead = 0;

        while ((nRead = bInputStream.read(barray, 0, SIZE)) != -1) {
            mnNodeOutput.write(barray, 0, nRead);
        }
        bInputStream.close();
        String nodeString = new String(mnNodeOutput.toByteArray());
        log.info(nodeString);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);


        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("POST");
        request.setContextPath("/Mock/node/");
        request.addFile(mockNodeFile);


        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            ModelAndView mav = testController.register(request, response);
            nodeReference = (NodeReference) mav.getModel().get("org.dataone.service.types.v1.NodeReference");

        } catch (InvalidRequest ex) {
            assertTrue(ex.getDetail_code().equals("4823"));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }

        assertNull(nodeReference);
        x509CertificateGenerator.cleanUpFiles();
    }
    
    @Test
    public void testUpdateNodeCapabilities() throws Exception {

        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        String sq1dId = "urn:node:sq1d";
        NodeReference sq1dNodeReference = new NodeReference();
        sq1dNodeReference.setValue(sq1dId);
        Node sq1dNode = nodeRegistryService.getNode(sq1dNodeReference);
        Subject sqR1ContactSubject = new Subject();
        sqR1ContactSubject.setValue("CN=Frankenstein,O=Test,C=US,DC=cilogon,DC=org");
        sq1dNode.addContactSubject(sqR1ContactSubject);
        sq1dNode.addSubject(sqR1ContactSubject);

        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sq1dNode, mnNodeOutput);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + sq1dId);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, sq1dId);
            Node sq1dUpdatedNode = nodeRegistryService.getNode(sq1dNodeReference);
            log.info("sizeContactSubjectList " + sq1dUpdatedNode.sizeContactSubjectList());
            assertTrue(sq1dUpdatedNode.sizeContactSubjectList() == 2);
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {

            x509CertificateGenerator.cleanUpFiles();
        }
    }
    /*
     * Last Harvested should not be able to be updated via updateNodeCapabilities
     * It is only updated by cn background system operations
     */
    @Test
    public void testUpdateNodeCapabilitiesIgnoreLastHarvested() throws Exception {

        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        String sq1shId = "urn:node:sq1sh";
        NodeReference sq1shNodeReference = new NodeReference();
        sq1shNodeReference.setValue(sq1shId);
        
        Node sq1shNode = nodeRegistryService.getNode(sq1shNodeReference);
        Date lastHarvested = sq1shNode.getSynchronization().getLastHarvested();
        
        
        sq1shNode.getSynchronization().setLastHarvested(new Date());
        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sq1shNode, mnNodeOutput);
        
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());
        log.debug(mnNodeOutput.toByteArray());
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + sq1shId);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, sq1shId);
            Node sq1shUpdatedNode = nodeRegistryService.getNode(sq1shNodeReference);
            Date notUpdatedLastHarvested = sq1shUpdatedNode.getSynchronization().getLastHarvested();
            log.info("sizeContactSubjectList " + sq1shUpdatedNode.sizeContactSubjectList());
            assertTrue(notUpdatedLastHarvested.compareTo(lastHarvested) == 0);
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {

            x509CertificateGenerator.cleanUpFiles();
        }
    }
    
    /* given an equivalent identity of an administrator, 
     * can changes be made?
     */
    @Test
    public void testUpdateNodeCapabilitiesWithEquivalentId() throws Exception {
       
        
        // Set up the session with the subject 
        // that is approved for registering and mapping accounts
        
        String dnPrime = Settings.getConfiguration().getString("testIdentity.primarySubject");
        Subject subjectPrime = new Subject();
        subjectPrime.setValue(dnPrime);
        Session session = new Session();
        session.setSubject(subjectPrime);
        
        // register first test subject
        String dnEquivalent1 = Settings.getConfiguration().getString("testIdentity.tertiarySubject");
        Subject subject1 = new Subject();
        subject1.setValue(dnEquivalent1);
        Person person1 = new Person();
        person1.setSubject(subject1);
        person1.setFamilyName("Lebowski");
        person1.addGivenName("Jeff");
        person1.addEmail("Jeff@dataone.org");
        cnIdentity.registerAccount(session,person1);
        
        // register second test subject
        String dnEquivalent2 = Settings.getConfiguration().getString("testIdentity.quartarySubject");
        Subject subject2 = new Subject();
        subject2.setValue(dnEquivalent2);
        Person person2 = new Person();
        person2.setSubject(subject2);
        person2.setFamilyName("Dude");
        person2.addGivenName("The");
        person2.addEmail("TheDude@dataone.org");
        cnIdentity.registerAccount(session,person2);

        // map the identities together 
        cnIdentity.mapIdentity(session, subject1, subject2);
        
        // Add the first subject to the administrators list
        List<String> nodeAdministrators = Settings.getConfiguration().getList("cn.administrators");
        nodeAdministrators.add(subject1.getValue());
        Settings.getConfiguration().setProperty("cn.administrators", nodeAdministrators);
        // Generate a certificate with the second subject
        // The first subject is a member of the cn.administrators
        // property in node.properties file
        x509CertificateGenerator.setCommonName("TheDude");
        x509CertificateGenerator.storeSelfSignedCertificate(true);
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        // change a changable property, like descriptiong
        String sq1shId = "urn:node:sq1sh";
        NodeReference sq1shNodeReference = new NodeReference();
        sq1shNodeReference.setValue(sq1shId);
        
        Node sq1shNode = nodeRegistryService.getNode(sq1shNodeReference);
        String newDescription = "ThisNodeIsaTestNode";
        sq1shNode.setDescription(newDescription);

        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sq1shNode, mnNodeOutput);
        
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());
        
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + sq1shId);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, sq1shId);
            Node sq1shUpdatedNode = nodeRegistryService.getNode(sq1shNodeReference);
            assertTrue(sq1shUpdatedNode.getDescription().compareTo(newDescription) == 0);
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {
            x509CertificateGenerator.cleanUpFiles();
        }
    }
    /* given a subject that is a member of an administrative group, 
     * can changes be made?
     */
    @Test
    public void testUpdateNodeCapabilitiesWithGroup() throws Exception {
       
        
        // Set up the session with the subject 
        // that is approved for registering and mapping accounts
        
        String dnPrime = Settings.getConfiguration().getString("testIdentity.primarySubject");
        Subject subjectPrime = new Subject();
        subjectPrime.setValue(dnPrime);
        Session session = new Session();
        session.setSubject(subjectPrime);
        
        // register first test subject
        String dnEquivalent1 = Settings.getConfiguration().getString("testIdentity.tertiarySubject");
        Subject subject1 = new Subject();
        subject1.setValue(dnEquivalent1);
        Person person1 = new Person();
        person1.setSubject(subject1);
        person1.setFamilyName("Lebowski");
        person1.addGivenName("Jeff");
        person1.addEmail("Jeff@dataone.org");
        cnIdentity.registerAccount(session,person1);
        
        // register second test subject
        String dnEquivalent2 = Settings.getConfiguration().getString("testIdentity.quartarySubject");
        Subject subject2 = new Subject();
        subject2.setValue(dnEquivalent2);
        Person person2 = new Person();
        person2.setSubject(subject2);
        person2.setFamilyName("Dude");
        person2.addGivenName("The");
        person2.addEmail("TheDude@dataone.org");
        cnIdentity.registerAccount(session,person2);

        Group adminGroup = new Group();
        Subject groupSubject = new Subject();
        groupSubject.setValue(Settings.getConfiguration().getString("testIdentity.groupName"));

        adminGroup.setSubject(groupSubject);
        SubjectList newMembers = new SubjectList();
        newMembers.addSubject(subject1);
        newMembers.addSubject(subject2);

        adminGroup.setHasMemberList(newMembers.getSubjectList());
        // map the identities together 
        cnIdentity.createGroup(session, adminGroup);
        
        // Add the first subject to the administrators list
        List<String> nodeAdministrators = Settings.getConfiguration().getList("cn.administrators");
        nodeAdministrators.add(groupSubject.getValue());
        Settings.getConfiguration().setProperty("cn.administrators", nodeAdministrators);
        // Generate a certificate with the second subject
        // The first subject is a member of the cn.administrators
        // property in node.properties file
        x509CertificateGenerator.setCommonName("TheDude");
        x509CertificateGenerator.storeSelfSignedCertificate(true);
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        // change a changable property, like descriptiong
        String sq1shId = "urn:node:sq1sh";
        NodeReference sq1shNodeReference = new NodeReference();
        sq1shNodeReference.setValue(sq1shId);
        
        Node sq1shNode = nodeRegistryService.getNode(sq1shNodeReference);
        String newDescription = "ThisNodeIsaTestNode";
        sq1shNode.setDescription(newDescription);

        ByteArrayOutputStream mnNodeOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sq1shNode, mnNodeOutput);
        
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());
        
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + sq1shId);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, sq1shId);
            Node sq1shUpdatedNode = nodeRegistryService.getNode(sq1shNodeReference);
            assertTrue(sq1shUpdatedNode.getDescription().compareTo(newDescription) == 0);
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {
            subjectLdapPopulation.testSubjectList.add(Settings.getConfiguration().getString("testIdentity.groupName"));
            
            x509CertificateGenerator.cleanUpFiles();
        }
    }
}
