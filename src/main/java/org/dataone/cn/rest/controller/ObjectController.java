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
public class ObjectController {
	@Autowired
	@Qualifier("crudService")
	CrudService crudService;
	@Autowired
	@Qualifier("queryService")
	QueryService queryService;

	@RequestMapping(value = "/object", method = RequestMethod.GET, headers="accept=*/xml")
	public void search(HttpServletRequest request, HttpServletResponse response) throws Exception {

		throw new Exception("search Not implemented Yet!");

	}

	@RequestMapping(value = "/object/{guid}", method = RequestMethod.GET, headers="accept=*/xml")
	public void get(HttpServletRequest request, HttpServletResponse response, @PathVariable String guid ) throws Exception {
		
		throw new Exception("get Not implemented Yet!");

	}
	
	@RequestMapping(value = "/object/{guid}/meta/", method = RequestMethod.GET)
	public void  getSystemMetadata(@PathVariable String guid) throws Exception {
		throw new Exception("getSystemMetadata Not implemented Yet!");

	}
	
	@RequestMapping(value = "/object/{guid}/locate/", method = RequestMethod.GET)
	public void  resolve(@PathVariable String guid) throws Exception {
		throw new Exception("resolve Not implemented Yet!");

	}
	
	
}





