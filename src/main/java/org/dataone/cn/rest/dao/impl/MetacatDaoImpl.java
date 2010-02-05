package org.dataone.cn.rest.dao.impl;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.cn.rest.dao.RepositoryDao;
import org.dataone.cn.rest.dao.SearchDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
//import org.w3c.dom.Document;

@Service("metacatRepositoryDao")
@Qualifier("metacatDaoImpl")
public class MetacatDaoImpl implements RepositoryDao, SearchDao {
/*
	@Override
	public String create(String objectId, Document object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String delete(String objectId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
*/
	@Override
	public void read(Object token, String guid,HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		OutputStream output = response.getOutputStream();
		output.write(guid.getBytes());
		output.flush();
		output.close();
		return;
	}
/*
	@Override
	public String update(String ObjectId, Document object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
*/
	@Override
	public String query() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
