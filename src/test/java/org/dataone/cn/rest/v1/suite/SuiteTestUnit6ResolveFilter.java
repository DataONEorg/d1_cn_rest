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

import org.dataone.cn.rest.v1.ResolveFilter;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
//import org.springframework.test.web.*;
import javax.annotation.Resource;

import org.junit.Test;

import org.dataone.service.exceptions.ServiceFailure;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.http.BufferedHttpResponseWrapper;
import org.dataone.cn.rest.ResolveServlet;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.cn.v1.NodeRegistryService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;

import org.springframework.core.io.FileSystemResourceLoader;  //FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v1/mockResolveFilter-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class SuiteTestUnit6ResolveFilter {

    private static String objectlocationlistUrl = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_1_0_2/dataoneTypes.xsd";
    private static boolean debuggingOutput = true;
    private static boolean useSchemas = true;
    private static Integer nodelistRefreshIntervalSeconds = 120;
    public static Log logger = LogFactory.getLog(SuiteTestUnit6ResolveFilter.class);

    // need to test that resolveFilter behaves properly under various conditions:
    // (general)
    // 1. All's well
    // (init)
    // 2. useSchemaValidation parameter is neither string "true" or "false"
    // 3. refreshInterval parameter is not a number.
    // (runtime)
    // 3. NodeList unavailable
    // 4. NodeList malformed (not valid against the schema)
    // 5. 1 or more empty baseURLs in the nodes of the nodelist
    // 6. can forward error response from metacat
    // 7. unregistered node in systemMetadata
    // 8. invalid systemMetadata document returned
    // 9. no replica nodes with completed status in the systemmetadata document
    // 10. resolveFilter creates valid objectlocationlists
    // 11. resolveFilter creates valid error xml documents
    //
    // It's impractical for resolve to handle some runtime errors:
    // so am not testing for them:
    // (note that runtime errors include errors in the nodelist, as it changes between
    // system deployments through a separate service)
    // 1. mangled urls (unfollowable)
    // 2. connection timeout from metacat (/meta service)
    @Resource
    @Qualifier("nodeRegistryServiceV1")
    private NodeRegistryService nodeRegistryService;

    @Resource
    @Qualifier("systemMetadata-valid")
    private ClassPathResource systemMetadataValidResource;

    @Resource
    @Qualifier("systemMetadata-valid-disallowed-ascii")
    private ClassPathResource systemMetadataValidDisallowedAsciiResource;

    @Resource
    @Qualifier("systemMetadata-valid-nonAscii-id-utf8")
    private ClassPathResource systemMetadataValidNonAsciiIdUtf8Resource;

    @Resource
    @Qualifier("metacat-error")
    private ClassPathResource metacatErrorResource;

    @Resource
    @Qualifier("metacat-error-docNotFound")
    private ClassPathResource metacatErrorDocNotFoundResource;

    @Resource
    @Qualifier("systemMetadata-malformedXML")
    private ClassPathResource systemMetadataMalformedXMLResource;

    @Resource
    @Qualifier("systemMetadata-noReplicasCompletedStatus")
    private ClassPathResource systemMetadataNoReplicasCompletedStatusResource;

    @Resource
    @Qualifier("systemMetadata-unregisteredNode")
    private ClassPathResource systemMetadataUnregisteredNodeResource;

    @Resource
    @Qualifier("systemMetadata-disabledReplicaNodeIdentifier")
    private ClassPathResource systemMetadataDisabledReplicaNodeIdentifierResource;

    /*
     @Ignore("not ready yet") @Test
     public void testInit() {

     // Init reads in the parameters from webapp configuration file
     // (not caching the nodelist anymore - no reason to...)
     MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

     ResolveFilter rf = new ResolveFilter();
     rf.setNodelistRefreshIntervalSeconds(new Integer(1234));
     try {
     rf.init(fc);
     } catch (ServletException se) {
     //se.printStackTrace();
     fail("servlet exception at ResolveFilter.init(fc)");
     }

     if (rf.getNodelistRefreshIntervalSeconds() != 1234) {
     fail("failed to set nodelistRefreshIntervalSeconds parameter");
     }
     }
     */
    @Test
    public void testDoFilter() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidResource);
        String content = new String(responseWrapper.getBuffer());
        logger.info(content);
        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));

        assertThat("response contains word 'objectLocationList'", content, containsString("objectLocationList"));
        this.nodelistRefreshIntervalSeconds = 120;
    }
    
    
    /**
     *  this test is for testing the response behavior with a HEAD method
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testDoFilterHEAD() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidResource,"HEAD");
        String content = new String(responseWrapper.getBuffer());
        logger.info(content);
        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 307", responseWrapper.getStatus() == responseWrapper.SC_TEMPORARY_REDIRECT);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));

        assertThat("response contains word 'objectLocationList'", content, containsString("objectLocationList"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testUrlEncodingAscii() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidDisallowedAsciiResource);

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));
        String content = new String(responseWrapper.getBuffer());

        assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
        assertThat("wonky identifier is escaped in url", content, containsString("aAbBcC__%2F%3Fparam=5%23__12345</url>"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testUrlEncodingNonAscii() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidNonAsciiIdUtf8Resource);

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));
        String content = new String(responseWrapper.getBuffer());

        //assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
        assertThat("non-Ascii identifier is escaped in url", content, containsString("%E0%B8%89%E0%B8%B1%E0%B8%99%E0%B8%81%E0%B8%B4%E0%B8%99%E0%B8%81%E0%B8%A3%E0%B8%B0%E0%B8%88%E0%B8%81%E0%B9%84%E0%B8%94%E0%B9%89</url>"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testValidOllXML() throws Exception {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataValidResource);

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));
        String content = new String(responseWrapper.getBuffer());

        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // create dom from output then validate against schema

        Validator validator;
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            URL xsdUrl = new URL(objectlocationlistUrl);
            URLConnection xsdUrlConnection = xsdUrl.openConnection();
            InputStream xsdUrlStream = xsdUrlConnection.getInputStream();
            Source schemaFile = new StreamSource(xsdUrlStream);
            Schema schema = factory.newSchema(schemaFile);
            validator = schema.newValidator();
            XSDValidationErrorHandler xsdveh = new XSDValidationErrorHandler();
            validator.setErrorHandler(xsdveh);
        } catch (MalformedURLException e) {
            throw new ServiceFailure("4150", "error: malformed URL for schema: " + objectlocationlistUrl);
        } catch (IOException e) {
            throw new ServiceFailure("4150", "Error connecting to schema: " + objectlocationlistUrl);
        } catch (SAXException e) {
            throw new ServiceFailure("4150", "error parsing schema for validation: " + objectlocationlistUrl);
        }

        // validate the output
        try {
            StreamSource ss = new StreamSource(new StringReader(content));
            validator.validate(ss);
        } catch (SAXParseException e) {
            fail("invalid against schema for returned objectlocationlist: " + e);
        } catch (SAXException e) {
            fail("invalid xml for returned objectlocationlist: " + e);
        } catch (IOException e) {
            fail("IO error during xml validation test: " + e);
        }

        // follow up with test to make sure we can catch the schema error in the first place!
        String invalidContent = content.replace("baseURL>", "baseHURL>");
        try {
            StreamSource ss = new StreamSource(new StringReader(invalidContent));
            validator.validate(ss);
        } catch (SAXParseException e) {
            assertThat("oll schema testing error checking", e, instanceOf(SAXParseException.class));
            return;
        } catch (SAXException e) {
            fail("invalid xml for returned objectlocationlist: " + e);
        } catch (IOException e) {
            fail("IO error during xml validation test: " + e);
        }
        fail("did not catch invalid OLL error");
    }

    @Test
    public void testingNegativeControl() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(metacatErrorResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);
        assertThat("testing negative control: can catch errors", content, not(containsString("errorCode=\"blahblah\"")));

    }

    @Test
    public void testMetacatErrorGeneric() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(metacatErrorResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);

        assertThat("metacat error code forwards", content, containsString("errorCode=\"500\""));
        assertThat("metacat error code forwards", content, containsString("detailCode=\"4150\""));
        assertThat("metacat error code forwards", content, containsString("generic error from the /meta service to test forwarding capabilities of resolve."));

    }

    @Test
    public void testMetacatErrorDocNotFound() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(metacatErrorDocNotFoundResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine status code
        int httpStatus = responseWrapper.getStatus();
        assertTrue("error response status is 404", httpStatus == 404);

        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);

        assertThat("metacat error code forwards", content, containsString("errorCode=\"404\""));
        assertThat("metacat error code forwards", content, containsString("detailCode=\"4140\""));
        assertThat("metacat error code forwards", content, containsString("Document not found"));

    }

    @Test
    public void testSystemMetadataInvalidVsSchema() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataMalformedXMLResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine status code
        int httpStatus = responseWrapper.getStatus();
        assertTrue("error response status is 500", httpStatus == 500);

        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);

        assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\"500\""));
        assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\"4150\""));
        assertThat("systemMetadata unregistered node error produced-3", content, containsString("Error parsing /meta output"));
    }

    @Test
    public void testSystemMetadataMalformedXMLError() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataMalformedXMLResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine status code
        int httpStatus = responseWrapper.getStatus();
        assertTrue("error response status is 500", httpStatus == 500);

        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);

        assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\"500\""));
        assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\"4150\""));
        assertThat("systemMetadata unregistered node error produced-3", content, containsString("Error parsing /meta output"));
    }

    @Test
    public void testSystemMetadataNoReplicasCompleted() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataNoReplicasCompletedStatusResource);

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.println("------------------");
        }
        // examine status code
        int httpStatus = responseWrapper.getStatus();
        assertTrue("error response status is 404", httpStatus == 404);

        // examine contents of the response
        assertTrue("response is non-null-(1)", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null-(2)", responseWrapper.getBuffer().length > 0);

        assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\"404\""));
        assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\"4140\""));
        assertThat("systemMetadata unregistered node error produced-3", content, containsString("The requested object is not presently available"));
    }
    /*
     * Test the situation where a node is reported to have a replica
     * but no record of that node exists
     * Bug #2423: cn.resolve() chokes on bad nodeIdentifiers
     */

    @Test
    public void testSystemMetadataSpuriousNodeEntry() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataUnregisteredNodeResource);

        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));
        String content = new String(responseWrapper.getBuffer());
        assertFalse("There should be not be a node entry, but there is!", content.contains("urn:node:sqrf"));
        assertTrue("There should be a node entry, but there is not!", content.contains("urn:node:testcn"));
    }
    /*
     * Test the situation where a node state is not UP
     * 
     * Bug #2423: cn.resolve() chokes on bad nodeIdentifiers
     */

    @Test
    public void testSystemMetadataDisabledNodeReplica() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter(systemMetadataDisabledReplicaNodeIdentifierResource);

        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);
        assertTrue("status must be 303", responseWrapper.getStatus() == responseWrapper.SC_SEE_OTHER);
        assertTrue("Http Header Location must be set", responseWrapper.containsHeader("Location"));
        String content = new String(responseWrapper.getBuffer());
        assertFalse("There should be not be a node entry, but there is!", content.contains("urn:node:sq1sh"));
        assertTrue("There should be a node entry, but there is not!", content.contains("urn:node:testcn"));
    }

    @Test
    public void testLookupBaseURL() {

        // building up a new ResolveFilter with the appropriate parameters
        ResourceLoader fsrl = new FileSystemResourceLoader();
        ServletContext sc = new MockServletContext("src/main/webapp", fsrl);
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

        ResolveFilter rf = new ResolveFilter();
        rf.setUseSchemaValidation(useSchemas);
        rf.setNodeListRetrieval(nodeRegistryService);
        try {
            rf.init(fc);
        } catch (ServletException se) {
            //se.printStackTrace();
            fail("servlet exception at ResolveFilter.init(fc)");
        }
        Hashtable<String, String> settings = new Hashtable<String, String>();

        MockHttpServletRequest request = new MockHttpServletRequest(fc.getServletContext(), null, "/resolve/12345");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        ResolveServlet testResolve = null;
        try {
            testResolve = new ResolveServlet(systemMetadataValidResource);

        } catch (IOException e) {
            fail("Test misconfiguration - IOException" + e);
        }

        FilterChain chain = new PassThroughFilterChain(testResolve);

        HttpServletResponse response = new MockHttpServletResponse();
        // need to wrap the response to examine
        BufferedHttpResponseWrapper responseWrapper
                = new BufferedHttpResponseWrapper((HttpServletResponse) response);

        try {
            rf.doFilter(request, responseWrapper, chain);
        } catch (ServletException se) {
            fail("servlet exception at ResolveFilter.doFilter(): " + se);
        } catch (IOException ioe) {
            fail("IO exception at ResolveFilter.doFilter(): " + ioe);
        }
        // read the baseURLmap to make sure it's working
        String url = null;
        try {
            url = rf.lookupBaseURLbyNode("urn:node:testcn");
        } catch (ServiceFailure e) {
            fail("baseURLmap lookup error: " + e);
        }
        if (url == null) {
            fail("baseURLmap lookup error: returned null value");
        } else if (url.isEmpty()) {
            fail("baseURLmap lookup error: url returned is empty");
        }

    }

    // ==========================================================================================================
    private BufferedHttpResponseWrapper callDoFilter(ClassPathResource xmlResource) {
        return callDoFilter(xmlResource, "GET");
    }
    
    
    private BufferedHttpResponseWrapper callDoFilter(ClassPathResource xmlResource, String method) {

        ResourceLoader fsrl = new FileSystemResourceLoader();
        ServletContext sc = new MockServletContext("src/main/webapp", fsrl);
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

        ResolveFilter rf = new ResolveFilter();
        rf.setUseSchemaValidation(this.useSchemas);
        rf.setNodelistRefreshIntervalSeconds(this.nodelistRefreshIntervalSeconds);
        rf.setNodeListRetrieval(nodeRegistryService);
        try {
            rf.init(fc);
        } catch (ServletException se) {
            //se.printStackTrace();
            fail("servlet exception at ResolveFilter.init(fc)");
        }

        MockHttpServletRequest request = new MockHttpServletRequest(fc.getServletContext(), null, "/resolve/12345");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod(method);
        ResolveServlet testResolve = null;
        try {
            testResolve = new ResolveServlet(xmlResource);

        } catch (IOException ex) {
            fail("Test misconfiguration - IOException " + ex);
        }

        FilterChain chain = new PassThroughFilterChain(testResolve);

        HttpServletResponse response = new MockHttpServletResponse();
        // need to wrap the response to examine
        BufferedHttpResponseWrapper responseWrapper
                = new BufferedHttpResponseWrapper((HttpServletResponse) response);

        try {
            rf.doFilter(request, responseWrapper, chain);
        } catch (ServletException se) {
            fail("servlet exception at ResolveFilter.doFilter(): " + se);
        } catch (IOException ioe) {
            fail("IO exception at ResolveFilter.doFilter(): " + ioe);
        }
        return responseWrapper;
    }

    class XSDValidationErrorHandler extends DefaultHandler {

        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }
    }
}
