package org.dataone.cn.web;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
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


import org.springframework.core.io.*;  //FileSystemResourceLoader;
import org.springframework.mock.web.*;


public class TestingMyResolve {

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

		// need to test that init behaves properly under various conditions:
		// 1. All's well
		// 2. NodeList unavailable
		// 3. NodeList malformed (not valid against the schema)
		// 4. Nodes with some null baseURLs - 
		// 5. unknown init parameters   [found in webapp configurations]
		// 6. expires parameter is not a number.
		 
		// building up a new ResolveFilter with the appropriate parameters
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		fc.addInitParameter("useSchemas","false");
		fc.addInitParameter("nodelistRefreshInterval","1234");
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			//se.printStackTrace();
			fail("servlet exception at ResolveFilter.init(fc)");
		}

		if (rf.getRefreshInterval() != 1234) {
			fail("failed to set nodelistRefreshInterval parameter");
		}
		// read the baseURLmap to make sure init's working
		String url = null;
		try {
			url = rf.lookupBaseURLbyNode("http://cn-ucsb-1.dataone.org");
		} catch (ServiceFailure e) {
			fail("baseURLmap lookup error");
		}
		if (url == null) {
			fail("baseURLmap not populated");
		}
	}

//	@Test
	public void testDestroy() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testDoFilter() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemas","false");
		settings.put("nodelistRefreshInterval","13579");
		
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-valid.xml", settings);
		
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
		String content = new String(responseWrapper.getBuffer());
//		System.out.print(content.toString());
		assertThat("response contains word 'objectLocationList'", content, containsString("objectLocationList"));

		
//		TODO more sophisticated tests can catch more potential errors in the XSLT.  depends on crafting more fake metadata.

//		JUnitMatchers matcher = new JUnitMatchers();
//		assertEquals(content,"<?xml version=\"1.0\" encoding=\"UTF-8\"?><locations identifier=\"Identifier0\"><location node=\"ReplicaMemberNode0\" " 
//				+"href=\"http://ReplicaMemberNode0object?id=Identifier0\"/><location node=\"ReplicaMemberNode2\" "
//				+"href=\"http://ReplicaMemberNode2object?id=Identifier0\"/></locations>");	

	}
	
	@Test
	public void testMetacatError() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemas","false");
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("metacat-error.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		System.out.println("===== output =====");
		System.out.print(content.toString());
		System.out.println("------------------");
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("response contains word 'error'", content, containsString("error"));
	
	}

	@Test
	public void testSystemMetadataError() throws FileNotFoundException {

		Hashtable<String, String> settings = new Hashtable<String, String>();
		settings.put("useSchemas","false");
		BufferedHttpResponseWrapper responseWrapper = callDoFilter("systemMetadata-unregisteredNode.xml", settings);

		String content = new String(responseWrapper.getBuffer());
		System.out.println("===== output =====");
		System.out.print(content.toString());
		System.out.println("------------------");
		// examine contents of the response
		assertTrue("response is non-null-(1)",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null-(2)",responseWrapper.getBuffer().length > 0);
		
		assertThat("response contains word 'error'", content, containsString("<error"));
	
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
	
}
