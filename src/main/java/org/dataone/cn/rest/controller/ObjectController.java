package org.dataone.cn.rest.controller;


import org.dataone.cn.rest.service.CrudService;
import org.dataone.cn.rest.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import org.dataone.ns.core.objects.Response;

/**
 * @author rwaltz
 *
 */

@Controller
public class ObjectController {
	@Autowired
	CrudService crudService;
	QueryService queryService;
	
	@RequestMapping(value = "/object/" , method = RequestMethod.GET)
		public ModelAndView getSearch() throws Exception {
		Object token = new Object();
		Object search = new Object();
		Response response = queryService.search(token, search);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}
	
	@RequestMapping(value = "/object/{guid}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable String guid) throws Exception {
		Object token = new Object();
		Response response = crudService.get(token, guid);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}
	
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
	}	

	
	
}





