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

package org.dataone.cn.rest;

import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.Resource;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.dataone.cn.rest.http.BufferedHttpResponseWrapper;
import org.dataone.cn.rest.ServiceDisableFilter;
import org.dataone.cn.rest.ResolveServlet;
import org.dataone.cn.rest.ResolveServlet;
import org.dataone.cn.rest.ServiceDisableFilter;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.ExceptionHandler;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/mockServiceDisableFilter-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class ServiceDisableFilterTest {

    @Resource
    @Qualifier("systemMetadata-valid")
    private ClassPathResource systemMetadataValidResource;

    @Test
    public void testInit() {

        // Init reads in the parameters from webapp configuration file
        // (not caching the nodelist anymore - no reason to...)
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "DisableRestServiceFilter");

        ServiceDisableFilter filter = new ServiceDisableFilter();
        try {
            filter.init(fc);
        } catch (ServletException se) {
            //se.printStackTrace();
            fail("servlet exception at DisableRestServiceFilter.init(fc)");
        }
    }

    @Test
    public void testDoFilter_Disabled() throws FileNotFoundException {

    	Settings.getResetConfiguration().setProperty("cn.service.disable",true);
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidResource);


        // examine contents of the response
        assertTrue("response should be non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response should be non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 500", responseWrapper.getStatus() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        try {
        	ByteArrayInputStream is = new ByteArrayInputStream(responseWrapper.getBuffer());
			BaseException be = ExceptionHandler.deserializeXml(is, "aDefaultMessage");
			System.out.println(be.getDescription());
			assertTrue("response content should be a ServiceFailure", be instanceof ServiceFailure);

			
        } catch (Exception e) {
			e.printStackTrace();
			fail("encountered Exception when deserializing the response content");
		}
    }
    
    @Test
    public void testDoFilter_Enabled() throws FileNotFoundException {

    	Settings.getResetConfiguration().setProperty("cn.service.disable",false);
    	
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidResource);

        // examine contents of the response
        assertTrue("response should be non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response should be non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 200", responseWrapper.getStatus() == HttpServletResponse.SC_OK);

        try {
        	System.out.println(new String(responseWrapper.getBuffer()));
        	ByteArrayInputStream is = new ByteArrayInputStream(responseWrapper.getBuffer());
			SystemMetadata smd = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, is);
			
        } catch (Exception e) {
			e.printStackTrace();
			fail("content returned should be SystemMetadata");
		}
    }

    
//    @Test
//    public void testDoFilter_DefaultFalse() throws FileNotFoundException {
//
//    	Settings.getResetConfiguration();
//    	
//        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml");
//
//        // examine contents of the response
//        assertTrue("response should be non-null", responseWrapper.getBufferSize() > 0);
//        assertTrue("response should be non-null", responseWrapper.getBuffer().length > 0);
//        assertTrue("status must be 200", responseWrapper.getStatus() == HttpServletResponse.SC_OK);
//
//        try {
//        	System.out.println(new String(responseWrapper.getBuffer()));
//        	ByteArrayInputStream is = new ByteArrayInputStream(responseWrapper.getBuffer());
//			SystemMetadata smd = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, is);
//			
//        } catch (Exception e) {
//			e.printStackTrace();
//			fail("content returned should be SystemMetadata");
//		}
//    }

 

    // ==========================================================================================================
    private BufferedHttpResponseWrapper callDoFilter(ClassPathResource xmlResource) {

        ResourceLoader fsrl = new FileSystemResourceLoader();
        ServletContext sc = new MockServletContext("src/main/webapp", fsrl);
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "DisableRestServiceFilter");

        ServiceDisableFilter filter = new ServiceDisableFilter();
        try {
            filter.init(fc);
        } catch (ServletException se) {
            //se.printStackTrace();
            fail("servlet exception at DisableRestServiceFilter.init(fc)");
        }

        MockHttpServletRequest request = new MockHttpServletRequest(fc.getServletContext(), null, "/resolve/12345");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        ResolveServlet testResolve = null;
        try {
            testResolve = new ResolveServlet(xmlResource);

        } catch (IOException ex) {
            fail("Test misconfiguration - IOException " + ex);
        }

        FilterChain chain = new PassThroughFilterChain(testResolve);

        HttpServletResponse response = new MockHttpServletResponse();
        // need to wrap the response to examine
        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);

        try {
            filter.doFilter(request, responseWrapper, chain);
        } catch (ServletException se) {
            fail("servlet exception at DisableRestServiceFilter.doFilter(): " + se);
        } catch (IOException ioe) {
            fail("IO exception at DisableRestServiceFilter.doFilter(): " + ioe);
        }
        return responseWrapper;
    }

    class XSDValidationErrorHandler extends DefaultHandler {

        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }
    }
}
