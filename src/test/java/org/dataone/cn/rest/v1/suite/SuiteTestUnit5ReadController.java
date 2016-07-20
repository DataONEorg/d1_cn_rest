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

package org.dataone.cn.rest.v1.suite;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.cn.rest.v1.ReadController;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.util.Constants;
import org.junit.*;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author rwaltz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v1/mockReadController-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class SuiteTestUnit5ReadController {

    Logger logger = Logger.getLogger(SuiteTestUnit5ReadController.class);
    /** the servlet */
    private static final String VERSION_PATH = "/v1";
    private static final String OBJECTS_V1 = "/v1/" + Constants.RESOURCE_OBJECTS + "/";
    private static final String META_REGISTER_V1 = "/v1/" + Constants.RESOURCE_META + "/";


    private ReadController testController;
    @Resource
    public void setTestController(ReadController testController) {
        this.testController = testController;
    }
    private static final String PID = "http%3A%2F%2Ffoo.com%2Fmeta%2F18";

protected static final boolean DEFAULT_READ_ONLY_VALUE = false;

    @Before
    public void before() throws Exception {
        WebApplicationContext  wac = WebApplicationContextUtils.getRequiredWebApplicationContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
        if (wac == null) {
            throw new Exception("cannot find Web Application Context!");
        }

        ProxyWebApplicationContextLoader.SERVLET_CONTEXT.setContextPath("/metacat");
        ProxyWebApplicationContextLoader.SERVLET_CONTEXT.setServletContextName("/d1");

    }

    @Test
    public void testValidResolveController() throws Exception {

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
       
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/v1/resolve/" + PID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        testController.resolve(request, response);


        logger.info("Response is: " + response.getForwardedUrl());

        assertTrue(response.getForwardedUrl().equals("/d1/cn/v1/meta/" + PID));
    }
    @Test
    public void testInvalidPathResolveController() throws Exception {

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
       
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock" + PID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        try {
            testController.resolve(request, response);
        } catch (ServiceFailure ex) {
            logger.info("Response is: " + ex.getMessage());
            assertTrue(ex.getDetail_code().contentEquals("4150"));
        }

        assertTrue(response.getForwardedUrl() == null);
        
    }

}
