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
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.service.cn.CNIdentity;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.Person;
import org.dataone.service.types.Session;
import org.dataone.service.types.Subject;
import org.dataone.service.types.SubjectList;
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

    private static final String ACCOUNTS_PATH = "/accounts";
    private static final String GROUPS_PATH = "/groups";

    private ServletContext servletContext;

    
    @Autowired
    @Qualifier("cnIdentity")
    CNIdentity  cnIdentity;
    public IdentityController() {}
    
    @RequestMapping(value = ACCOUNTS_PATH, method = RequestMethod.GET)
    public ModelAndView listSubjects(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented {

    	// TODO: get the Session object from?
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

        return new ModelAndView("xmlSubjectListViewResolver", "org.dataone.service.types.SubjectList", subjectList);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH + "/*", method = RequestMethod.GET)
    public ModelAndView getSubjectInfo(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

    	// TODO: get the Session object from?
    	Session session = null;
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String subjectString = requesUri.substring(requesUri.lastIndexOf("/") + 1);
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
        SubjectList subjectList = cnIdentity.getSubjectInfo(session, subject);

        return new ModelAndView("xmlSubjectListViewResolver", "org.dataone.service.types.SubjectList", subjectList);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH, method = RequestMethod.POST)
    public ModelAndView registerAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.Subject", subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH, method = RequestMethod.PUT)
    public ModelAndView updateAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlSubjectViewResolver", "org.dataone.service.types.Subject", subject);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH + "/*", method = RequestMethod.POST)
    public ModelAndView verifyAccount(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// TODO: get the Session object from?
    	Session session = null;
    	// get params from request
    	String requesUri = request.getRequestURI();
    	String subjectString = requesUri.substring(requesUri.lastIndexOf("/") + 1);
    	Subject subject = new Subject();
    	subject.setValue(subjectString);
    	
		boolean success = cnIdentity.verifyAccount(session, subject);

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH + "/map", method = RequestMethod.POST)
    public ModelAndView mapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }
    
    @RequestMapping(value = ACCOUNTS_PATH + "/confirm", method = RequestMethod.POST)
    public ModelAndView confirmMapIdentity(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }
    
    @RequestMapping(value = GROUPS_PATH, method = RequestMethod.POST)
    public ModelAndView createGroup(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// TODO: get the Session object from?
    	Session session = null;
    	// get params from request
        Subject group = null;
    	String groupString = request.getParameter("group");
    	try {
			group = TypeMarshaller.unmarshalTypeFromStream(Subject.class, new ByteArrayInputStream(groupString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create Group from input");
		}
    	
		boolean success = cnIdentity.createGroup(session, group);

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }
    
    @RequestMapping(value = GROUPS_PATH, method = RequestMethod.PUT)
    public ModelAndView addGroupMembers(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }
    
    @RequestMapping(value = GROUPS_PATH, method = RequestMethod.DELETE)
    public ModelAndView removeGroupMembers(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

    	// TODO: get the Session object from?
    	Session session = null;
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

        return new ModelAndView("xmlBooleanViewResolver", "java.lang.Boolean", success);

    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
