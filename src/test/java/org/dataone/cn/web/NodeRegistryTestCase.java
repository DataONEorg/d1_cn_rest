/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web;

import org.dataone.cn.rest.web.node.NodeController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.cn.web.proxy.service.MockProxyObjectServiceImpl;
import org.dataone.service.types.NodeList;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.core.io.Resource;

/**
 *
 * @author rwaltz
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/mockObject-dispatcher.xml", "classpath:/org/dataone/cn/resources/web/mockObject-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class NodeRegistryTestCase {

    /** the servlet */
    private WebApplicationContext wac;
    private NodeController testController;
    private Resource nodeRegistryResource;
    private MockProxyObjectServiceImpl testProxyObject;

    @Before
    public void before() throws Exception {
        wac = WebApplicationContextUtils.getRequiredWebApplicationContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
        if (wac == null) {
            throw new Exception("cannot find Web Application Context!");
        }
        testProxyObject = wac.getBean(MockProxyObjectServiceImpl.class);
        nodeRegistryResource = wac.getBean("nodeRegistryResource", Resource.class);
        testController = wac.getBean(NodeController.class);
    }

    @Test
    public void testValidMetacatControllerGetNodeList() throws Exception {
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/node/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeList nodeList = null;
        try {
            ModelAndView mav = testController.getNodeList(request, response);
            nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.NodeList");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(nodeList);
        assertTrue(nodeList.getNodeList().size() > 0);
        /*
         * Dates are converted correctly SO the strings will never equal,
         *
        // need to do comparison from original to returned and assert truth!
        ByteArrayOutputStream nodeListOutput = (ByteArrayOutputStream)TypeMarshaller.marshalTypeToOutputStream(nodeList);
        String nodeListString = new String(nodeListOutput.toByteArray());

        ByteArrayOutputStream nodeRegistryOutput = new ByteArrayOutputStream();
        testProxyObject.writeToResponse(nodeRegistryResource.getInputStream(), nodeRegistryOutput);
        String nodeRegistryString = new String(nodeRegistryOutput.toByteArray());



        // strip out all the newlines and spaces
        nodeRegistryString = nodeRegistryString.replace("\n", "");
        nodeRegistryString = nodeRegistryString.replace(" ", "");
        nodeListString = nodeListString.replace("\n", "");
        nodeListString = nodeListString.replace(" ", "");
        System.out.println( "node list compare: \n" + nodeListString +"\n" +nodeRegistryString +"\n");
        assertTrue(nodeRegistryString.contentEquals(nodeListString));

         *
         */
    }
    @Test
    public void testInvalidSchemaNodeList() throws Exception {
        nodeRegistryResource = wac.getBean("nodeRegistryInvalidSchemaResource", Resource.class);
        testProxyObject.setNodeRegistryResource(nodeRegistryResource);
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/node/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeList nodeList = null;
        try {
            ModelAndView mav = testController.getNodeList(request, response);
            nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.NodeList");

        } catch (ServiceFailure ex) {
            assertTrue(ex.getDetail_code().equalsIgnoreCase("4801"));
            return;
        }
        fail("InvalidSchema should have failed on Service Failure!");
    }
    @Test
    public void testMalformedXMLSchemaNodeList() throws Exception {
        nodeRegistryResource = wac.getBean("nodeRegistryMalformedXMLResource", Resource.class);
        testProxyObject.setNodeRegistryResource(nodeRegistryResource);
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/node/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        NodeList nodeList = null;
        try {
            ModelAndView mav = testController.getNodeList(request, response);
            nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.NodeList");

        } catch (ServiceFailure ex) {
            assertTrue(ex.getDetail_code().equalsIgnoreCase("4801"));
            return;
        }
        fail("MalformedXML should have failed on Service Failure!");
    }


//    XXX Empty BaseURL's are valid in Schema, should not be
//    @Test
//    public void testNullBaseURLSchemaNodeList() throws Exception {
//        nodeRegistryResource = wac.getBean("nodeRegistryNullBaseURLResource", Resource.class);
//        testProxyObject.setNodeRegistryResource(nodeRegistryResource);
//        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
//
//        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/node/");
//
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        NodeList nodeList = null;
//        try {
//            ModelAndView mav = testController.getNodeList(request, response);
//            nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.NodeList");
//
//        } catch (ServiceFailure ex) {
//            assertTrue(ex.getDetail_code().equalsIgnoreCase("4801"));
//            return;
//        }
//        fail("NullBaseURL should have failed on Service Failure!");
//    }
}
