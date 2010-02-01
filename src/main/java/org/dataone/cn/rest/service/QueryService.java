package org.dataone.cn.rest.service;

import java.util.Date;

import org.dataone.ns.core.objects.Response;

/**
 * @author rwaltz
 *
 */
public interface QueryService {
	
	/** generates a response to be used with servlet. Displays logging information, initially
	 *  from apache logs (i think) and later from our logging service
	 *  
	 * @param token
	 * @param fromDate
	 * @param toDate
	 * @return
	 * @throws Exception
	 */
	public Response getLogRecords(Object token, Date fromDate, Date toDate) throws Exception; // -> logRecords

	/** returns a list of search results. format yet to be determined. but the query may come in
	 * three different forms, thus making this an interesting interface. Should I have several dao search
	 * interfaces that are injected into the Service layer? need three different searches based on query type..
	 * i'll have to think about this...
	 * 
	 * @param token
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public Response search(Object token, Object query) throws Exception; // -> list of GUIDs
}
