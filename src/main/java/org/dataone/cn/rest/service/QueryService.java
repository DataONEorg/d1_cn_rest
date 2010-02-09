package org.dataone.cn.rest.service;

import java.io.InputStream;
import java.util.Date;

import org.dataone.ns.core.objects.Response;
import java.util.List;
/**
 * @author rwaltz
 *
 */
public interface QueryService {
	
	/** generates a response to be used with servlet. Displays logging information, initially
	 *  from apache logs (i think) and later from our logging service
	 *  
	 * @param token Security token
	 * @param fromDate Start date or reports
	 * @param toDate  End date of reports
	 * @return List<InputStream> list of input streams to write out from the servlet 
	 * @throws Exception
	 */
	public List<InputStream> getLogRecords(Object token, Date fromDate, Date toDate) throws Exception; // -> logRecords

	/** returns a list of search results. format yet to be determined. but the query may come in
	 * three different forms, thus making this an interesting interface. Should I have several dao search
	 * interfaces that are injected into the Service layer? need three different searches based on query type..
	 * i'll have to think about this...
	 * 
	 * @param token Security token
	 * @param query query string or object?
	 * @param start	(Optional) Zero based index of the first item to retrieve.
	 * @param count	(Optional) Number of items to retrieve from the list
	 * @param oclass (Optional) Restricts response to the specified object class
	 * @return
	 * @throws Exception
	 */

	public Response search(Object token, Object query, String... params)
			throws Exception;
}
