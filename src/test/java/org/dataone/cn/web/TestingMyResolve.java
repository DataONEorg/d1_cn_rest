package org.dataone.cn.web;

import static org.junit.Assert.*;
import FilterChain;
import FilterConfig;
import HttpServletRequest;
import HttpServletResponse;
import MockFilterChain;
import MockFilterConfig;
import MockHttpServletRequest;
import MockHttpServletResponse;
import ResolveFilter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import junit.framework.TestCase;
import org.dataone.cn.rest.filter.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.mock.*;


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
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testDestroy() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testDoFilter() {
		fail("Not yet implemented"); // TODO
	}

	public void testXmlTransformation() {
		//building up a new ResolveFilter with the appropriate parameters
//		ServletContext sc = new MockServletContext();
//		sc.addInitParameter("text/xml","/WEB-INF/config/resolve-filter-xml.xsl");
//		FilterConfig fc = new MockFilterConfig(sc,"ResolveFilter");

		FilterConfig fc = new MockFilterConfig("ResolveFilter");
		fc.addInitParameter("text/xml","/WEB-INF/config/resolve-filter-xml.xsl");
		ResolveFilter rf = new ResolveFilter();	
		
		rf.init(fc);
		
		HttpServletRequest request= new MockHttpServletRequest(
				fc.getServletContext(),
				null,
				"/resolve/12345");
		
		request.addHeader("accept", (Object) "text/xml");
		
		HttpServletResponse response = new MockHttpServletResponse();
		
		FilterChain chain = new MockFilterChain();
		
		
		rf.doFilter(request,response,filterChain);

		assertEquals("two plus two", "five");
	}

	
}
