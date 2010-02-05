package org.dataone.cn.rest.service;

import org.dataone.ns.core.objects.Response;
import org.dataone.ns.core.objects.SystemMetadata;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* pass the request in pass it to the implementation layer, pass it down to where it can be processed */
/* streams will be  filtered via SAX */
/*change the get mehtod to return an output stream, i provide an output stream
 * to which teh respository implentation is written. Object Stream not a writer
 */
public interface CrudService {
	public void get(Object token, String guid,HttpServletRequest request, HttpServletResponse response ) throws Exception;
	public Response getSystemMetadata(Object token, String guid) throws Exception;
	public Response resolve(Object token, String guid) throws Exception;
	/* take out void create(String systemMetadataGuid, SystemMetadata systemMetadata, String scienceMetadataGuid, Object scienceMetadata) throws Exception; */
}
