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

package org.dataone.cn.web.node;

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
import org.dataone.cn.rest.web.node.NodeController;
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
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.types.v1.Subject;

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
    private NodeRegistryService nodeRegistryService = new NodeRegistryService();
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
        cnLdapPopulation.populateTestMNs();
        subjectLdapPopulation.populateTestIdentities();
    }
    @After
    public void after() throws Exception {
        cnLdapPopulation.deletePopulatedNodes();
        subjectLdapPopulation.deletePopulatedSubjects();
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
        sqR1ContactSubject.setValue("CN=Frankenstein,DC=cilogon,DC=org");
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
    @Test
    public void testUpdateNodeCapabilitiesNullSubjectList() throws Exception {

        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        String sq1shId = "urn:node:sq1sh";
        NodeReference sq1shNodeReference = new NodeReference();
        sq1shNodeReference.setValue(sq1shId);
        Node sq1shNode = nodeRegistryService.getNode(sq1shNodeReference);
        Subject sq1shContactSubject = new Subject();
        sq1shContactSubject.setValue("CN=Dracula,DC=cilogon,DC=org");
        sq1shNode.addContactSubject(sq1shContactSubject);
        sq1shNode.addSubject(sq1shContactSubject);

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
            log.info("sizeContactSubjectList " + sq1shUpdatedNode.sizeContactSubjectList());
            assertTrue(sq1shUpdatedNode.sizeContactSubjectList() == 2);
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        } finally {

            x509CertificateGenerator.cleanUpFiles();
        }
    }
}
