package org.dataone.cn.web;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import java.io.IOException;
//import org.springframework.test.web.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import junit.framework.TestCase;
import org.dataone.cn.rest.filter.*;

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

//	@Test
	public void testInit() {
		fail("Not yet implemented"); // TODO
	}

//	@Test
	public void testDestroy() {
		fail("Not yet implemented"); // TODO
	}

//	@Test
	public void testDoFilter() {
		fail("Not yet implemented"); // TODO
	}
	@Test
	public void testXmlTransformation() {
		//building up a new ResolveFilter with the appropriate parameters

		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
//		sc.addInitParameter("text/xml","src/main/webapp/WEB-INF/config/resolve-filter-xml.xsl");
		MockFilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");
		

		
//		MockFilterConfig fc = new MockFilterConfig("ResolveFilter");
		fc.addInitParameter("text/csv","/WEB-INF/config/resolve-filter-csv.xsl");
		fc.addInitParameter("application/json","/WEB-INF/config/resolve-filter-json.xsl");
		fc.addInitParameter("text/xml","/WEB-INF/config/resolve-filter-xml.xsl");
		ResolveFilter rf = new ResolveFilter();	

		try {
			rf.init(fc);
		} catch (ServletException se) {
			fail("servlet exception at ResolveFilter.init(fc)");
		}
		
		MockHttpServletRequest request= new MockHttpServletRequest(
				fc.getServletContext(),
				null,
				"/resolve/12345");
		
		request.addHeader("accept", (Object) "text/xml");		
		request.setMethod("POST");
		HttpServletResponse response = new MockHttpServletResponse();	
		
		Servlet testResolve = new TestResolveServlet();
		FilterChain chain = new PassThroughFilterChain(testResolve);
		
		// need to wrap the response to examine
		BufferedHttpResponseWrapper responseWrapper =
            new BufferedHttpResponseWrapper((HttpServletResponse) response);
		
		
		try {
			rf.doFilter(request,responseWrapper,chain);
		} catch (ServletException se) {
			fail("servlet exception at ResolveFilter.doFilter(): " + se);
		} catch (IOException ioe) {
			fail("servlet exception at ResolveFilter.doFilter(): " + ioe);
		}
		// examine contents of the response
		assertTrue("response is non-null",responseWrapper.getBufferSize() > 0);
		assertTrue("response is non-null",responseWrapper.getBuffer().length > 0);
		
		String content = new String(responseWrapper.getBuffer());
		
//		JUnitMatchers matcher = new JUnitMatchers();
		assertThat("response contains word 'replica'", content, containsString("objectLocationList"));
//		assertEquals(content,"<?xml version=\"1.0\" encoding=\"UTF-8\"?><locations identifier=\"Identifier0\"><location node=\"ReplicaMemberNode0\" " 
//				+"href=\"http://ReplicaMemberNode0object?id=Identifier0\"/><location node=\"ReplicaMemberNode2\" "
//				+"href=\"http://ReplicaMemberNode2object?id=Identifier0\"/></locations>");
		
	
	}

	
}
