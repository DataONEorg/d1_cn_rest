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
package org.dataone.cn.rest.v1.suite;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.auth.X509CertificateGenerator;

import org.dataone.cn.rest.v1.RegistryController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.util.TypeMarshaller;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
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
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.cn.impl.v1.CNIdentityLDAPImpl;
import org.dataone.service.cn.v1.impl.NodeRegistryServiceImpl;
import org.dataone.service.exceptions.NotAuthorized;
import org.jibx.runtime.JiBXException;

/**
 *
 * @author rwaltz
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v1/mockRegistryController-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class SuiteTestUnit3Registry {

    /**
     * the servlet
     */
    public static Log log = LogFactory.getLog(SuiteTestUnit3Registry.class);

    private final String primarySubject = Settings.getConfiguration().getString("testIdentity.primarySubject");
    private final String primarySubjectCN = Settings.getConfiguration().getString("testIdentity.primarySubjectCN");

    private final String secondarySubject = Settings.getConfiguration().getString("testIdentity.secondarySubject");
    private final String secondarySubjectCN = Settings.getConfiguration().getString("testIdentity.secondarySubjectCN");

    private final String tertiarySubject = Settings.getConfiguration().getString("testIdentity.tertiarySubject");
    private final String tertiarySubjectCN = Settings.getConfiguration().getString("testIdentity.tertiarySubjectCN");

    private final String quartarySubject = Settings.getConfiguration().getString("testIdentity.quartarySubject");
    private final String quartarySubjectCN = Settings.getConfiguration().getString("testIdentity.quartarySubjectCN");

    private final String groupName = Settings.getConfiguration().getString("testIdentity.groupName");

    private final String testNodeMNv1Subject = Settings.getConfiguration().getString("testNode.mn.v1.subject");
    private final String testMNv1NodeID = Settings.getConfiguration().getString("testNode.mn.v1.nodeId");

    private final String testNodeMNv100Subject = Settings.getConfiguration().getString("testNode.mn.v100.subject");
    private final String testMNv100NodeID = Settings.getConfiguration().getString("testNode.mn.v100.nodeId");

    final static int SIZE = 16384;
    final String aTONdescription = "Another Test Of Nodes";

    private RegistryController testController;

    @Resource
    public void setTestController(RegistryController testController) {
        this.testController = testController;
    }

    /*    @Resource
     public void setX509CertificateGenerator(X509CertificateGenerator ciLogonX509CertificateGenerator) {
     this.x509CertificateGenerator = ciLogonX509CertificateGenerator;
     }
     */
    @Resource
    @Qualifier("ciLogonX509CertificateGenerator")
    private X509CertificateGenerator ciLogonX509CertificateGenerator;

    @Resource
    @Qualifier("dataoneX509CertificateGenerator")
    private X509CertificateGenerator dataoneX509CertificateGenerator;

    @Resource
    @Qualifier("nodeRegistryServiceV1")
    private NodeRegistryServiceImpl nodeRegistryService;
    @Resource
    @Qualifier("identityServiceV1")
    private CNIdentityLDAPImpl cnIdentity;

    @Resource
    @Qualifier("mnNodeResource")
    private ClassPathResource mnNodeResource;

    @Resource
    @Qualifier("mnInvalidBaseUrlNodeResource")
    private ClassPathResource mnInvalidBaseUrlNodeResource;

    @Resource
    @Qualifier("mnInvalidSubjectNodeResource")
    private ClassPathResource mnInvalidSubjectNodeResource;
    
    /* 
     * confirm that a nodeList may be retrieved
     */
    @Test
    public void testValidControllerGetNodeList() throws Exception {
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
    
    /* confirm that a new node with correct properties may be registered with a 
     valid certificate and valid contact subject
     */

    @Test
    public void testRegisterNode() throws Exception {

        X509Certificate x509Certificate = ciLogonX509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();

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

            assertNotNull(nodeReference);
            assertFalse(nodeReference.getValue().isEmpty());

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }
    }
    /*
     Confirm that well-formed and valid node xml may contain bad information
     and is rejected. The bad information is the id of the node, non-compliance
     with nodeId standards.
     */

    @Test
    public void testRegisterBadNode() throws Exception {
        X509Certificate x509Certificate = ciLogonX509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};
        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();

        BufferedInputStream bInputStream = new BufferedInputStream(mnInvalidBaseUrlNodeResource.getInputStream());
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

    }
    /* Retrieve from the samples resources directory a node that
     contains an subject that is not authorized to register node.
     (an unverified subject may not register nodes)
     Try to register it and confirm that it fails
     */

    @Test
    public void testRegisterNodeUnauthorizedSubject() throws Exception {

        X509Certificate x509Certificate = ciLogonX509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();

        BufferedInputStream bInputStream = new BufferedInputStream(mnInvalidSubjectNodeResource.getInputStream());
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
            testController.register(request, response);
            fail("registerNode should have failed with a NotAuthorized Exception");
        } catch (NotAuthorized ex) {
            assert (ex.getMessage().contains("not verified"));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }
    }
    /* verify that an authorized subject (the subject of the node)
     can update the node capabilities of the node
     */

    @Test
    public void testUpdateNodeCapabilities() throws Exception {

        X509Certificate x509Certificate = dataoneX509CertificateGenerator.getCertificate(testMNv1NodeID);
        log.info(x509Certificate.getSubjectDN().getName());
        X509Certificate certificate[] = {x509Certificate};

        NodeReference testNodeMNv1NodeReference = new NodeReference();
        testNodeMNv1NodeReference.setValue(testMNv1NodeID);
        Node sq1dNode = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
        sq1dNode.setDescription(aTONdescription);

        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sq1dNode, mnNodeOutput);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + testMNv1NodeID);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, testMNv1NodeID);
            Node sq1dUpdatedNode = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
            log.info("sizeContactSubjectList " + sq1dUpdatedNode.sizeContactSubjectList());
            assertTrue(sq1dUpdatedNode.getDescription().equals(aTONdescription));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }
    }
    /*
     * Last Harvested should not be able to be updated via updateNodeCapabilities
     * It is only updated by cn background system operations
     */

    @Test
    public void testUpdateNodeCapabilitiesIgnoreLastHarvested() throws Exception {

        X509Certificate x509Certificate = dataoneX509CertificateGenerator.getCertificate(testMNv1NodeID);
        X509Certificate certificate[] = {x509Certificate};

        NodeReference testNodeMNv1NodeReference = new NodeReference();
        testNodeMNv1NodeReference.setValue(testMNv1NodeID);
        Node testNodeMNv1 = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
        Date lastHarvested = testNodeMNv1.getSynchronization().getLastHarvested();

        testNodeMNv1.getSynchronization().setLastHarvested(new Date());
        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(testNodeMNv1, mnNodeOutput);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());
        log.debug(mnNodeOutput.toByteArray());
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + testNodeMNv1NodeReference);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, testMNv1NodeID);
            Node sq1shUpdatedNode = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
            Date notUpdatedLastHarvested = sq1shUpdatedNode.getSynchronization().getLastHarvested();
            assertTrue(notUpdatedLastHarvested.compareTo(lastHarvested) == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }
    }

    /* given an equivalent identity of an administrative subject, 
     * can changes be made?
     */
    @Test
    public void testUpdateNodeCapabilitiesWithEquivalentId() throws Exception {

        // Generate a certificate with a subject that is the equivalent identity
        // of a contactSubject
        X509Certificate x509Certificate = ciLogonX509CertificateGenerator.getCertificate(quartarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        // change a changable property, like descriptiong
        NodeReference testNodeMNv1NodeReference = new NodeReference();
        testNodeMNv1NodeReference.setValue(testMNv1NodeID);
        Node testNodeMNv1 = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);

        Node testNodeMNv1Node = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
        String newDescription = "ThisNodeIsaTestNode";
        testNodeMNv1Node.setDescription(newDescription);

        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(testNodeMNv1Node, mnNodeOutput);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + testMNv1NodeID);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, testMNv1NodeID);
            Node testNodeMNv1UpdatedNode = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
            assertTrue(testNodeMNv1UpdatedNode.getDescription().compareTo(newDescription) == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }
    }
    /* given a subject that is a member of an administrative group, 
     * can changes be made?
     */

    @Test
    public void testUpdateNodeCapabilitiesWithGroup() throws Exception {

        // Generate a certificate with a member of the group assigned to cn.administrators
        // located in node.properties
        X509Certificate x509Certificate = ciLogonX509CertificateGenerator.getCertificate(secondarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        NodeReference testNodeMNv1NodeReference = new NodeReference();
        testNodeMNv1NodeReference.setValue(testMNv1NodeID);

        Node testNodeMNv1Node = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
        String newDescription = "ThisNodeIsaTestNode";
        testNodeMNv1Node.setDescription(newDescription);

        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(testNodeMNv1Node, mnNodeOutput);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockNodeFile = new MockMultipartFile("node", mnNodeOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.setMethod("PUT");
        request.setContextPath("/Mock/node/" + testMNv1NodeID);
        request.addFile(mockNodeFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            testController.updateNodeCapabilities(request, response, testMNv1NodeID);
            Node sq1shUpdatedNode = nodeRegistryService.getNodeCapabilities(testNodeMNv1NodeReference);
            assertTrue(sq1shUpdatedNode.getDescription().compareTo(newDescription) == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test misconfiguration " + ex);
        }

    }

    private Node getTestMNNode() throws IOException, InstantiationException, IllegalAccessException, JiBXException, MarshallingException {
        ByteArrayOutputStream mnNodeOutput = new ByteArrayOutputStream();

        BufferedInputStream bInputStream = new BufferedInputStream(mnNodeResource.getInputStream());
        byte[] barray = new byte[SIZE];
        int nRead = 0;

        while ((nRead = bInputStream.read(barray, 0, SIZE)) != -1) {
            mnNodeOutput.write(barray, 0, nRead);
        }
        bInputStream.close();
        String nodeString = new String(mnNodeOutput.toByteArray());
        log.info(nodeString);
        Node mnNode = new Node();
        return TypeMarshaller.unmarshalTypeFromStream(mnNode.getClass(), bInputStream);
    }
}
