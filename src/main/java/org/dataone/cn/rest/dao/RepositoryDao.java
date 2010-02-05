package org.dataone.cn.rest.dao;

/*import org.w3c.dom.Document; */
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RepositoryDao {
/*	public void create(String objectId, Document object) throws Exception; */
	public void read(Object token, String guid,HttpServletRequest request, HttpServletResponse response) throws Exception;
/*	public void update(String ObjectId, Document object) throws Exception;
	public void delete(String objectId) throws Exception; */
}
