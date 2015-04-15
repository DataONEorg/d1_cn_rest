/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.rest.web.identity.v1;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.net.URLCodec;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.service.util.Constants;
import org.dataone.service.cn.v1.CNIdentity;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.apache.log4j.Logger;

/**
 * The controller for identity manager service
 *
 * @author leinfelder
 */
@Controller("identityController")
public class IdentityController extends AbstractWebController implements ServletContextAware {
    Logger logger = Logger.getLogger(IdentityController.class.getName());
    /*
     * hard coded paths that this controller will proxy out.
     * easier to modify in future releases to keep them all at the top
     */
    private static final String ACCOUNT_MAPPING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING;
    private static final String ACCOUNT_MAPPING_PENDING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING_PENDING;
    private static final String ACCOUNT_VERIFICATION_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_VERIFICATION;
    private static final String ACCOUNTS_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNTS;
    private static final String GROUPS_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS;


    private ServletContext servletContext;

	private URLCodec urlDecoder = new URLCodec();

    @Autowired
    @Qualifier("cnIdentityV1")
    CNIdentity  cnIdentity;
    public IdentityController() {}
    /**
     * Create a new mapping between the two identities, asserting that they represent the same subject.
     * 
     * POST /accounts/map 	
     * CNIdentity.mapIdentity(session, subject) -> boolean
     *
     * @author leinfelder
     */
    @RequestMapping(value = {ACCOUNT_MAPPING_PATH_V1, ACCOUNT_MAPPING_PATH_V1 + "/" }, method = RequestMethod.POST)
    public void mapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);

    	// get params from request
        Subject primarySubject = new Subject();
        String primarySubjectString = request.getParameter("primarySubject");
        primarySubject.setValue(primarySubjectString);
        
		Subject secondarySubject = new Subject();
		String secondarySubjectString = request.getParameter("secondarySubject");
        secondarySubject.setValue(secondarySubjectString);

		boolean success = cnIdentity.mapIdentity(session, primarySubject, secondarySubject);

    }
    
    /**
     * Removes a previously asserted identity mapping from the Subject in the Session to the Subject given by the parameter. 
     * The reciprocal mapping entry is also removed.
     * 
     * DELETE /accounts/map/{subject} 	
     * CNIdentity.removeMapIdentity(session, subject) -> boolean
     *
     * @author leinfelder
     */
    @RequestMapping(value = ACCOUNT_MAPPING_PATH_V1 + "/*", method = RequestMethod.DELETE)
    public void removeMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNT_MAPPING_PATH_V1 + "/";
    	String subjectString = requestUri.substring(requestUri.lastIndexOf(path) + path.length());
        logger.info("Removing Identity " + subjectString);
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not determine Subject from path: " + requestUri);
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);

		boolean success = cnIdentity.removeMapIdentity(session, subject);

    }

    /**
     * 
     * GET /accounts/pendingmap/{subject}
     * CNIdentity.getPendingMapIdentity(session, subject) -> subjectInfo
     *
     * @author leinfelder
     */
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.GET)
    public ModelAndView getPendingMapIdentity(HttpServletRequest request, HttpServletResponse response) 
    	throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String path = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/";
    	String subjectString = requesUri.substring(requesUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
    	SubjectInfo subjectInfo = cnIdentity.getPendingMapIdentity(session, subject);

        return new ModelAndView("xmlSubjectInfoViewResolver", "org.dataone.service.types.v1.SubjectInfo", subjectInfo);

    }
    /**
     *
     * Request a new mapping between the authenticated identity in the session and the given identity,
     * asserting that they represent the same subject.
     *
     * Mapping identities is a two-step process wherein a map request is made by a primary Subject and a subsequent
     * (confirmation) map request is made by the secondary Subject. This ensures that mappings are
     * performed only by those that have authority to do so.
     *
     * POST /accounts/pendingmap
     * CNIdentity.requestMapIdentity(session, subject) -> boolean
     *
     * @author leinfelder
     * 
     */
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1, method = RequestMethod.POST)
    public void requestMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String subjectString = request.getParameter("subject");

    	Subject subject = new Subject();
    	subject.setValue(subjectString);
		

		boolean success = cnIdentity.requestMapIdentity(session, subject);

    }

    /**
     *
     * Confirms a previously initiated identity mapping. If subject A asserts that B is the same identity through
     * CNIdentity.requestMapIdentity(), then this method is called by B to confirm that assertion.
     *
     * PUT /accounts/pendingmap/{subject}
     * CNIdentity.confirmMapIdentity(session, subject) -> boolean
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.PUT)
    public void confirmMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/";
    	String subjectString = requestUri.substring(requestUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);

		boolean success = cnIdentity.confirmMapIdentity(session, subject);

    }

    /**
     *
     * Denies a previously initiated identity mapping. If subject A asserts that B is the same identity through
     * CNIdentity.requestMapIdentity(), then this method is called by B to deny that assertion.
     *
     * DELETE /accounts/pendingmap/{subject}
     * CNIdentity.denyMapIdentity(session, subject) -> boolean
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.DELETE)
    public void denyMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/";
    	String subjectString = requestUri.substring(requestUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);

		boolean success = cnIdentity.denyMapIdentity(session, subject);

    }
    /**
     *
     * List the subjects, including users, groups, and systems, that match search criteria.
     *
     * GET /accounts?query={query}[&status={status}&start={start}&count={count}]
     * CNIdentity.listSubjects(session, query, status, start, count) -> Types.SubjectList
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = {ACCOUNTS_PATH_V1, ACCOUNTS_PATH_V1 + "/"}, method = RequestMethod.GET)
    public ModelAndView listSubjects(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String query = request.getParameter("query");
    	String status = request.getParameter("status");
    	int start = 0;
    	int count = -1;
    	try {
    		start = Integer.parseInt(request.getParameter("start"));
    	} catch (Exception e) {}
    	try {
    		count = Integer.parseInt(request.getParameter("count"));
    	} catch (Exception e) {}

    	SubjectInfo subjectInfo = cnIdentity.listSubjects(session, query, status, start, count);

        return new ModelAndView("xmlSubjectInfoViewResolver", "org.dataone.service.types.v1.SubjectInfo", subjectInfo);

    }

    /**
     *
     * Get the information about a Person (their equivalent identities, and the Groups to which they belong)
     * or the Group (including members).
     *
     * GET /accounts/{subject}
     * CNIdentity.getSubjectInfo(session, subject) -> Types.SubjectList
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/*", method = RequestMethod.GET)
    public ModelAndView getSubjectInfo(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String path = ACCOUNTS_PATH_V1 + "/";
    	String subjectString = requesUri.substring(requesUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);

    	SubjectInfo subjectInfo = cnIdentity.getSubjectInfo(session, subject);

        return new ModelAndView("xmlSubjectInfoViewResolver", "org.dataone.service.types.v1.SubjectInfo", subjectInfo);

    }

    /**
     *
     * Create a new subject in the DataONE system.
     *
     * POST /accounts
     * CNIdentity.registerAccount(session, person) -> Types.Subject
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = {ACCOUNTS_PATH_V1, ACCOUNTS_PATH_V1 + "/"}, method = RequestMethod.POST)
    public ModelAndView registerAccount(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(fileRequest);
    	// get params from request
        Person person = null;
    	MultipartFile personPart = fileRequest.getFile("person");
    	try {
			person = TypeMarshaller.unmarshalTypeFromStream(Person.class, personPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Person from input");
		}
    	
		Subject subject = cnIdentity.registerAccount(session, person);

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", subject);

    }
    
    /**
     *
     * Update an existing subject in the DataONE system. The target subject is determined from the X509Certificate provided with the session.
     *
     * PUT /accounts
     * CNIdentity.updateAccount(session, person) -> Types.Subject
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/**", method = RequestMethod.PUT)
    public ModelAndView updateAccount(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(fileRequest);
    	// get params from request
        Person person = null;
    	MultipartFile personPart = fileRequest.getFile("person");
    	try {
			person = TypeMarshaller.unmarshalTypeFromStream(Person.class, personPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Person from input");
		}
    	
		Subject subject = cnIdentity.updateAccount(session, person);

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", subject);

    }

    /**
     *
     * Verify that the Person data associated with this Subject is a true representation of the real world person.
     *
     * This service can only be called by users who have an administrative role for the domain of users in question.
     *
     * POST /accounts/{subject}
     * CNIdentity.verifyAccount(session, subject) -> boolean
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = ACCOUNT_VERIFICATION_PATH_V1 + "/*", method = RequestMethod.PUT)
    public void verifyAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	
    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNT_VERIFICATION_PATH_V1 + "/";
    	String subjectString = requestUri.substring(requestUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
		boolean success = cnIdentity.verifyAccount(session, subject);

    }

    /**
     *
     * Create a group with the given name.
     *
     * Groups are lists of subjects that allow all members of the group to be referenced by listing
     * solely the subject name of the group. Group names must be unique within the DataONE system.
     * Groups can only be modified by Subjects listed as rightsHolders.
     *
     * 	POST /groups
     *  CNIdentity.createGroup(session, group) -> Types.Subject
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = {GROUPS_PATH_V1, GROUPS_PATH_V1 + "/"}, method = RequestMethod.POST)
    public ModelAndView createGroup(MultipartHttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(request);
    	// get params from request
    	Group group = null;
    	MultipartFile groupPart = request.getFile("group");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Group.class, groupPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create SubjectList from members input");
		}
    	
		Subject retGroup = cnIdentity.createGroup(session, group);
		
        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", retGroup);


    }

    /**
     *
     * Add members to the named group.
     *
     * Group members can be added by the original creator of the group, otherwise a NotAuthorized exception is thrown.
     * Group members are provided as a list of subjects to be added to the group.
     *
     * PUT /groups
     * CNIdentity.updateGroup(session, group) -> boolean
     *
     *
     * @author leinfelder
     *
     */
    @RequestMapping(value = {GROUPS_PATH_V1, GROUPS_PATH_V1 + "/"}, method = RequestMethod.PUT)
    public void updateGroup(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = PortalCertificateManager.getInstance().getSession(fileRequest);
    	// get params from request
		Group group = null;
    	MultipartFile groupPart = fileRequest.getFile("group");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Group.class, groupPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from group input");
		}
    	
		boolean success = cnIdentity.updateGroup(session, group);


    }
    

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
