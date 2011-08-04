/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identifier;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.rest.filter.BufferedHttpResponseWrapper;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.cn.rest.proxy.http.ProxyServletResponseWrapper;
import org.dataone.cn.rest.proxy.service.ProxyCNReadService;
import org.dataone.cn.rest.proxy.util.AcceptType;
import org.dataone.service.util.Constants;
import org.dataone.service.cn.impl.v1.ReserveIdentifierService;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.EncodingUtilities;
import org.dataone.service.util.TypeMarshaller;
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
    public static Log log = LogFactory.getLog(ReserveIdentifierController.class);
    private ServletContext servletContext;
    @Autowired
    @Qualifier("reserveIdentifierService")
    ReserveIdentifierService reserveIdentifierService;
    @Autowired
    @Qualifier("proxyCNReadService")
    ProxyCNReadService proxyCNReadService;

    public ReserveIdentifierController() {
    }

    @RequestMapping(value = "/" + Constants.RESOURCE_RESERVE, method = RequestMethod.POST)
    public ModelAndView reserveIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

        // get the Session object from certificate in request
        Session session = CertificateManager.getInstance().getSession(request);
        String pidString ;
        try {
            pidString = EncodingUtilities.decodeString(request.getParameter("pid"));
        } catch (UnsupportedEncodingException ex) {
           throw new InvalidRequest("4200", "PID causes: " + ex.getMessage());
        }
        log.info(pidString);

        // get params from request
        Identifier pid = new Identifier();
        pid.setValue(pidString);

        // look for existing use of the Identifier
        BufferedHttpResponseWrapper metaResponse = new BufferedHttpResponseWrapper(response);
        try {
            proxyCNReadService.getSystemMetadata(servletContext, request, metaResponse, pid.getValue(), AcceptType.XML);
            String bufferedData = new String(metaResponse.getBuffer());
            log.info(bufferedData);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bufferedData.getBytes());
            SystemMetadata systemMetadata = null;
            try {
                systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, inputStream);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("4210", "Problem deserializing system metadata, " + e.getMessage());
            }
            // is there system meta data for the Identifier?
            if (systemMetadata != null) {
                throw new IdentifierNotUnique("4210", "The given pid is already in use: " + pid.getValue());
            }
        } catch (NotFound e) {
            // Identifier is not in use, continue
        }

        // place the reservation
        pid = reserveIdentifierService.reserveIdentifier(session, pid);
        if (pid == null) {
            throw new ServiceFailure("4210", "ReserveIdentifierService returned null value for Identifier ");
        }
        return new ModelAndView("xmlIdentifierViewResolver", "org.dataone.service.types.v1.Identifier", pid);

    }

    @RequestMapping(value = "/" + Constants.RESOURCE_RESERVE, method = RequestMethod.GET)
    public void hasReservation(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

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

        // check the reservation
        boolean hasReservation = reserveIdentifierService.hasReservation(session, pid);

        // look for existing use of the ID if there is no reservation
        if (!hasReservation) {
            // look for existing use of the Identifier
            ProxyServletResponseWrapper metaResponse = new ProxyServletResponseWrapper(response);
            proxyCNReadService.getSystemMetadata(servletContext, request, metaResponse, pid.getValue(), AcceptType.XML);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(metaResponse.getData());
            SystemMetadata systemMetadata = null;
            try {
                systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, inputStream);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("1090", "Problem deserializing system metadata, " + e.getMessage());
            }
            // is there system meta data for the Identifier?
            if (systemMetadata != null) {
                throw new IdentifierNotUnique("4210", "The given pid is already in use: " + pid.getValue());
            }
        }

        // if we got here, we have the reservation

    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
