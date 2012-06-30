/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.base;

import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.web.base.BaseController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.util.Constants;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author waltz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/mockObject-dispatcher.xml", "classpath:/org/dataone/cn/resources/web/mockObject-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class BaseControllerTestCase {
    public static Log log = LogFactory.getLog(BaseControllerTestCase.class);
    @Test
    public void testJunk() {
        log.debug("hello");
    }

    private BaseController testController;
    @Resource
    public void setTestController(BaseController testController) {
        this.testController = testController;
    }
    @Test
    public void testValidBaseControllerChecksumList() throws Exception {
        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/" + Constants.RESOURCE_CHECKSUM + "/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        ChecksumAlgorithmList checksumAlgorithmList = null;
        try {
            ModelAndView mav = testController.listChecksumAlgorithms(request, response);
            checksumAlgorithmList = (ChecksumAlgorithmList) mav.getModel().get("org.dataone.service.types.v1.ChecksumAlgorithmList");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration " + ex);
        }

        assertNotNull(checksumAlgorithmList);
        assertNotNull(checksumAlgorithmList.getAlgorithmList());
        for (String checksum : checksumAlgorithmList.getAlgorithmList()) {
            log.debug(checksum);
        }
        assertTrue(checksumAlgorithmList.getAlgorithmList().size() == 2);
    }

}
