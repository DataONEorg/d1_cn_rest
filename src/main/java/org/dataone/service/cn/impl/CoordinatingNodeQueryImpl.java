package org.dataone.service.cn.impl;

import java.util.Date;

import org.dataone.service.cn.CoordinatingNodeQuery;
import javax.management.Query;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.ObjectList;
import org.dataone.service.types.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
@Service("queryServiceImpl")
@Qualifier("queryService")
public class CoordinatingNodeQueryImpl implements CoordinatingNodeQuery {

	/** generates a response to be used with servlet. Displays logging information, initially
	 *  from apache logs (i think) and later from our logging service
	 *  
	 * @param token
	 * @param fromDate specified by appropriate standard (ISO? W3C?)
	 * @param toDate specified by appropriate standard (ISO? W3C?)
	 * @return Object the set of log records specified; format is not yet specified; the set of records may be empty
	 * @throws Exception
	 */
	@Override
    public Log getLogRecords(AuthToken token,
            Date fromDate, Date toDate)
        throws NotAuthorized, InvalidRequest {
		// TODO Auto-generated method stub

		throw new NotAuthorized("1111","getLogRecords Not implemented Yet!");
	}

	/**	
	 * <p>List of objects that are present on the node, ordered with newest first. Additional parameters specify the start index for the list, the number of items to retrieve, and the object class.</p
	 *
	 * @param token
	 * @param query
	 * @param start	(Optional) Zero based index of the first item to retrieve.
	 * @param count:	(Optional) Number of items to retrieve from the list
	 * @param oclass:	(Optional) Restricts response to the specified object class
	 * 
	 * @return Response The expected response when a user agent sends a GET request to a MN exposing the DataONE REST API /object/ collection is an extract from the total list of identifiers that the MN is able to provide access to when the request was received. The format of the response is determined by the Accept: HTTP header provided by the client, and may be one of: application/json (default), text/csv, text/xml, application/rdf+xml. Regardless of the format, the response is always encoded using the UTF-8 character set.
	 * 
	 * <pre>
	 * The structure of the response is:
	 * 
	 *	RESPONSE      = RESPONSE_INFO + RESPONSE_BODY
	 *	RESPONSE_INFO = START + COUNT + TOTAL
	 *	  START       = integer, zero based index of first item
	 *	  COUNT       = integer, number of items in response
	 *	  TOTAL       = integer, total number of items in collection
	 *	RESPONSE_BODY = n*(guid + GUID_INFO)
	 *	  guid        = object identifier, string
	 *	GUID_INFO     = OBJECT_CLASS + HASH + TIME_STAMP + SIZE
	 *	OBJECT_CLASS  = 'data' | 'metadata' | 'system'
	 *	HASH          = SHA1 hash
	 *	TIME_STAMP    = ISO8601 formatted date time, GMT, YYYY-MM-DDTHH:mm:SS.FFFZ
	 *	  YYYY        = Year
	 *	  MM          = Month
	 *	  DD          = Day
	 *	  HH          = Hour
	 *	  mm          = Minute
	 *	  SS          = Second
	 *	  FFF         = Fraction of second
	 *	SIZE = Byte size of object
	 * </pre>
	*/

    @Override
    public ObjectList search(AuthToken token, Query query)
        throws NotAuthorized, InvalidRequest {
		// TODO Auto-generated method stub
		throw new NotAuthorized("1111","getLogRecords Not implemented Yet!");
	}

}
