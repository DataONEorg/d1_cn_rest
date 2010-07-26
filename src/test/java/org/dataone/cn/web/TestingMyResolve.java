package org.dataone.cn.web;

import static org.junit.Assert.*;

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
		HttpServletResponse response = new MockHttpServletResponse();		
		FilterChain chain = new MockFilterChain();
		
		try {
			rf.doFilter(request,response,chain);
		} catch (ServletException se) {
			fail("servlet exception at ResolveFilter.doFilter(): " + se);
		} catch (IOException ioe) {
			fail("servlet exception at ResolveFilter.doFilter(): " + ioe);
		}
		
//		catch (IOException ioe) {
//			throw new IOException(ioe);
//		}	
		assertEquals("two plus two", "five");
	}

	
}
