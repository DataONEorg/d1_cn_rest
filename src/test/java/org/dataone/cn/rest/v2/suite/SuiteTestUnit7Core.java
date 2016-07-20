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
package org.dataone.cn.rest.v2.suite;

import org.dataone.cn.rest.v1.suite.*;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.cn.rest.v2.CoreController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v1_1.QueryEngineList;
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
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v2/mockCoreController-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class SuiteTestUnit7Core {

    public static Log log = LogFactory.getLog(SuiteTestUnit7Core.class);

    private CoreController testController;

    @Resource
    public void setTestController(CoreController testController) {
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
            log.info(checksum);
        }
        assertTrue(checksumAlgorithmList.getAlgorithmList().size() == 2);
    }

    @Test
    public void testValidBaseControllerListQueryEngines() throws Exception {

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/" + Constants.RESOURCE_QUERY + "/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        QueryEngineList queryEngineList = null;
        try {
            ModelAndView mav = testController.listQueryEngines(request, response);
            queryEngineList = (QueryEngineList) mav.getModel().get("org.dataone.service.types.v1_1.QueryEngineList");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration " + ex);
        }

        assertNotNull(queryEngineList);
        assertNotNull(queryEngineList.getQueryEngineList());
        for (String engine : queryEngineList.getQueryEngineList()) {
            log.info(engine);
        }
        assertTrue(queryEngineList.getQueryEngineList().size() > 0);
    }

    @Test
    public void testValidBaseControllerGetNode() throws Exception {

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/" + Constants.RESOURCE_NODE + "/urn:node:testcn");

        MockHttpServletResponse response = new MockHttpServletResponse();
        Node v2Node = null;
        try {
            ModelAndView mav = testController.getCapabilities(request, response);
            v2Node = (Node) mav.getModel().get("org.dataone.service.types.v2.Node");

        } catch (ServiceFailure ex) {
            fail("Test misconfiguration " + ex);
        }

        assertNotNull(v2Node);
        assertNotNull(v2Node.getIdentifier());

        // assertTrue(queryEngineList.getQueryEngineList().size() > 0);
    }
}
