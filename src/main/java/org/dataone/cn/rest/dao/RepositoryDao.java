package org.dataone.cn.rest.dao;


/**
 * Currently this is only an idea of how to interface with lower level repository access
 * @author rwaltz
 *
 */
public interface RepositoryDao {
	public void create(String objectId, Object object) throws Exception;
	public void read(Object token, String guid) throws Exception;
	public void update(String ObjectId, Object object) throws Exception;
	public void delete(String objectId) throws Exception;
}
