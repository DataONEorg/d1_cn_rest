package org.dataone.cn.rest.service;

import org.dataone.ns.core.objects.Response;
import org.dataone.ns.core.objects.SystemMetadata;


public interface CrudService {
	public Response get(Object token, String guid) throws Exception;
	public Response getSystemMetadata(Object token, String guid) throws Exception;
	public Response resolve(Object token, String guid) throws Exception;
	public void create(String systemMetadataGuid, SystemMetadata systemMetadata, String scienceMetadataGuid, Object scienceMetadata) throws Exception;
}
