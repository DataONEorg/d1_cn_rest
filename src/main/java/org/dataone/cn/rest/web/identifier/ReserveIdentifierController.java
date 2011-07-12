/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identifier;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.service.Constants;
import org.dataone.service.cn.ReserveIdentifierService;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.Session;
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
@Controller("reserveIdentifierController")
public class ReserveIdentifierController extends AbstractProxyController implements ServletContextAware {

    private ServletContext servletContext;
    
//    @Autowired
//    @Qualifier("reserveIdentifierService")
//    ReserveIdentifierService  reserveIdentifierService;
    public ReserveIdentifierController() {}
    
    @RequestMapping(value = "/" + Constants.RESOURCE_RESERVE, method = RequestMethod.POST)
    public ModelAndView reserveIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

    	// get the Session object from certificate in request
    	Session session = CertificateManager.getInstance().getSession(request);
    	// get params from request
        Identifier pid = null;
    	String pidString = request.getParameter("pid");
    	try {
			pid = TypeMarshaller.unmarshalTypeFromStream(Identifier.class, new ByteArrayInputStream(pidString.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceFailure(null, "Could not create pid from input");
		}
		String format = request.getParameter("format");
		String scope = request.getParameter("scope");
		
		pid = ReserveIdentifierService.getInstance().reserveIdentifier(session, pid, scope, format);

        return new ModelAndView("xmlIdentifierViewResolver", "org.dataone.service.types.Identifier", pid);

    }
    
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
