package org.dataone.cn.rest.controller;


import java.io.OutputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.cn.rest.service.HealthService;
import org.dataone.cn.rest.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class LogController {

	@Autowired
	@Qualifier("healthService")
	HealthService healthService;
	@Autowired
	@Qualifier("queryService")
	QueryService queryService;
	
	@RequestMapping(value ="/log", method = RequestMethod.GET)
		public void getSearch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		OutputStream output = response.getOutputStream();
		output.write("testing logs\n".getBytes());
		output.flush();
		output.close();
		return;
	}
	
	@RequestMapping(value = "/log/report", method = RequestMethod.GET)
	public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
		OutputStream output = response.getOutputStream();
		output.write("testing report\n".getBytes());
		output.flush();
		output.close();
		return ;
	}

}





