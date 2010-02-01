package org.dataone.cn.rest.dao;

import org.w3c.dom.Document;

public interface RepositoryDao {
	public String create(String objectId, Document object) throws Exception;
	public String read(String objectId) throws Exception;
	public String update(String ObjectId, Document object) throws Exception;
	public String delete(String objectId) throws Exception;
}
