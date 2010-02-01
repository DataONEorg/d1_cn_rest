package org.dataone.cn.rest.dao.impl;

import org.dataone.cn.rest.dao.RepositoryDao;
import org.dataone.cn.rest.dao.SearchDao;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service("metacatDaoImpl")
public class MetacatDaoImpl implements RepositoryDao, SearchDao {

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

	@Override
	public String read(String objectId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String update(String ObjectId, Document object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String query() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
