package org.dataone.cn.web;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import junit.framework.TestCase;
import org.dataone.cn.rest.filter.*;
import javax.servlet.*;
import javax.servlet.http.*;
//import static org.easymock.EasyMock.*;
import org.springframework.mock.*;

public class TestResolveTransformations extends TestCase {

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

	
    public TestResolveTransformations(String name) {
    	super(name);
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
	public void testJsonTransformation() {
		
	}
	public void testCsvTransformation() {
		
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(TestResolveTransformations.class);
	}
}

