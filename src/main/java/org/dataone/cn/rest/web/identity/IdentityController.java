/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identity;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.net.URLCodec;
import org.dataone.client.auth.CertificateManager;
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
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SubjectList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * The controller for identity manager service
 *
 * @author leinfelder
 */
@Controller("identityController")
public class IdentityController extends AbstractWebController implements ServletContextAware {
    /*
     * hard coded paths that this controller will proxy out.
     * easier to modify in future releases to keep them all at the top
     */
    private static final String ACCOUNT_MAPPING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING;
    private static final String ACCOUNT_MAPPING_PENDING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING_PENDING;
    private static final String ACCOUNTS_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNTS;
    private static final String GROUPS_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS;
    private static final String GROUPS_REMOVE_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS_REMOVE;


    private ServletContext servletContext;

	private URLCodec urlDecoder = new URLCodec();

    @Autowired
    @Qualifier("cnIdentity")
    CNIdentity  cnIdentity;
    public IdentityController() {}
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.GET)
    public ModelAndView listSubjects(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/*", method = RequestMethod.GET)
    public ModelAndView getSubjectInfo(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.GET)
    public ModelAndView getPendingMapIdentity(HttpServletRequest request, HttpServletResponse response) 
    	throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.POST)
    public ModelAndView registerAccount(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(fileRequest);
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
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.PUT)
    public ModelAndView updateAccount(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(fileRequest);
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
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/*", method = RequestMethod.POST)
    public void verifyAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	
    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNTS_PATH_V1 + "/";
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
    
    @RequestMapping(value = ACCOUNT_MAPPING_PATH_V1, method = RequestMethod.POST)
    public void mapIdentity(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(fileRequest);
    	
    	// get params from request
        Subject primarySubject = null;
        MultipartFile primarySubjectPart = fileRequest.getFile("primarySubject");
    	try {
    		primarySubject = TypeMarshaller.unmarshalTypeFromStream(Subject.class, primarySubjectPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create primary Subject from input");
		}
		Subject secondarySubject = null;
        MultipartFile secondarySubjectPart = fileRequest.getFile("secondarySubject");
    	try {
    		secondarySubject = TypeMarshaller.unmarshalTypeFromStream(Subject.class, secondarySubjectPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create secondary Subject from input");
		}
    	
		boolean success = cnIdentity.mapIdentity(session, primarySubject, secondarySubject);

    }
    
    
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.POST)
    public void requestMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    	
		boolean success = cnIdentity.requestMapIdentity(session, subject);

    }
    
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.PUT)
    public void confirmMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    
    @RequestMapping(value = ACCOUNT_MAPPING_PENDING_PATH_V1 + "/*", method = RequestMethod.DELETE)
    public void denyMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
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
    
    @RequestMapping(value = ACCOUNT_MAPPING_PATH_V1 + "/*", method = RequestMethod.DELETE)
    public void removeMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requestUri = request.getRequestURI();
    	String path = ACCOUNT_MAPPING_PATH_V1 + "/";
    	String subjectString = requestUri.substring(requestUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
		boolean success = cnIdentity.removeMapIdentity(session, subject);

    }
    
    @RequestMapping(value = GROUPS_PATH_V1 + "/*", method = RequestMethod.POST)
    public ModelAndView createGroup(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	Subject group = null;
        String requesUri = request.getRequestURI();
    	String path = GROUPS_PATH_V1 + "/";
    	String subjectString = requesUri.substring(requesUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	try {
			group = new Subject();
			group.setValue(subjectString);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
    	
		Subject retGroup = cnIdentity.createGroup(session, group);
		
        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", retGroup);


    }
    
    @RequestMapping(value = GROUPS_PATH_V1 + "/*", method = RequestMethod.PUT)
    public void addGroupMembers(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(fileRequest);
    	// get params from request
        Subject group = null;
        String requesUri = fileRequest.getRequestURI();
    	String path = GROUPS_PATH_V1 + "/";
    	String subjectString = requesUri.substring(requesUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	try {
			group = new Subject();
			group.setValue(subjectString);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
		SubjectList members = null;
    	MultipartFile memberPart = fileRequest.getFile("members");
    	try {
			members = TypeMarshaller.unmarshalTypeFromStream(SubjectList.class, memberPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create SubjectList from members input");
		}
    	
		boolean success = cnIdentity.addGroupMembers(session, group, members);


    }
    
    @RequestMapping(value = GROUPS_REMOVE_PATH_V1 + "/*", method = RequestMethod.POST)
    public void removeGroupMembers(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(fileRequest);
    	// get params from request
    	Subject group = null;
        String requesUri = fileRequest.getRequestURI();
    	String path = GROUPS_REMOVE_PATH_V1 + "/";
    	String subjectString = requesUri.substring(requesUri.lastIndexOf(path) + path.length());
    	try {
			subjectString = urlDecoder.decode(subjectString, "UTF-8");
		} catch (Exception e) {
			// ignore
		}
    	try {
			group = new Subject();
			group.setValue(subjectString);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
		SubjectList members = null;
    	MultipartFile memberPart = fileRequest.getFile("members");
    	try {
			members = TypeMarshaller.unmarshalTypeFromStream(SubjectList.class, memberPart.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create SubjectList from members input");
		}
    	
		boolean success = cnIdentity.removeGroupMembers(session, group, members);

    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
