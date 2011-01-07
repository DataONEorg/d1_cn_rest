package org.dataone.cn.web;


import static org.junit.Assert.assertTrue;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.DecoderException;
import org.dataone.cn.rest.filter.RecodePathFilterRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

public class testRecodePathFilterRequest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testURIParsing() throws DecoderException {
	
		
		String startingString = "/metacat/object/aaabbbccc";
		String expect = "aaabbbccc";
		
		ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp",fsrl);
		MockFilterConfig fc = new MockFilterConfig(sc,"PathRecodingFilter");

		MockHttpServletRequest request= new MockHttpServletRequest(fc.getServletContext(), null, startingString);
		RecodePathFilterRequest	recodedPathRequest = new RecodePathFilterRequest(request);
		String recodedPath = recodedPathRequest.getPathInfo();
		System.out.println("Got: " + recodedPath);
		assertTrue("recoded Path is " + expect, recodedPath.equals(expect) );
		
	}
}
