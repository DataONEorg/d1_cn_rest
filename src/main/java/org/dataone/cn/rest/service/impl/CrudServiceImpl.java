package org.dataone.cn.rest.service.impl;

import org.dataone.cn.rest.dao.RepositoryDao;
import org.dataone.cn.rest.service.CrudService;
import org.dataone.ns.core.objects.Response;
import org.dataone.ns.core.objects.SystemMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("crudServiceImpl")
public class CrudServiceImpl implements CrudService {

	private RepositoryDao repositoryDao;
	
	@Autowired
	@Qualifier("metacat")
	public void setRepositoryDao(RepositoryDao repositoryDao) {
		this.repositoryDao = repositoryDao;
	}
	
	public void create(String systemMetadataGuid,
			SystemMetadata systemMetadata, String scienceMetadataGuid,
			Object scienceMetadata) throws Exception {
		// TODO Auto-generated method stub

	}

	public Response get(Object token, String guid) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public Response getSystemMetadata(Object token, String guid)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public Response resolve(Object token, String guid) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
