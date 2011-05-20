/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identity;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.service.cn.tier2.CNIdentity;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
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
    private ServletContext servletContext;

    
    @Autowired
    @Qualifier("cnIdentity")
    CNIdentity  cnIdentity;
    public IdentityController() {}
    
    @RequestMapping(value = ACCOUNTS_PATH, method = RequestMethod.GET)
    public ModelAndView listSubjects(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented {

    	// TODO: get the Session object from?
    	Session session = null;
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
    public ModelAndView getSubjectInfo(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented {

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

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
