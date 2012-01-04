package org.dataone.cn.web.v1;

import org.dataone.cn.rest.filter.v1.ResolveFilter;
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

import org.junit.Before;
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
import org.dataone.cn.ldap.v1.NodeLdapPopulation;
import org.dataone.cn.rest.filter.BufferedHttpResponseWrapper;
import org.dataone.cn.web.ResolveServlet;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.cn.v1.CNCore;
import org.junit.After;
import org.junit.runner.RunWith;


import org.springframework.core.io.FileSystemResourceLoader;  //FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/mockObject-dispatcher.xml", "classpath:/org/dataone/cn/resources/web/mockObject-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class TestingMyResolve {

    private static String resourcePath = "/org/dataone/cn/resources/samples/v1/";
    private static String objectlocationlistUrl = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_1_0_0/dataoneTypes.xsd";
    private static boolean debuggingOutput = true;
    private static boolean useSchemas = true;
    private static Integer nodelistRefreshIntervalSeconds = 120;
    private NodeRegistryService cnLdapCore;
    private NodeLdapPopulation cnLdapPopulation;
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
    public void setTestController(NodeRegistryService cnLdapCore) {
        this.cnLdapCore = cnLdapCore;
    }

    @Resource
    public void setCNLdapPopulation(NodeLdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }

    @Before
    public void before() throws Exception {
        cnLdapPopulation.populateTestMNs();
    }

    @After
    public void after() throws Exception {
        cnLdapPopulation.deletePopulatedNodes();
    }

    @Test
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

    @Test
    public void testDoFilter() throws FileNotFoundException {


        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml");

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);

        String content = new String(responseWrapper.getBuffer());

        assertThat("response contains word 'objectLocationList'", content, containsString("objectLocationList"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testUrlEncodingAscii() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid-disallowed-ascii.xml");

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);

        String content = new String(responseWrapper.getBuffer());

        assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
        assertThat("wonky identifier is escaped in url", content, containsString("aAbBcC__%2F%3Fparam=5%23__12345</url>"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testUrlEncodingNonAscii() throws FileNotFoundException {

        this.nodelistRefreshIntervalSeconds = 13579;
        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid-nonAscii-id.utf8.xml");

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);

        String content = new String(responseWrapper.getBuffer());

        //assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
        assertThat("non-Ascii identifier is escaped in url", content, containsString("%E0%B8%89%E0%B8%B1%E0%B8%99%E0%B8%81%E0%B8%B4%E0%B8%99%E0%B8%81%E0%B8%A3%E0%B8%B0%E0%B8%88%E0%B8%81%E0%B9%84%E0%B8%94%E0%B9%89</url>"));
        this.nodelistRefreshIntervalSeconds = 120;
    }

    @Test
    public void testValidOllXML() throws Exception {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml");

        // examine contents of the response
        assertTrue("response is non-null", responseWrapper.getBufferSize() > 0);
        assertTrue("response is non-null", responseWrapper.getBuffer().length > 0);

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

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error.xml");

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

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error.xml");

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

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error-docNotFound.xml");

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
    public void testSystemMetadataError() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-unregisteredNode.xml");

        String content = new String(responseWrapper.getBuffer());
        if (debuggingOutput) {
            System.out.println("testSystemMetadataError");
            System.out.println("===== output =====");
            System.out.print(content.toString());
            System.out.print("http response code = " + responseWrapper.getStatus());
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
        assertThat("systemMetadata unregistered node error produced-3", content, containsString("unregistered Node identifier"));
    }

    @Test
    public void testSystemMetadataInvalidVsSchema() throws FileNotFoundException {

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-malformedXML.xml");

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

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-malformedXML.xml");

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

        BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-noReplicasCompletedStatus.xml");

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

    @Test
    public void testLookupBaseURL() {

        // building up a new ResolveFilter with the appropriate parameters
        ResourceLoader fsrl = new FileSystemResourceLoader();
        ServletContext sc = new MockServletContext("src/main/webapp", fsrl);
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

        ResolveFilter rf = new ResolveFilter();
        rf.setUseSchemaValidation(useSchemas);
        rf.setNodeListRetrieval(cnLdapCore);
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

        ResolveServlet testResolve = new ResolveServlet();

        try {
            testResolve.setOutput(resourcePath + "systemMetadata-valid.xml");
        } catch (IOException e) {
            fail("Test misconfiguration - IOException" + e);
        }

        FilterChain chain = new PassThroughFilterChain(testResolve);

        HttpServletResponse response = new MockHttpServletResponse();
        // need to wrap the response to examine
        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);

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
            url = rf.lookupBaseURLbyNode("sq1d");
        } catch (ServiceFailure e) {
            fail("baseURLmap lookup error: " + e);
        }
        if (url == null) {
            fail("baseURLmap lookup error: returned null value");
        } else if (url.isEmpty()) {
            fail("baseURLmap lookup error: url returned is empty");
        }

    }

//	@Test
    public void testDestroy() {
        fail("Not yet implemented"); // TODO
    }

    // ==========================================================================================================
    private BufferedHttpResponseWrapper callDoFilter(String outputFilename) {

        ResourceLoader fsrl = new FileSystemResourceLoader();
        ServletContext sc = new MockServletContext("src/main/webapp", fsrl);
        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

        ResolveFilter rf = new ResolveFilter();
        rf.setUseSchemaValidation(this.useSchemas);
        rf.setNodelistRefreshIntervalSeconds(this.nodelistRefreshIntervalSeconds);
        rf.setNodeListRetrieval(cnLdapCore);
        try {
            rf.init(fc);
        } catch (ServletException se) {
            //se.printStackTrace();
            fail("servlet exception at ResolveFilter.init(fc)");
        }

        MockHttpServletRequest request = new MockHttpServletRequest(fc.getServletContext(), null, "/resolve/12345");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        ResolveServlet testResolve = new ResolveServlet();

        try {

            testResolve.setOutput(resourcePath + outputFilename);
        } catch (IOException ex) {
            fail("Test misconfiguration - IOException " + ex);
        }

        FilterChain chain = new PassThroughFilterChain(testResolve);

        HttpServletResponse response = new MockHttpServletResponse();
        // need to wrap the response to examine
        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);

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
