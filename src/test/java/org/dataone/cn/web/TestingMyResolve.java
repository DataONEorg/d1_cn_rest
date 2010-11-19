package org.dataone.cn.web;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
//import org.springframework.test.web.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import junit.framework.TestCase;
import org.dataone.cn.rest.filter.*;
import org.dataone.service.exceptions.ServiceFailure;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;


import org.springframework.core.io.*;  //FileSystemResourceLoader;
import org.springframework.mock.web.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public class TestingMyResolve {
	private static String objectlocationlistUrl = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5_1/dataoneTypes.xsd";
	private static String validTestingNodelistLocation = "src/test/resources/resolveTesting/nodelist_0_5_valid.xml";
	private static String deployedNodelistLocationURL = "http://cn-dev.dataone.org/cn/node";
	private static boolean debuggingOutput = false;
	private static String useSchemasString = "true";

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
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInit() {

		// Init reads in the parameters from webapp configuration file
		// (not caching the nodelist anymore - no reason to...)
		 
		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation",useSchemasString);
		fc.addInitParameter("nodelistRefreshIntervalSeconds","1234");
		fc.addInitParameter("nodelistLocation", validTestingNodelistLocation);
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		if (rf.getRefreshInterval() != 1234) {
			fail("failed to set nodelistRefreshIntervalSeconds parameter");
		}
	}

	@Test
	public void testRefreshIntervalErrorCatching() {

		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("nodelistRefreshIntervalSeconds","should be a number but is not");
		fc.addInitParameter("nodelistLocation", validTestingNodelistLocation);
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			assertThat("refreshInterval error checking", se,  instanceOf(ServletException.class));
			return;
		}
		fail("did not catch refreshInterval bad-value error");
	}
	
	@Test
	public void testuseSchemaValidationFlag() {

		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation","neitherTrueNorFalse");
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			assertThat("useSchema error checking", se,  instanceOf(ServletException.class));
			return;
		}
		fail("did not catch useSchema bad-value error");
	}
	
	@Test
	public void testLookupBaseURL() {	
		 
		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation",useSchemasString);
		fc.addInitParameter("nodelistLocation", validTestingNodelistLocation);
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		// read the baseURLmap to make sure it's working
		String url = null;
		try {
			url = rf.lookupBaseURLbyNode("daacmn");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error: "+ e);
		}
		if (url == null) 
			fail("baseURLmap lookup error: returned null value");
		else if(url.isEmpty())
			fail("baseURLmap lookup error: url returned is empty");	

	}
	
	@Test
	public void testNodeListUrlLookup() {	
		 
		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation",useSchemasString);
		fc.addInitParameter("nodelistLocation", deployedNodelistLocationURL);
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		// read the baseURLmap to make sure it's working
		String url = null;
		try {
			url = rf.lookupBaseURLbyNode("cn-dev");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error: "+ e);
		}
		if (url == null) 
			fail("baseURLmap lookup error: returned null value");
		else if(url.isEmpty())
			fail("baseURLmap lookup error: url returned is empty");	

	}


	@Test
	public void testNodeListNullBaseURLError() throws FileNotFoundException {

		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation",useSchemasString);
		fc.addInitParameter("nodelistLocation", "src/test/resources/resolveTesting/nodelist_0_5_nullBaseURL.xml");
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}
		
		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", "src/test/resources/resolveTesting/nodelist_0_5_nullBaseURL.xml");
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("NodeList null baseURL error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("Nodelist null baseURL error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("Nodelist null baseURL error produced-3", content, containsString("Error parsing Nodelist: cannot get baseURL"));
	
	}

	@Test
	public void testNodeListInvalidVsSchemaError() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation","true");
		settings.put("nodelistLocation", "src/test/resources/resolveTesting/nodelist_0_5_invalid_schema.xml");
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("NodeList Invalid vs Schema error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("Nodelist Invalid vs Schema error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("Nodelist Invalid vs Schema error produced-3", content, containsString("document invalid against NodeList schema"));
	}

	@Test
	public void testNodeListMalformedXML() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation","true");
		settings.put("nodelistLocation", "src/test/resources/resolveTesting/nodelist_0_5_malformedXML.xml");
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("NodeList null baseURL error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("Nodelist null baseURL error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("Nodelist null baseURL error produced-3", content, containsString("Cannot parse NodeList"));
	
	}

	
	
//	@Test
	public void testDestroy() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testDoFilter() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistRefreshIntervalSeconds","13579");
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);
		
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
		String content = new String(responseWrapper.getBuffer());

		assertThat("response contains word 'objectLocationList'", content, containsString("objectLocationList"));
	}
	
	
	@Test
	public void testUrlEncodingAscii() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistRefreshIntervalSeconds","13579");
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid-disallowed-ascii.xml", settings);
		
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
		String content = new String(responseWrapper.getBuffer());

		assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
		assertThat("wonky identifier is escaped in url", content, containsString("aAbBcC__%2F%3Fparam=5%23__12345</url>"));
	}

	// TODO: seems to be importing the nonAscii improperly, as a different character string
	//  so the test needs work.
//	@Test
	public void testUrlEncodingNonAscii() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistRefreshIntervalSeconds","13579");
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid-nonAscii-id.xml", settings);
		
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
		String content = new String(responseWrapper.getBuffer());

		//assertThat("wonky identifier is not escaped", content, containsString("<identifier>aAbBcC__/?param=5#__12345"));
		assertThat("non-Ascii identifier is escaped in url", content, containsString("%A9%u2014%u03C0%B0%u2018%u03C0%B0%u221A%u2013%AE%B0%u2030%A5%C8</url>"));
	}
	
	
	@Test
	public void testValidOllXML() throws Exception {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);
		
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
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
			throw new ServiceFailure("4150","error: malformed URL for schema: " + objectlocationlistUrl);	
		} catch (IOException e) {
			throw new ServiceFailure("4150","Error connecting to schema: " + objectlocationlistUrl);
		} catch (SAXException e) {
			throw new ServiceFailure("4150","error parsing schema for validation: " + objectlocationlistUrl );
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
			assertThat("oll schema testing error checking", e,  instanceOf(SAXParseException.class));
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

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		assertThat("testing negative control: can catch errors", content, not(containsString("errorCode=\"blahblah\"")));
		
	}
	
	@Test
	public void testMetacatErrorGeneric() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("metacat error code forwards", content, containsString("errorCode=\"123456\""));
		assertThat("metacat error code forwards", content, containsString("detailCode=\"987654\""));
		assertThat("metacat error code forwards", content, containsString("generic error from the /meta service to test forwarding capabilities of resolve."));
	
	}
	
	@Test
	public void testMetacatErrorDocNotFound() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error-docNotFound.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("metacat error code forwards", content, containsString("errorCode=\"404\""));
		assertThat("metacat error code forwards", content, containsString("detailCode=\"1000\""));
		assertThat("metacat error code forwards", content, containsString("Document not found"));
	
	}
	
	@Test
	public void testSystemMetadataError() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-unregisteredNode.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("systemMetadata unregistered node error produced-3", content, containsString("unregistered Node identifier"));
	}

	@Test
	public void testSystemMetadataInvalidVsSchema() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-malformedXML.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("systemMetadata unregistered node error produced-3", content, containsString("Error parsing /meta output"));
	}
	
	@Test
	public void testSystemMetadataMalformedXMLError() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-malformedXML.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\'500\'"));
		assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\'4150\'"));
		assertThat("systemMetadata unregistered node error produced-3", content, containsString("Error parsing /meta output"));
	}

	@Test
	public void testSystemMetadataNoReplicasCompleted() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemaValidation",useSchemasString);
		settings.put("nodelistLocation", validTestingNodelistLocation);
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-noReplicasCompletedStatus.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		if (debuggingOutput) {
			System.out.println("===== output =====");
			System.out.print(content.toString());
			System.out.println("------------------");
		}
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("systemMetadata unregistered node error produced-1", content, containsString("errorCode=\'404\'"));
		assertThat("systemMetadata unregistered node error produced-2", content, containsString("detailCode=\'4140\'"));
		assertThat("systemMetadata unregistered node error produced-3", content, containsString("The requested object is not presently available"));
	}

	
	/* to test the refresh ability, the test points ResolveFilter to a nodelistLocation and first copies
	 * the original valid nodelist to that location and does a baseURL lookup for a nodeIdentifier
	 * then copies an altered nodelist file with different baseURL for the same nodeIdentifier,
	 * sleeps for longer than the refresh interval, and repeats the lookup.
	 * The results should be different.
	 */
	
	@Test
	public void testNodeListRefresh() throws InterruptedException, IOException {

		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemaValidation",useSchemasString);
		fc.addInitParameter("nodelistRefreshIntervalSeconds","2");
		
		String tmpNodelistLocation = "src/test/resources/resolveTesting/tmpNodelistCachingTest.xml";
		fc.addInitParameter("nodelistLocation", tmpNodelistLocation);

		// the two source versions of the nodelist
		File origNodelistFile = new File(validTestingNodelistLocation);
		File newNodelistFile = new File("src/test/resources/resolveTesting/nodelistCachingTest.xml");

		File nodelistLocation = new File(tmpNodelistLocation);

		// copy the original nodelist file to the tmp location where Resolve will be looking for it
		FileReader in = new FileReader(origNodelistFile);
		FileWriter out = new FileWriter(nodelistLocation);

		int c;
		while ((c = in.read()) != -1)  out.write(c);
		in.close();
		out.close();
		
		ResolveFilter rf = new ResolveFilter();

		try {
			rf.init(fc);
		} catch (ServletException se) {
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		// lookup a baseURL
		String url = null;
		try {
			url = rf.lookupBaseURLbyNode("daacmn");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error: "+ e);
		}
		if (url == null) 
			fail("baseURLmap lookup error: returned null value");
		else if(url.isEmpty())
			fail("baseURLmap lookup error: url returned is empty");	

		// the wait is longer than the refresh interval
		Thread.sleep( 5 * 1000);
		
		// after the refresh interval is over, we still shouldn't refresh unless
		// the nodelist has changed.
		// lookup the baseURL again
		String url2 = null;
		try {
			url2 = rf.lookupBaseURLbyNode("daacmn");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error: "+ e);
		} finally {
			nodelistLocation.delete();
		}
		if (url2 == null) 
			fail("baseURLmap lookup error: returned null value");
		else if(url2.isEmpty())
			fail("baseURLmap lookup error: url returned is empty");	

		// urls should be different
		assertTrue("cache should not refresh unless the file changes.",url.equals(url2));
		
		
		
		// copy the new nodelist to the designated location
		in = new FileReader(newNodelistFile);
		out = new FileWriter(nodelistLocation);

		while ((c = in.read()) != -1)  out.write(c);
		in.close();
		out.close();

		// lookup the baseURL again
		String url3 = null;
		try {
			url3 = rf.lookupBaseURLbyNode("daacmn");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error: "+ e);
		} finally {
			nodelistLocation.delete();
		}
		if (url3 == null) 
			fail("baseURLmap lookup error: returned null value");
		else if(url3.isEmpty())
			fail("baseURLmap lookup error: url returned is empty");	

		// urls should be different
		assertFalse("cache refresh failed - should have returned different url string. Got: " + url + " and " + url3,
				url.equals(url3));

	}
	
	
	// ==========================================================================================================

	private BufferedHttpResponseWrapper callDoFilter(String outputFilename, Hashtable<String, String> params) {
	
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		
		Enumeration<String> pNames = params.keys();
		while (pNames.hasMoreElements()) {
			String name = pNames.nextElement(); 
			String val = params.get(name);
			fc.addInitParameter(name,val);		
		}
		
		ResolveFilter rf = new ResolveFilter();	
		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		MockHttpServletRequest request= new MockHttpServletRequest(fc.getServletContext(), null, "/resolve/12345");
		request.addHeader("accept", (Object) "text/xml");		
		request.setMethod("POST");
		
		ResolveServlet testResolve = new ResolveServlet();
		
		try {
			testResolve.setOutput(outputFilename);
		} catch (FileNotFoundException e) {
			fail("Test misconfiguration - output file not found" + e);
		}

		FilterChain chain = new PassThroughFilterChain(testResolve);

		HttpServletResponse response = new MockHttpServletResponse();	
		// need to wrap the response to examine
		BufferedHttpResponseWrapper responseWrapper =
            new BufferedHttpResponseWrapper((HttpServletResponse) response);
		
		try {
			rf.doFilter(request,responseWrapper,chain);
		} catch (ServletException se) {
			fail("servlet exception at ResolveFilter.doFilter(): " + se);
		} catch (IOException ioe) {
			fail("IO exception at ResolveFilter.doFilter(): " + ioe);
		}
		return responseWrapper;
	}
	

	class XSDValidationErrorHandler extends DefaultHandler  {

		public void error(SAXParseException e) throws SAXParseException {
			throw e;
		}
		
	}
}
