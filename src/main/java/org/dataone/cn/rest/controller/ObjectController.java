package org.dataone.cn.rest.controller;


import java.util.Date;

import org.dataone.cn.rest.service.HealthService;
import org.dataone.cn.rest.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
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
	HealthService healthService;
	QueryService queryService;
	
	@RequestMapping(value = "/log/" , method = RequestMethod.GET)
		public ModelAndView getSearch() throws Exception {
		Date fromDate = new Date();
		Date toDate = new Date();
		Object token = new Object();
		Response response = queryService.getLogRecords(token, fromDate, toDate);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}
	
	@RequestMapping(value = "/log/report", method = RequestMethod.GET)
	public ModelAndView get() throws Exception {
		Object token = new Object();
		Object response = healthService.generateReport(token);
		return new ModelAndView("response", "org.dataone.ns.core.objects.Response",response);
	}
	
	
}





