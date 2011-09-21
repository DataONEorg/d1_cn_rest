/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.node;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.dataone.cn.ldap.v1.SubjectLdapPopulation;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.dataone.cn.ldap.v1.NodeLdapPopulation;
import org.dataone.cn.rest.web.node.NodeController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.util.TypeMarshaller;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

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
    private ClassPathResource mnNodeResource;
    final static int SIZE = 16384;
    @Resource
    public void setCNLdapPopulation(NodeLdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }
    @Resource
    public void setTestController(NodeController testController) {
        this.testController = testController;
    }
    @Before
    public void before() throws Exception {
        cnLdapPopulation.populateTestMNs();
    }
    @After
    public void after() throws Exception {
        cnLdapPopulation.deletePopulatedMns();
    }
    @Resource
    public void setMnNodeResource(ClassPathResource mnNodeResource) {
        this.mnNodeResource = mnNodeResource;
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
        request.setMethod("POST");
        request.setContextPath("/Mock/node/");
        request.addFile(mockNodeFile);
 

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeReference nodeReference = null;
        try {
            ModelAndView mav = testController.register(request, response);
            nodeReference = (NodeReference) mav.getModel().get("org.dataone.service.types.v1.NodeReference");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration " + ex);
        }
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(mnNodeOutput.toByteArray());
        Node mnNode = TypeMarshaller.unmarshalTypeFromStream(Node.class,  bArrayInputStream);
        mnNode.getIdentifier().setValue(nodeReference.getValue());
        cnLdapPopulation.testNodeList.add(mnNode);
        assertNotNull(nodeReference);
        assertFalse(nodeReference.getValue().isEmpty());
    }

}
