package org.dataone.cn.rest.service.impl;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.dataone.cn.rest.dao.RepositoryDao;
import org.dataone.cn.rest.service.CrudService;
import org.dataone.ns.core.objects.Response;
import org.dataone.ns.core.objects.SystemMetadata;
import org.dataone.ns.core.objects.Response.Data;
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
		throw new Exception("create Not implemented Yet!");
	}

	public Response get(Object token, String guid) throws Exception {
		// TODO Auto-generated method stub
		
		
		Response response = new Response();
		response.setStart(1);
		response.setCount(1);
		response.setTotal(1);
		ArrayList<Data> datalist = new ArrayList<Data>();
		Data data = new Data();
		data.setGuid("abc123");
		data.setModified(new Date());
		data.setOclass("String");

		// String read = repositoryDao.read(guid);
		String read = new String("Hello World!");
		byte[] readBytes = Base64.encodeBase64(read.getBytes("UTF-8"));
		data.setHash(readBytes);
		data.setSize((long)readBytes.length);
		datalist.add(data);
		response.setDatas(datalist);
		return response;
	}

	public Response getSystemMetadata(Object token, String guid)
			throws Exception {
		// TODO Auto-generated method stub
		throw new Exception("getSystemMetadata Not implemented Yet!");
	}

	public Response resolve(Object token, String guid) throws Exception {
		// TODO Auto-generated method stub
		throw new Exception("resolve Not implemented Yet!");
	}

}
