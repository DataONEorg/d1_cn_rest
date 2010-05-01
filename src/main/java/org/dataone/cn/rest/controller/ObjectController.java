package org.dataone.cn.rest.controller;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.service.cn.CoordinatingNodeCrud;
import org.dataone.service.cn.CoordinatingNodeQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * @author rwaltz
 *
 */

@Controller
public class ObjectController {
	@Autowired
	@Qualifier("crudService")
	CoordinatingNodeCrud crudService;
	@Autowired
	@Qualifier("queryService")
	CoordinatingNodeQuery queryService;

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





