/**
 * This work was created by participants in the DataONE project, and is jointly copyrighted by participating
 * institutions in DataONE. For more information on DataONE, see our web site at http://dataone.org.
 *
 * Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * $Id$
 */
package org.dataone.cn.rest.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.net.URLCodec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.AbstractServiceController;
import org.dataone.configuration.Settings;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.cn.impl.v1.ReserveIdentifierService;
import org.dataone.service.cn.v1.NodeRegistryService;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

/**
 * This controller will provide a default xml serialization of the Node that is represented by this CN. This
 * functionality has not yet been defined in an API or documentation but was suggested in July, 2011.
 *
 * The controller also acts as a passthru mechanism for relatively static content that is provided by the
 * node.properties file. Most content of the CN is provided by backend services, such as LDAP or metacat, but some
 * trivial content can be read directly from a properties file and easily exposed as xml
 *
 * @author waltz
 */
@Controller("coreControllerV1")
public class CoreController extends AbstractServiceController implements ServletContextAware {

    public static Log logger = LogFactory.getLog(CoreController.class);

    @Autowired
    @Qualifier("nodeRegistryServiceV1")
    private NodeRegistryService nodeRegistryService;

    @Autowired
    @Qualifier("reserveIdentifierServiceV1")
    ReserveIdentifierService reserveIdentifierService;

    private ServletContext servletContext;

    String nodeIdentifier = Settings.getConfiguration().getString("cn.nodeId");
    protected NodeReference nodeReference;
    private static final String V1 = "/v1";
    private static final String RESOURCE_MONITOR_PING_V1 = V1 + "/" + Constants.RESOURCE_MONITOR_PING;
    private static final String RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 = V1 + "/" + Constants.RESOURCE_CHECKSUM;
    private static final String RESOURCE_LIST_QUERY_V1 = V1 + "/" + Constants.RESOURCE_QUERY;
    private static final String NODELIST_PATH_V1 = V1 + "/" + Constants.RESOURCE_NODE;
    private static final String RESOURCE_RESERVE_PATH_V1 = V1 + "/" + Constants.RESOURCE_RESERVE;
    private static final String RESOURCE_GENERATE_PATH_V1 = V1 + "/" + Constants.RESOURCE_GENERATE;


    private SimpleDateFormat pingDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    protected URLCodec urlCodec = new URLCodec();

    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
    }

    @RequestMapping(value = {V1, V1 + "/"}, method = RequestMethod.GET)
    public ModelAndView getCapabilities(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound {

        Node node = nodeRegistryService.getNodeCapabilities(nodeReference);

        return new ModelAndView("xmlNodeViewResolverV1", "org.dataone.service.types.v1.Node", node);

    }

 

    @RequestMapping(value = {RESOURCE_MONITOR_PING_V1, RESOURCE_MONITOR_PING_V1 + "/"}, method = RequestMethod.GET)
    public void ping(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound {
        OutputStream responseStream = null;
        boolean throwFailure = false;
        String failureMessage = "";
        try {
            Date today = new Date();
            response.addDateHeader("Date", today.getTime());
            response.addIntHeader("Expires", -1);
            response.addHeader("Cache-Control", "private, max-age=0");
            response.addHeader("Content-Type", "text/xml");
            responseStream = response.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
            failureMessage = ex.getMessage();
            throwFailure = true;
        } finally {
            try {
                responseStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                failureMessage = ex.getMessage();
                throwFailure = true;
            }
        }
        if (throwFailure) {
            throw new ServiceFailure("2042", failureMessage);
        }

    }

    @RequestMapping(value = {RESOURCE_LIST_CHECKSUM_ALGORITHM_V1, RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 + "/"}, method = RequestMethod.GET)
    public ModelAndView listChecksumAlgorithms(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound {
        ChecksumAlgorithmList checksumAlgorithmList = new ChecksumAlgorithmList();

        String[] checksums = Settings.getConfiguration().getStringArray("cn.checksumAlgorithmList");

        for (int i = 0; i < checksums.length; i++) {
            logger.info(checksums[i]);
            checksumAlgorithmList.addAlgorithm(checksums[i]);
        }
        return new ModelAndView("xmlChecksumAlgorithmListViewResolverV1", "org.dataone.service.types.v1.ChecksumAlgorithmList", checksumAlgorithmList);

    }
    /*
     * Returns a list of query engines, i.e. supported values for the queryEngine parameter of the getQueryEngineDescription and query operations.
     * 
     * The list of search engines available may be influenced by the authentication status of the request.
     * (If authentication does become a requirement for retrieval of the query engines then a new
     * persistence mechanism will need to be created, and this method will deserve its own 
     * controller with class structure and service implementation to boot)
     * 
     * @author waltz
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @throws NotImplemented
     * @throws ServiceFailure
     * @return ModelAndView
     */

    @RequestMapping(value = {RESOURCE_LIST_QUERY_V1, RESOURCE_LIST_QUERY_V1 + "/"}, method = RequestMethod.GET)
    public ModelAndView listQueryEngines(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized {
        QueryEngineList queryEngineList = new QueryEngineList();

        String[] queryEngines = Settings.getConfiguration().getStringArray("cn.query.engines");

        if ((queryEngines == null) || (queryEngines.length == 0)) {
            throw new NotImplemented("4420", "Query Engine List has not yet been configured");
        }
        for (int i = 0; i < queryEngines.length; i++) {
            logger.debug(queryEngines[i]);
            queryEngineList.addQueryEngine(queryEngines[i]);
        }
        return new ModelAndView("xmlQueryEngineListViewResolverV1", "org.dataone.service.types.v1_1.QueryEngineList", queryEngineList);

    }

    /**
     * Reserves the given identifier
     *
     * @param request
     * @param response
     * @return the identifier that was reserved
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws IdentifierNotUnique
     * @throws InvalidCredentials
     * @throws InvalidRequest
     */
    @RequestMapping(value = {RESOURCE_RESERVE_PATH_V1, RESOURCE_RESERVE_PATH_V1}, method = RequestMethod.POST)
    public ModelAndView reserveIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidCredentials, InvalidRequest {

        // get the Session object from certificate in request
        Session session = PortalCertificateManager.getInstance().getSession(request);

        // get params from request
        Identifier pid = extractPidFromRequestParam(request);

        // place the reservation
        pid = reserveIdentifierService.reserveIdentifier(session, pid);
        if (pid == null) {
            throw new ServiceFailure("4210", "ReserveIdentifierService returned null value for Identifier ");
        }
        return new ModelAndView("xmlIdentifierViewResolverV1", "org.dataone.service.types.v1.Identifier", pid);

    }

    /**
     * Generate a unique identifier that complies with the given identifier scheme, and then reserve the identifier for
     * use only by the Subject of the current session. Future calls to MN_storage.create() and MN_storage.update() that
     * reference this ID must originate from the session in which the identifier was reserved, otherwise an error is
     * raised on those methods.
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
    @RequestMapping(value = {RESOURCE_GENERATE_PATH_V1, RESOURCE_GENERATE_PATH_V1 + "/"}, method = RequestMethod.POST)
    public ModelAndView generateIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

        // get the Session object from certificate in request
        Session session = PortalCertificateManager.getInstance().getSession(request);

        // get params from request
        // get params from request
        String scheme = null;
        try {
            scheme = extractValueFromRequestParam(request, SCHEME_REQUEST_PARAM_KEY);
        } catch (NullPointerException ex) {
            throw new InvalidRequest("4200", ex.getMessage());
        }

        String fragment = null;
        try {
            fragment = extractValueFromRequestParam(request, FRAGMENT_REQUEST_PARAM_KEY);
        } catch (NullPointerException ex) {
            logger.debug("Fragment parameter is optional. ok to ignore");
        }

        // Generate the identifier, and reserve it
        Identifier pid = reserveIdentifierService.generateIdentifier(session, scheme, fragment);
        if (pid == null) {
            throw new ServiceFailure("4210", "ReserveIdentifierService returned null value for Identifier for generateIdentifier()");
        }
        return new ModelAndView("xmlIdentifierViewResolverV1", "org.dataone.service.types.v1.Identifier", pid);
    }

    /**
     * Checks to determine if the subject has the reservation (i.e. is the owner) of the specified PID.
     *
     * @param request
     * @param response
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws IdentifierNotUnique
     * @throws InvalidCredentials
     * @throws InvalidRequest
     * @throws NotFound
     */
    @RequestMapping(value = RESOURCE_RESERVE_PATH_V1 + "/**", method = RequestMethod.GET)
    public void hasReservation(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidCredentials, InvalidRequest, NotFound {

        // get the Session object from certificate in request
        Session session = PortalCertificateManager.getInstance().getSession(request);

        // get params from request
        Identifier pid = extractPidFromRequestURI(request, RESOURCE_RESERVE_PATH_V1 + "/");

        Subject subject = extractSubjectFromRequestParam(request);

        // check the reservation
        boolean hasReservation = reserveIdentifierService.hasReservation(session, subject, pid);

        // if we got here, we have the reservation
    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
