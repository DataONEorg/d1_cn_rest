/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.identifier;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.rest.filter.BufferedHttpResponseWrapper;
import org.dataone.cn.rest.proxy.service.ProxyCNReadService;
import org.dataone.cn.rest.proxy.util.AcceptType;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.service.cn.impl.v1.ReserveIdentifierService;
import org.dataone.service.exceptions.BaseException;
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
import org.dataone.service.util.Constants;
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
public class ReserveIdentifierController extends AbstractWebController implements ServletContextAware {
    public static Log log = LogFactory.getLog(ReserveIdentifierController.class);

    private static final String RESOURCE_RESERVE_PATH_V1 = "/v1/" + Constants.RESOURCE_RESERVE;
    private static final String RESOURCE_GENERATE_PATH_V1 = "/v1/" + Constants.RESOURCE_GENERATE;

    private ServletContext servletContext;
    @Autowired
    @Qualifier("reserveIdentifierService")
    ReserveIdentifierService reserveIdentifierService;
    @Autowired
    @Qualifier("proxyCNReadService")
    ProxyCNReadService proxyCNReadService;

    private URLCodec urlCodec = new URLCodec();
    
    public ReserveIdentifierController() {
    }
    
   /*
     * Given an optional scope and format, reserves and
     * returns an identifier within that scope and format
     * that is unique and will not be used by any other sessions.
     * Future calls to MN_storage.create() and MN_storage.update() that
     * reference this ID must originate from the session in which the
     * identifier was reserved, otherwise an error is raised on those methods.
     *
     * return the identifier that was reserved
     * default serialized as XML because there was not an accept header
     *
     * @author leinfelder
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String acceptType
     * @return void
     * @exception
     */
    @RequestMapping(value = {RESOURCE_RESERVE_PATH_V1, RESOURCE_RESERVE_PATH_V1 + "/" }, method = RequestMethod.POST)
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
            if (metaResponse.isException()) {
                BaseException d1Exception = metaResponse.getD1Exception();
                TreeMap<String, String> trace_information = new TreeMap<String, String>();
                for (String key : d1Exception.getTraceKeySet()) {
                    trace_information.put(key, d1Exception.getTraceDetail(key));
                }
                if (d1Exception.getDetail_code().equalsIgnoreCase("1050")) {
                    throw new InvalidToken("4190", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1041")) {
                    throw new NotImplemented("4191", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1090")) {
                    throw new ServiceFailure("4210", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1040")) {
                    throw new NotAuthorized("4180", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1060")) {
                    throw new NotFound("0", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1800")) {
                    throw new NotFound("0", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1080")) {
                    throw new InvalidRequest("4200", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else {
                    throw new ServiceFailure("4210", "Unrecognized getSystemMetadata failure: " + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                }
            }
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
            response.setStatus(response.SC_OK);
            
            // Identifier is not in use, continue
        }

        // place the reservation
        pid = reserveIdentifierService.reserveIdentifier(session, pid);
        if (pid == null) {
            throw new ServiceFailure("4210", "ReserveIdentifierService returned null value for Identifier ");
        }
        return new ModelAndView("xmlIdentifierViewResolver", "org.dataone.service.types.v1.Identifier", pid);

    }
    
    /**
     * Generate a unique identifier that complies with the given identifier scheme,
     * and then reserve the identifier for use only by the Subject of the current session.
     * Future calls to MN_storage.create() and MN_storage.update() that
     * reference this ID must originate from the session in which the
     * identifier was reserved, otherwise an error is raised on those methods.
     * 
     * @param request the Servlet request containing parameters
     * @param response the Servlet response to be returned to clients
     * @return an identifier that is unique and will not be used by any other sessions
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidCredentials
     * @throws InvalidRequest when the scheme is not recognized, or missing
     */
    @RequestMapping(value = {RESOURCE_GENERATE_PATH_V1, RESOURCE_GENERATE_PATH_V1 + "/" }, method = RequestMethod.POST)
    public ModelAndView generateIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

        // get the Session object from certificate in request
        Session session = CertificateManager.getInstance().getSession(request);
        
        // get params from request
        String scheme = request.getParameter("scheme");
        if (scheme != null) {
            try {
                scheme = EncodingUtilities.decodeString(request.getParameter("scheme"));
            } catch (UnsupportedEncodingException ex) {
               throw new InvalidRequest("4200", "Request missing 'scheme' parameter: " + ex.getMessage());
            }
        } else {
            throw new InvalidRequest("4200", "Request missing 'scheme' parameter" );
        }
        log.info(scheme);
        String fragment =request.getParameter("fragment");
        if (fragment != null) {
            try {
                fragment = EncodingUtilities.decodeString(request.getParameter("fragment"));

            } catch (UnsupportedEncodingException ex) {
                // fragment is optional, so set it to null if it isn't included in the request
                fragment = null;
            }
        }

        // Generate the identifier, and reserve it
        Identifier pid = reserveIdentifierService.generateIdentifier(session, scheme, fragment);
        if (pid == null) {
            throw new ServiceFailure("4210", "ReserveIdentifierService returned null value for Identifier for generateIdentifier()");
        }
        return new ModelAndView("xmlIdentifierViewResolver", "org.dataone.service.types.v1.Identifier", pid);
    }
    
   /*
     * Checks to determine if the caller (as determined by session)
     * has the reservation (i.e. is the owner) of the specified PID.
     *
     * return 200 if so, otherwise raise an exception
     *
     * @author leinfelder
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String acceptType
     * @return void
     * @exception
     */
    @RequestMapping(value = RESOURCE_RESERVE_PATH_V1 + "/**", method = RequestMethod.GET)
    public void hasReservation(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest, NotFound {

        // get the Session object from certificate in request
        Session session = CertificateManager.getInstance().getSession(request);
        // get params from request
        Identifier pid = new Identifier();
        try {
            String pidString = getRequestPID(request, RESOURCE_RESERVE_PATH_V1);
            pid.setValue(pidString);
        } catch (DecoderException ex) {
            throw new ServiceFailure("4872", "Problem reading pid , " + ex.getMessage());
        }


        // check the reservation
        boolean hasReservation = reserveIdentifierService.hasReservation(session, pid);

        // look for existing use of the ID if there is no reservation
        if (!hasReservation) {
            // look for existing use of the Identifier
            BufferedHttpResponseWrapper metaResponse = new BufferedHttpResponseWrapper(response);
            proxyCNReadService.getSystemMetadata(servletContext, request, metaResponse, pid.getValue(), AcceptType.XML);
            if (metaResponse.isException()) {
                BaseException d1Exception = metaResponse.getD1Exception();
                TreeMap<String, String> trace_information = new TreeMap<String, String>();
                for (String key : d1Exception.getTraceKeySet()) {
                    trace_information.put(key, d1Exception.getTraceDetail(key));
                }
                if (d1Exception.getDetail_code().equalsIgnoreCase("1050")) {
                    throw new InvalidToken("4875", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1041")) {
                    throw new NotImplemented("4870", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1090")) {
                    throw new ServiceFailure("4872", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1040")) {
                    throw new NotAuthorized("4871", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1060")) {
                    throw new NotFound("4874", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1800")) {
                    throw new NotFound("4874", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else if (d1Exception.getDetail_code().equalsIgnoreCase("1080")) {
                    throw new InvalidRequest("4873", "getSystemMetadata failed:" + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                } else {
                    throw new ServiceFailure("4872", "Unrecognized getSystemMetadata failure: " + d1Exception.getDescription(), d1Exception.getPid(), trace_information);
                }
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(metaResponse.getBuffer());

            SystemMetadata systemMetadata = null;
            try {
                systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, inputStream);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("4872", "Problem deserializing system metadata, " + e.getMessage());
            }
            // is there system meta data for the Identifier?
            if (systemMetadata != null) {
                throw new IdentifierNotUnique("4876", "The given pid is already in use: " + pid.getValue());
            }
        }

        // if we got here, we have the reservation

    }
    /**
     * pull the pid from an url
     *  it maybe that the PID is url encoded or it may appear as a path
     *  use the path as a way to delimit the pid
     *
     * @author rwaltz
     * @param HttpServletRequest request
     * @param String delimiter
     * @return PID
     * @exception DecoderException
     */
    private String getRequestPID(HttpServletRequest request, String delimiter) throws DecoderException {
        StringBuffer urlBuffer = request.getRequestURL();
        int objectStart = urlBuffer.indexOf(delimiter);
        String pid = urlBuffer.substring(objectStart + delimiter.length(), urlBuffer.length());
        String decodedPID = null;
        try {
            decodedPID = EncodingUtilities.decodeString(pid);
        } catch (UnsupportedEncodingException e) {
            decodedPID = urlCodec.decode(pid);
        }
        log.info("decoding PID/PID: " + pid + " to " + decodedPID);
        return decodedPID;
    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
