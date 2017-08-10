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
package org.dataone.cn.rest.v2;

import com.hazelcast.core.HazelcastInstance;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
import org.dataone.cn.rest.AbstractServiceController;
import org.dataone.cn.synchronization.types.SyncObject;
import org.dataone.configuration.Settings;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.cn.impl.v2.ReserveIdentifierService;
import org.dataone.service.cn.v2.NodeRegistryService;
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
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides resolution to REST API calls that are described in DataONE's V2 CNCore API
 * (The only exception being getNodeList, it is found in RegistryController due to 
 * RequestMapping conflicts)
 * 
 *
 * @author waltz
 */
@Controller("coreControllerV2")
public class CoreController extends AbstractServiceController implements ServletContextAware {

    public static Log logger = LogFactory.getLog(CoreController.class);

    private ServletContext servletContext;

    @Autowired
    @Qualifier("hazelcastInstance")
    HazelcastInstance hazelcastInstance;

    @Autowired
    @Qualifier("nodeRegistryServiceV2")
    private NodeRegistryService nodeRegistryService;

    @Autowired
    @Qualifier("reserveIdentifierServiceV2")
    ReserveIdentifierService reserveIdentifierService;

    BlockingQueue<SyncObject> hzSyncObjectQueue = null;
    static final String synchronizationObjectQueueName = Settings.getConfiguration().getString("dataone.hazelcast.synchronizationObjectQueue");

    String nodeIdentifier = Settings.getConfiguration().getString("cn.nodeId");
    private NodeReference nodeReference;
    
    private static final String V2 = "/v2";
    private static final String RESOURCE_MONITOR_PING_V2 = V2 + "/" + Constants.RESOURCE_MONITOR_PING;
    private static final String RESOURCE_LIST_CHECKSUM_ALGORITHM_V2 = V2 + "/" + Constants.RESOURCE_CHECKSUM;
    private static final String RESOURCE_LIST_QUERY_V2 = V2 + "/" + Constants.RESOURCE_QUERY;
    private static final String RESOURCE_SYNCHRONIZE_V2 = V2 + "/" + Constants.RESOURCE_SYNCHRONIZE;
    private static final String RESOURCE_RESERVE_PATH_V2 = V2 + "/" + Constants.RESOURCE_RESERVE;
    private static final String RESOURCE_GENERATE_PATH_V2 = V2 + "/" + Constants.RESOURCE_GENERATE;

    SimpleDateFormat pingDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    /* probably should be set in dataone-cn-rest-service/etc/dataone/cn/dataoneHazelcast.properties */
    private static boolean SYNC_CHECK_BEFORE_ADDING = 
            Settings.getConfiguration().getBoolean("dataone.hazelcast.synchronization.checkBeforeAdding", /* default */ true);
    
    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
        hzSyncObjectQueue = hazelcastInstance.getQueue(synchronizationObjectQueueName);
    }

    
    @RequestMapping(value = {V2, V2 + "/", "/"}, method = RequestMethod.GET)
    public ModelAndView getCapabilities(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound {

        Node node = nodeRegistryService.getNodeCapabilities(nodeReference);

        return new ModelAndView("xmlNodeViewResolverV2", "org.dataone.service.types.v2.Node", node);

    }


    /**
     * Low level “are you alive” operation. A valid ping response is indicated by a HTTP status of 200. 
     * A timestmap indicating the current system time (UTC) on the node MUST be returned in the HTTP Date header.
     * 
     * Any status response other than 200 indicates that the node is offline for DataONE operations.
     * 
     * @param request
     * @param response
     * @throws org.dataone.service.exceptions.ServiceFailure
     * @throws org.dataone.service.exceptions.NotImplemented
     * @throws org.dataone.service.exceptions.NotFound
    */    
    @RequestMapping(value = {RESOURCE_MONITOR_PING_V2, RESOURCE_MONITOR_PING_V2 + "/"}, method = RequestMethod.GET)
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

    /**
     * Returns a list of checksum algorithms that are supported by DataONE.
     * 
     * @param request
     * @param response
     * @return ModelAndView
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws NotFound 
     */    
    @RequestMapping(value = {RESOURCE_LIST_CHECKSUM_ALGORITHM_V2, RESOURCE_LIST_CHECKSUM_ALGORITHM_V2 + "/"}, method = RequestMethod.GET)
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
     * Returns a list of query engines, i.e. supported values for the queryEngine parameter of the 
     * getQueryEngineDescription and query operations.
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

    
    @RequestMapping(value = {RESOURCE_LIST_QUERY_V2, RESOURCE_LIST_QUERY_V2 + "/"}, method = RequestMethod.GET)
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
    
    @RequestMapping(value = {RESOURCE_RESERVE_PATH_V2, RESOURCE_RESERVE_PATH_V2}, method = RequestMethod.POST)
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
    
    @RequestMapping(value = {RESOURCE_GENERATE_PATH_V2, RESOURCE_GENERATE_PATH_V2 + "/"}, method = RequestMethod.POST)
    public ModelAndView generateIdentifier(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidRequest {

        // get the Session object from certificate in request
        Session session = PortalCertificateManager.getInstance().getSession(request);

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
    
    @RequestMapping(value = RESOURCE_RESERVE_PATH_V2 + "/**", method = RequestMethod.GET)
    public void hasReservation(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented, InvalidCredentials, InvalidRequest, NotFound {

        // get the Session object from certificate in request
        Session session = PortalCertificateManager.getInstance().getSession(request);

        // get params from request
        Identifier pid = extractPidFromRequestURI(request, RESOURCE_RESERVE_PATH_V2 + "/");

        Subject subject = extractSubjectFromRequestParam(request);

        // check the reservation
        boolean hasReservation = reserveIdentifierService.hasReservation(session, subject, pid);

        // if we got here, we have the reservation
    }
    /**
     * Indicates to the CN that a new or existing object identified by PID requires synchronization. 
     * Note that this operation is asynchronous, a successful return indicates that the synchronization task was 
     * successfully queued.
     * This method may be called by any Member Node for new content or the authoritative Member Node for updates 
     * to existing content.
     * 
     * The CN will schedule the synchronization task which will then be processed in the same way as content changes 
     * identified through the listObjects polling mechanism.
     * 
     * @param request
     * @param response
     * @throws ServiceFailure
     * @throws NotAuthorized 
     */
    @RequestMapping(value = {RESOURCE_SYNCHRONIZE_V2, RESOURCE_SYNCHRONIZE_V2 + "/"}, method = RequestMethod.POST)
    public void synchronize(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotAuthorized {

        String progress = null;
        long start = 0;
        long t0 = 0;
        if (logger.isDebugEnabled()) {
            start = System.currentTimeMillis();
            t0 = start;
        }
        try {
            // get the NodeID from the subject via xref to the nodeList
            Session session = PortalCertificateManager.getInstance().getSession(request);
            if (session == null) {
                throw new NotAuthorized("4962", "The client session is null!!");
            }
            final Subject clientSubject = session.getSubject();
            
         /////////////  PERFORMANCE TIMING ////////////////
            if (logger.isDebugEnabled()) {
                long time1 = System.currentTimeMillis();
                logger.debug(String.format("in CN.synchronize, got Session in %d millis", time1 = start));
                start = time1;
            }
            
            NodeList nodeList = nodeRegistryService.listNodes();
            List<Node> nodes = nodeList.getNodeList();
            Node firstFoundNode = (Node) CollectionUtils.find(nodes,
                    new Predicate() {
                        public boolean evaluate(Object o) {
                            Node node = (Node) o;
                            return node.getSubjectList().contains(clientSubject);
                        }
                    }
            );
            if (firstFoundNode == null) {
                throw new NotAuthorized("4962", "The client certificate does not find an approved Member Node");
            }
            NodeReference nodeId = firstFoundNode.getIdentifier();
            progress = "(a) found an approved MN: " + nodeId.getValue();
            
         /////////////  PERFORMANCE TIMING ////////////////
            if (logger.isDebugEnabled()) {
                long time1 = System.currentTimeMillis();
                logger.debug(String.format("in CN.synchronize, got registered Node in %d millis", time1 - start));
                start = time1;
                
            }

            // get the pid parameter from the request object directly
            String pidString = request.getParameter("pid");
            Identifier pid = new Identifier();
            pid.setValue(pidString);

            progress = "(b) got pid from request: " + pidString;

            // if this is a synchronized object and the recorded authoritativeMN conflicts, 
            Map<Identifier, SystemMetadata> smm = HazelcastClientFactory.getSystemMetadataMap();
            if (smm == null) {
                throw new ServiceFailure("4691", "Unexpected Error! The CN could not get the SystemMetadata map!");
            }

         /////////////  PERFORMANCE TIMING ////////////////
            progress = "(c) got Hz sysMeta map: " + pidString;
            if (logger.isDebugEnabled()) {
                long time1 = System.currentTimeMillis();
                logger.debug(String.format("in CN.synchronize, got the Hz sysMeta map in %d millis", time1 - start));
                start = time1;
                
            }
            
            SystemMetadata sysmeta = smm.get(pid);
            if (sysmeta != null && !sysmeta.getAuthoritativeMemberNode().equals(nodeId)) {
                String message = String.format(
                        "The requesting MemberNode (%s) is not the Authoritative MN for this object (%s).",
                        nodeId.getValue(), pid.getValue());
                logger.info(message);
                throw new NotAuthorized("4692", message);
            }
            
         /////////////  PERFORMANCE TIMING ////////////////
            progress = "(d) got sysMeta from the CN map...";
            if (logger.isDebugEnabled()) {
                long time1 = System.currentTimeMillis();
                logger.debug(String.format("in CN.synchronize, got the systemMetadata from Hz in %d millis", time1 - start));
                start = time1;
                
            }

            if (hzSyncObjectQueue == null) {
                throw new ServiceFailure("4691", "CN misconfiguration - could not reach hzSyncObjectQueue named "
                        + synchronizationObjectQueueName);
            }
            progress = "(e) got HzSyncObjectQueue: " + synchronizationObjectQueueName;
            // check that the item isn't already in the queue
            SyncObject so = new SyncObject(nodeId.getValue(), pid.getValue());
            
            long split = 0;
            if (!SYNC_CHECK_BEFORE_ADDING || !hzSyncObjectQueue.contains(so)) {
                split = System.currentTimeMillis();
                hzSyncObjectQueue.add(so);
            }
            
         /////////////  PERFORMANCE TIMING ////////////////
            if (logger.isDebugEnabled()) {
                long time1 = System.currentTimeMillis();
           
                if (split == 0) {
                    // it was a dupe, only record time to find it
                    logger.debug(String.format("in CN.synchronize, checked sync queue for duplicate in %d millis", time1 - start));
                } else {
                    logger.debug(String.format("in CN.synchronize, checked sync queue for duplicate in %d millis", split - start));
                    logger.debug(String.format("in CN.synchronize, added to queue in %d millis", time1 - split));
                }
            }
        } catch (ServiceFailure e) {
            e.setDetail_code("4961");
            throw e;
            // TODO: log the error (as ERROR)
        } catch (NotAuthorized e) {
            throw e; // catching and rethrowing to avoid being recast as a service failure 
            // in the following catch Exception block
            // TODO: log the error (as warn)
        } catch (Exception e) {
            String message = "Unexpected Exception in CN.synchronize: progress: "
                    + progress + ":: " + e.toString();
            logger.error(message, e);
            throw new ServiceFailure("4961", message);
        } finally {
            logger.debug(String.format("in CN.synchronize for total time of %d millis", System.currentTimeMillis() - t0));
        }
    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

}
