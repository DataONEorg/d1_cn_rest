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

package org.dataone.cn.rest.v2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.cn.v2.impl.NodeRegistryServiceImpl;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
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
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v2/mockDiagnosticController-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class DiagnosticControllerTest {

    /** the servlet */
    public static Log log = LogFactory.getLog(DiagnosticControllerTest.class);
    private DiagnosticController testController;

    @Resource
    public void setTestController(DiagnosticController testController) {
        this.testController = testController;
    }
    
   
    @Autowired
    @Qualifier("systemMetadata-valid")
    private ClassPathResource sysMetaResource;


    @Autowired
    @Qualifier("systemMetadata-invalid")
    private ClassPathResource invalidSysMetaResource;
    
    

    
    @Test
    public void testEchoSystemMetadata() throws Exception {
        
        ByteArrayOutputStream sysMetaOutput= new ByteArrayOutputStream();
        BufferedInputStream bInputStream = new BufferedInputStream(sysMetaResource.getInputStream());
        IOUtils.copy(bInputStream, sysMetaOutput);
        
        String sysMetaString = new String(sysMetaOutput.toByteArray());
        log.info(sysMetaString);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockSysMetaFile = new MockMultipartFile("sysmeta", sysMetaOutput.toByteArray());
        
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/diag/sysmeta/");
        request.addFile(mockSysMetaFile);
 
        MockHttpServletResponse response = new MockHttpServletResponse();
        SystemMetadata sysMeta = null;
        try {
            ModelAndView mav = testController.echoSystemMetadata(request, response);
            sysMeta = (SystemMetadata) mav.getModel().get("org.dataone.service.types.v2.SystemMetadata");

            assertNotNull(sysMeta);
            assertFalse(sysMeta.getIdentifier().getValue().isEmpty());

        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Test misconfiguration " + ex);
        }
    }
    @Ignore
    @Test
    public void testEchoInvalidSystemMetadata() throws Exception {
        
        ByteArrayOutputStream sysMetaOutput= new ByteArrayOutputStream();
        BufferedInputStream bInputStream = new BufferedInputStream(invalidSysMetaResource.getInputStream());
        IOUtils.copy(bInputStream, sysMetaOutput);
        
        String sysMetaString = new String(sysMetaOutput.toByteArray());
        log.info(sysMetaString);

        testController.setServletContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);

        MockMultipartFile mockSysMetaFile = new MockMultipartFile("sysmeta", sysMetaOutput.toByteArray());
        
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/diag/sysmeta/");
        request.addFile(mockSysMetaFile);
 
        MockHttpServletResponse response = new MockHttpServletResponse();
        SystemMetadata sysMeta = null;
        try {
            ModelAndView mav = testController.echoSystemMetadata(request, response);

        } catch (InvalidSystemMetadata ex) {
        	assertTrue(true);
        	return;
        }
        fail("should not have reached end of test");
    }
}
