/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identity;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.client.auth.CertificateManager;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
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
import org.dataone.service.types.v1.SubjectList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

/**
 * The controller for identity manager service
 *
 * @author leinfelder
 */
@Controller("identityController")
public class IdentityController extends AbstractProxyController implements ServletContextAware {
    /*
     * hard coded paths that this controller will proxy out.
     * easier to modify in future releases to keep them all at the top
     */
    private static final String ACCOUNTS_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNTS;
    private static final String GROUPS_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS;

    private ServletContext servletContext;

    
    @Autowired
    @Qualifier("cnIdentity")
    CNIdentity  cnIdentity;
    public IdentityController() {}
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.GET)
    public ModelAndView listSubjects(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	String query = request.getParameter("query");
    	int start = 0;
    	int count = -1;
    	try {
    		start = Integer.parseInt(request.getParameter("start"));
    	} catch (Exception e) {}
    	try {
    		count = Integer.parseInt(request.getParameter("count"));
    	} catch (Exception e) {}
    	
        SubjectList subjectList = cnIdentity.listSubjects(session, query, start, count);

        return new ModelAndView("xmlSubjectListViewResolver", "org.dataone.service.types.v1.SubjectList", subjectList);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/*", method = RequestMethod.GET)
    public ModelAndView getSubjectInfo(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String subjectString = requesUri.substring(requesUri.lastIndexOf("/") + 1);
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
        SubjectList subjectList = cnIdentity.getSubjectInfo(session, subject);

        return new ModelAndView("xmlSubjectListViewResolver", "org.dataone.service.types.v1.SubjectList", subjectList);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.POST)
    public ModelAndView registerAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Person person = null;
    	String personString = request.getParameter("person");
    	try {
			person = TypeMarshaller.unmarshalTypeFromStream(Person.class, new ByteArrayInputStream(personString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Person from input");
		}
    	
		Subject subject = cnIdentity.registerAccount(session, person);

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1, method = RequestMethod.PUT)
    public ModelAndView updateAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Person person = null;
    	String personString = request.getParameter("person");
    	try {
			person = TypeMarshaller.unmarshalTypeFromStream(Person.class, new ByteArrayInputStream(personString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Person from input");
		}
    	
		Subject subject = cnIdentity.updateAccount(session, person);

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/*", method = RequestMethod.POST)
    public void verifyAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String subjectString = requesUri.substring(requesUri.lastIndexOf("/") + 1);
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
		boolean success = cnIdentity.verifyAccount(session, subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/map", method = RequestMethod.POST)
    public void mapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Subject subject = null;
    	String subjectString = request.getParameter("subject");
    	try {
			subject = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(subjectString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Subject from input");
		}
    	
		boolean success = cnIdentity.mapIdentity(session, subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH_V1 + "/confirm", method = RequestMethod.POST)
    public void confirmMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Subject subject = null;
    	String subjectString = request.getParameter("subject");
    	try {
			subject = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(subjectString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Subject from input");
		}
    	
		boolean success = cnIdentity.confirmMapIdentity(session, subject);

    }
    
    @RequestMapping(value = GROUPS_PATH_V1, method = RequestMethod.POST)
    public ModelAndView createGroup(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Subject group = null;
    	String groupString = request.getParameter("group");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(groupString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
    	
		Subject retGroup = cnIdentity.createGroup(session, group);
		
        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.v1.Subject", retGroup);


    }
    
    @RequestMapping(value = GROUPS_PATH_V1, method = RequestMethod.PUT)
    public void addGroupMembers(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Subject group = null;
    	String groupString = request.getParameter("groupName");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(groupString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
		SubjectList members = null;
    	String memberString = request.getParameter("members");
    	try {
			members = TypeMarshaller.unmarshalTypeFromStream(SubjectList.class, new ByteArrayInputStream(memberString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create SubjectList from members input");
		}
    	
		boolean success = cnIdentity.addGroupMembers(session, group, members);


    }
    
    @RequestMapping(value = GROUPS_PATH_V1, method = RequestMethod.DELETE)
    public void removeGroupMembers(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Subject group = null;
    	String groupString = request.getParameter("groupName");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(groupString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
		SubjectList members = null;
    	String memberString = request.getParameter("members");
    	try {
			members = TypeMarshaller.unmarshalTypeFromStream(SubjectList.class, new ByteArrayInputStream(memberString.getBytes("UTF-8")));
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
