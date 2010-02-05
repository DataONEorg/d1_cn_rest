package org.dataone.cn.rest.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.dataone.cn.rest.dao.RepositoryDao;
import org.dataone.cn.rest.service.CrudService;
import org.dataone.ns.core.objects.Response;
import org.dataone.ns.core.objects.SystemMetadata;
import org.dataone.ns.core.objects.Response.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Service("crudServiceImpl")
@Qualifier("crudService")
public class CrudServiceImpl implements CrudService {

	private RepositoryDao repositoryDao;
	
	@Autowired
	@Qualifier("metacatDaoImpl")
	public void setRepositoryDao(RepositoryDao repositoryDao) {
		this.repositoryDao = repositoryDao;
	}

	public void create(String systemMetadataGuid,
			SystemMetadata systemMetadata, String scienceMetadataGuid,
			Object scienceMetadata) throws Exception {
		throw new Exception("create Not implemented Yet!");
	}

	public void get(Object token, String guid, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		repositoryDao.read(token, guid, request, response);
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

	// probably needs to be moved to a statically exposed util class
	/* dont' think i'll need it afterall
	private byte[] getBytes(Object obj) throws java.io.IOException{
	      ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      ObjectOutputStream oos = new ObjectOutputStream(bos);
	      oos.writeObject(obj);
	      oos.flush();
	      oos.close();
	      bos.close();
	      byte [] data = bos.toByteArray();
	      return data;
	  }
	  */
}
