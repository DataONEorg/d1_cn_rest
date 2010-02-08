package org.dataone.cn.rest.controller;


import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.rest.service.CrudService;
import org.dataone.cn.rest.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

import org.dataone.ns.core.objects.Response;

/**
 * @author rwaltz
 *
 */

@Controller
public class ObjectController implements ServletContextAware {
	@Autowired
	@Qualifier("crudService")
	CrudService crudService;
	@Autowired
	@Qualifier("queryService")
	QueryService queryService;

	private ServletContext servletContext;
	@RequestMapping(value = "/object", method = RequestMethod.GET, headers="accept=*/xml")
		public void getSearch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Object token = new Object();
		Object search = new Object();

//		Response response = queryService.search(token, search);
		crudService.get(token, "junk", request, response);
		return ;
	}

	@RequestMapping(value = "/object/{guid}", method = RequestMethod.GET, headers="accept=*/xml")
	public void get(HttpServletRequest request, HttpServletResponse response, @PathVariable String guid ) throws Exception {
//	public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Object token = new Object();
		servletContext.getServerInfo();
		ServletContext testContext = servletContext.getContext("/TestTest");
		if (testContext != null) {
			RequestDispatcher testDispatcher = testContext.getRequestDispatcher("/test/" + guid);
			if (testDispatcher != null) {
			      testDispatcher.forward(request, response);
			} else {
				throw new Exception("TestTest test not found! " + servletContext.getServerInfo());
			}
		} else {
			throw new Exception("TestTest not found! "+ servletContext.getServerInfo());
		}

//		crudService.get(token, guid);
		
		return ;
	}
	/*
	@RequestMapping(value = "/object/{guid}/meta/", method = RequestMethod.GET)
	public ModelAndView getMeta(@PathVariable String guid) throws Exception {
		Object token = new Object();
		Response response = crudService.getSystemMetadata(token, guid);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}
	
	@RequestMapping(value = "/object/{guid}/locate/", method = RequestMethod.GET)
	public ModelAndView locateMeta(@PathVariable String guid) throws Exception {
		Object token = new Object();
		Response response = crudService.resolve(token, guid);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}	*/

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		
	}

	
	
}





