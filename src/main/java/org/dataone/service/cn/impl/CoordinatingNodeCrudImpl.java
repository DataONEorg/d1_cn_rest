package org.dataone.service.cn.impl;

import java.io.InputStream;
import java.util.List;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;

import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.IdentifierFormat;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.ObjectLocationList;
import org.dataone.service.types.SystemMetadata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.dataone.service.cn.*;

@Service("crudServiceImpl")
@Qualifier("crudService")
public class CoordinatingNodeCrudImpl implements CoordinatingNodeCrud {


/*	private RepositoryDao repositoryDao;
	
	@Autowired
	@Qualifier("someRepositorysDaoImpl")
	public void setRepositoryDao(RepositoryDao repositoryDao) {
		this.repositoryDao = repositoryDao;
	}*/
	
	/*    
	 * This is a method used internally by the CN, not exposed to MNs or the world. 
	 * Stores the two given objects (systemMetadata, scienceMetadata) in a single atomic action. 
	 * This method is used as part of the synchronization of science metadata between a MN and the CN.
	 * 
	 * @param systemMetadataGUID the GUID of the system metadata
	 * @param systemMetadata The system metadata describing the data package (data and science metadata)
	 * @param scienceMetadataGUID the GUID of the science metadata
	 * @param scienceMetadata the science metadata portion of the data package
	 * @throws Exception
	 */
//	public void create(String systemMetadataGuid,
//			SystemMetadata systemMetadata, String scienceMetadataGuid,
//			Object scienceMetadata) throws Exception {
//		throw new Exception("create Not implemented Yet!");
//	}
	/**
	 * Retrieves the science metadata or system metadata object identified by the given GUID. 
	 * If the object identified by the GUID is a data object, then an error is raised.
	 *     
	 * 
	 * @param token authentication token; ignored for V0.3
	 * @param guid Identifier for the science data or science metedata object of interest.
	 * @return For system metadata objects, the system metadata itself is returned. For science metadata objects, this will be the exact byte stream of the science metadata oject, as is was original ingested.
	 * @throws Exception
	 * @throws NotFound The object specified by GUID does not exist.
	 * @throws ObjectNotHere The object specified by the GUID is a data object and is not present on this (or any) CN. This response could be accompanied by the results of a standard resolve() method call to provide more information about the object.
	 */
        @Override
	public InputStream get(AuthToken token, Identifier guid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
        NotImplemented {
                ServiceFailure notImplemented = new ServiceFailure("1111", "get method Not Implemented yet");

		// TODO Auto-generated method stub
		// repositoryDao.read(token, guid);
		throw notImplemented;
	}
	
	/**
	 * Describes the science metadata or data object identified by the GUID by 
	 * returning the system metadata object associated with the GUID.
	 *
	 * @param token authentication token; ignored for V0.3
	 * @param guid Identifier for the science data or science metedata object of interest.
	 * @return System metadata object describing the object.
	 * @throws NotFound There is no data or science metadata identified by the given GUID.
	 * @throws InvalidArgument The GUID requested identifies a system metadata object.
	 * @throws Exception
	 */
    @Override
    public SystemMetadata getSystemMetadata(AuthToken token, Identifier guid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
        InvalidRequest, NotImplemented {
        NotImplemented  notImplemented = new NotImplemented("1111", "getSystemMetadata method Not implemented Yet!");
		throw notImplemented;
	}
	
	/**
	 * Resolves the locations of where the science data object identified by the GUID can be found.
	 * 
	 * 
	 * @param token
	 * @param guid
	 * @return A structured list of information about the member nodes that hold the data
	 * @throws Exception
	 * 
	 * <p>Note: It seems that returning just a simple list of URLs for the service endpoints of the relevant MNs 
	 * is not sufficient. Some sort of MN identifier also needs to be returned so that the caller can ask further 
	 * information about MN (which would include the endpoint URL for the MN API implementation). Is there a use 
	 * case that involves asking a CN for metadata describing a MN?</p>
	 */


    @Override
    public boolean assertRelation(AuthToken token, Identifier subjectId,
        String relationship, Identifier objectId)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
        InvalidRequest, NotImplemented{
		// TODO Auto-generated method stub
                NotImplemented  notImplemented = new NotImplemented("1111", "assertRelation method Not implemented Yet!");
		throw notImplemented;
	}

    @Override
    public Identifier create(AuthToken token, Identifier guid, InputStream object, SystemMetadata sysmeta) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier reserveIdentifier(AuthToken token, String scope, IdentifierFormat format) throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier reserveIdentifier(AuthToken token, String scope) throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier reserveIdentifier(AuthToken token, IdentifierFormat format) throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier reserveIdentifier(AuthToken token) throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectLocationList resolve(AuthToken token, Identifier guid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest, NotImplemented {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
