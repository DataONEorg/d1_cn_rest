package org.dataone.cn.rest.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.cn.rest.service.HealthService;
import org.dataone.cn.rest.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller; //import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import org.dataone.ns.core.objects.Response;
import org.dataone.cn.rest.util.ControllerUtilities;
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
	
	/** rest url for retrieving log information
	 *  
	 * @param HttpServletRequest 
	 * @param HttpServletResponse 
	 * @return void will stream input streams from queryService.getLogRecords method
	 * @throws Exception
	 */
	@RequestMapping(value = "/log", method = RequestMethod.GET)
	public void getLogRecords(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		OutputStream outputStream = response.getOutputStream();

		Object token = new Object();
		Date fromDate = new Date();
		Date toDate = new Date();
		List<InputStream> inputStreams = queryService.getLogRecords(token,
				fromDate, toDate);
		try {
			for (InputStream inputStream : inputStreams) {
				ControllerUtilities.writeByteOutput(inputStream, outputStream);
			}
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
		return;
	}
	/** rest url for retrieving status information of the member nodes
	 *  
	 * @param HttpServletRequest 
	 * @param HttpServletResponse 
	 * @return ???
	 * @throws Exception
	 */
	@RequestMapping(value = "/log/report", method = RequestMethod.GET)
	public void generateReport(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Object token = new Object();
		OutputStream outputStream = response.getOutputStream();
		InputStream inputStream = healthService.generateReport(token);
		try {
			ControllerUtilities.writeByteOutput(inputStream, outputStream);
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
		return;
	}

}
