package org.dataone.cn.rest.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.service.cn.CoordinatingNodeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller; //import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.dataone.service.types.AuthToken;
import org.dataone.service.types.LogRecordSet;
/**
 * @author rwaltz
 * 
 */

@Controller
public class LogController {


	@Autowired
	@Qualifier("queryService")
	CoordinatingNodeQuery queryService;
	
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
                AuthToken token = new AuthToken("Hello!");
		Date fromDate = new Date();
		Date toDate = new Date();
		LogRecordSet logRecordSet = queryService.getLogRecords(token,
				fromDate, toDate);
                
/*		OutputStream outputStream = response.getOutputStream();

		try {
			for (InputStream inputStream : inputStreams) {
				ControllerUtilities.writeByteOutput(inputStream, outputStream);
			}
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
		return; */
	}



}
