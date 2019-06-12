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

package org.dataone.cn.rest.v2;

// TODO: refactor to split out nodelist logic into separate class - CachedNodeList
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathFactory;

import org.dataone.exceptions.MarshallingException;
import org.apache.log4j.Logger;
import org.dataone.cn.rest.http.BufferedHttpResponseWrapper;
import org.dataone.service.cn.v2.NodeRegistryService;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.util.NodelistUtil;
import org.dataone.service.util.EncodingUtilities;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * ResolveFilter interoperates with UrlRewriteFilter to handle cn/resolve calls.
 * Using a Tomcat / Spring filter chain, UrlRewriteFilter redirects the 
 * cn/resolve call to cn/meta, and ResolveFilter intercepts the response and
 * transforms it into an ojbectLocationList.  To accomplish this, it also 
 * caches baseURL/nodeID pairs obtained from the dataone nodeList, refreshing 
 * the cache periodically, based on a minimum refresh interval, and the nodelist
 * modication date
 * 
 * @param targetEnvironment:          for nodelist selection: prod,staging,test
 * @param nodelistLocation:           URI for where to find the nodelist  (URL or file)
 * @param nodelistSchemaLocation:            a URI for the nodelist schema (URL or file)
 * @param systemmetadataSchemaLocation:      a URI for the sysMD schema     (URL or file)
 * @param objectlocationlistSchemaLocation:  a URI for the objloclist schema (URL or file)
 * @param nodelistRefreshIntervalSeconds:    the nodelist cache will be refreshed
 *      									    after this number of seconds
 * @param useSchemaValidation:        if false, bypass validating data objects
 *                                    against their schemas
 *                                  
 * @author rnahf 
 *
 */
public class ResolveFilter implements Filter {

    Logger logger = Logger.getLogger(ResolveFilter.class);
    private FilterConfig filterConfig = null;
    private Map<String, ArrayList<String>> supportedVersionMap = null;
    private Map<String, String> baseUrlMap = null;
    private XPathFactory xFactory = null;
    private long lastRefreshTimeMS = 0;
    // parameters and their default values  (defaulting for production environment)
    // (see d1_cn_rest/src/main/webapp/WEB-INF/web.xml for std settings of these parameters) 
    private Integer nodelistRefreshIntervalSeconds = 2 * 60;
    private boolean useSchemaValidation = true;
    // static for this deployment of the dataone architecture
    // if you are changing this, you better look at the procedure to 
    // create the objectLocationList
    @Autowired
    @Qualifier("nodeRegistryServiceV2")
    NodeRegistryService nodeListRetrieval;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
        this.supportedVersionMap = null;
        this.xFactory = null;
    }

    /*
     * @see javax.servlet.Filter#destroy(javax.servlet.FilterConfig)
     */
    /**
     *
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("init ResolveFilter");
        this.filterConfig = filterConfig;

        this.xFactory = XPathFactory.newInstance();
    }

    /**
     *
     * @throws ServiceFailure
     */
    private void cacheNodeListURLs(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure {
        NodeList nodeList = null;

        if (isTimeForRefresh()) {
            this.baseUrlMap = null;
        }

        if (this.baseUrlMap == null) {

            try {

                nodeList = nodeListRetrieval.listNodes();

            } catch (ServiceFailure sf) {
                sf.setDetail_code("4150");
                throw sf;
            } catch (NotImplemented ex) {
                throw new ServiceFailure("4150", "Error retrieving Nodelist: Not Implemented" + ": " + ex.getMessage());
            }
            if (nodeList.sizeNodeList() == 0) {
                throw new ServiceFailure("4150", "Error parsing Nodelist: nodeList is Empty!");
            }

            baseUrlMap = NodelistUtil.mapNodeList(nodeList);


            HashMap<String, ArrayList<String>> cacheVersionMap = new HashMap<String, ArrayList<String>>();
            for (Node node : nodeList.getNodeList()) {

                ArrayList<String> supportedVersions = new ArrayList<String>();
                Services services = node.getServices();
                if (node.getState().equals(NodeState.UP) && (services != null)) {
                    for (Service service : services.getServiceList()) {
                        if (service.getName().contains("Read") && service.getAvailable()) {
                            supportedVersions.add(service.getVersion());
                        }
                    }
                    // ??? since we haven't nailed down our version schema yet
                    // like for instance [v1 v2 v3] or [v1.1 v1.2 v2] etc
                    // gonna use default alpha sorting,
                    // TODO when we make a decision, may have to make custom comparator
                    //
                    Collections.sort(supportedVersions);

                    cacheVersionMap.put(node.getIdentifier().getValue(), supportedVersions);
                } else {
                    logger.info("NO SERVICES FOR " + node.getIdentifier().getValue());
                }
            }
            supportedVersionMap = cacheVersionMap;
        }
    }

    /**
     * determines if it is time to refresh the nodelist information cache.  The combination of refresh interval
     * and the modification date on the nodelist file both help to minimize unnecessary refreshes and checks.
     * 
     * @return boolean.  true if time to refresh 
     */
    private Boolean isTimeForRefresh() {
        Date now = new Date();
        long nowMS = now.getTime();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.format(now);

        // convert seconds to milliseconds
        long refreshIntervalMS = this.getNodelistRefreshIntervalSeconds() * 1000L;
        if (nowMS - this.lastRefreshTimeMS > refreshIntervalMS) {
            this.lastRefreshTimeMS = nowMS;
            logger.info("  nodelist refresh: new cached time: " + df.format(now));
            return true;
        } else {
            return false;
        }
    }

    /**
     *  procedure to lookup a BaseURL by the node id.  Meant to be called by
     *  internally, but made public for testing sake, and no apparent reason to restrict it.
     *  
     *  @param String nodeID  - the registered nodeID
     *  @return String theBaseURL mapped to the nodeID
     *  @exception org.dataone.service.exceptions.ServiceFailure
     * 
     **/
    public String lookupVersionedBaseURLbyNode(String nodeID) throws ServiceFailure {
        String baseUrl = baseUrlMap.get(nodeID);
        String versionedBaseUrl = null;
        //determine the highest version for the node
        List<String> versions = supportedVersionMap.get(nodeID);
        if (versions != null && !versions.isEmpty()) {
            String version = versions.get(versions.size() - 1);
            if (baseUrl.endsWith("/")) {
                versionedBaseUrl = baseUrl + version + "/";
            } else {
                versionedBaseUrl = baseUrl + "/" + version + "/";
            }
        }
        return versionedBaseUrl;
    }

    public String lookupBaseURLbyNode(String nodeID) throws ServiceFailure {

        return baseUrlMap.get(nodeID);
    }

    /**
     *  location of the translation logic that transforms systemMetadata to an objectlocationList
     *  or passes through or serializes any error condition that arose at runtime
     *  
     *  For general information on doFilter:
     *  @see javax.servlet.Filter#doFilter(javax.servlet.FilterConfig)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        try {
            doFilterDelegate(req, res, chain);
        } catch (BaseException e) {

            byte[] errorMsgXML = e.serialize(BaseException.FMT_XML).getBytes();

            HttpServletResponse response = (HttpServletResponse) res;
//			response.sendError(e.getCode(), "ResolveFilter: " + e.getMessage());
            response.setStatus(e.getCode());
            response.setContentLength(errorMsgXML.length);
//			response.setContentType(type);
            response.getOutputStream().write(errorMsgXML);
            response.flushBuffer();
        }

    }

    private void doFilterDelegate(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException, ServiceFailure, NotFound, InvalidToken, NotImplemented, NotAuthorized, InvalidRequest {

        // return if init has not been called - it's the setter for filterConfig
        if (filterConfig == null) {
            return;
        }
       
        // compiles without the subtyping to Http versions of request and response.
        // why is the check here?
        if (!(res instanceof HttpServletResponse) || !(req instanceof HttpServletRequest)) {
            throw new ServletException("This filter only supports HTTP");
        }
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        // Resolve will return a HTTP status of 303 (see other) for GET, and 307 (temporary redirect) for HEAD, on success.
        // figure it out before the filter chain, because we need to mask the HEAD as a GET to
        // get a successful /metacat/meta call
        
        
        String requestMethod = request.getMethod().toUpperCase();
        int successStatusCode;
        HttpServletRequest metacatRequest = null;
        if (requestMethod.equals("GET")) {
            successStatusCode = HttpServletResponse.SC_SEE_OTHER;
            metacatRequest = request;
            
        } else if (requestMethod.equals("HEAD")) {
            successStatusCode = HttpServletResponse.SC_TEMPORARY_REDIRECT;
            // metacat will need a GET request to return the systemMetadata
            metacatRequest = new HttpServletRequestWrapper(request) {
                public String getMethod() {
                    return "GET";
                }
            };
        } else {
            throw new ServiceFailure("4150", "error responding to unhandled HTTP Method: " + request.getMethod());
        }
        
        cacheNodeListURLs(request, response);
        //  ****** Handle request before passing control to next filter or servlet  *********

        // we are going to return xml no matter what


        //  ******* pass control to next filter in the chain  ********

        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);

        chain.doFilter(metacatRequest, responseWrapper);

        // we're using tomcat 6.  Is the workaround still necessary?

        // Tomcat 4.0 reuses instances of its HttpServletResponse
        // implementation class in some scenarios. For instance, hitting
        // reload( ) repeatedly on a web browser will cause this to happen.
        // Unfortunately, when this occurs, output is never written to the
        // BufferedHttpResponseWrapper's OutputStream. This means that the
        // XML output array is empty when this happens. The following
        // code is a workaround:

        byte[] origXML = responseWrapper.getBuffer();
//       String forDebug = new String(origXML);
        if (origXML == null || origXML.length == 0) {
            // just let Tomcat deliver its cached data back to the client
            chain.doFilter(req, response);
            return;
        }

        // Examine byte to make certain there is not an error

        if (responseWrapper.isException()) {
            try {
                TreeMap<String, String> trace_information = new TreeMap<String, String>();

                BaseException d1be = responseWrapper.getD1Exception();
                /*
                 * Check for these exceptions from getSystemMetadata and re-raise them as resolve exceptions
                 * Exceptions.InvalidToken (errorCode=401, detailCode=1050)
                 * Exceptions.NotImplemented (errorCode=501, detailCode=1041)
                 * Exceptions.ServiceFailure (errorCode=500, detailCode=1090)
                 * Exceptions.NotAuthorized (errorCode=401, detailCode=1040)
                 * Exceptions.NotFound (errorCode=404, detailCode=1060)
                 */

                for (String key : d1be.getTraceKeySet()) {
                    trace_information.put(key, d1be.getTraceDetail(key));
                }

                String augmentedDescription = String.format("getSystemMetadata failed: %s - %s: %s",
                		d1be.getClass().getSimpleName(),
                		d1be.getDetail_code(),
                		d1be.getDescription());
                
                if (d1be instanceof InvalidToken) {
                    throw new InvalidToken("4130", augmentedDescription, d1be.getPid(), trace_information);
                
                } else if (d1be instanceof NotImplemented) {
                    throw new NotImplemented("4131", augmentedDescription, d1be.getPid(), trace_information);
                
                } else if (d1be instanceof ServiceFailure) {
                    throw new ServiceFailure("4150", augmentedDescription, d1be.getPid(), trace_information);
                
                } else if (d1be instanceof NotAuthorized) {
                    throw new NotAuthorized("4120", augmentedDescription, d1be.getPid(), trace_information);
                
                } else if (d1be instanceof NotFound) {
                    throw new NotFound("4140", augmentedDescription, d1be.getPid(), trace_information);
                
                } else {
                    throw new ServiceFailure("4150", "Unrecognized getSystemMetadata failure: " + 
                    		String.format("%s - %s: %s", 
                    				d1be.getClass().getSimpleName(),
                    				d1be.getDetail_code(),
                    				d1be.getDescription()),
                    		d1be.getPid(), trace_information);
                }
            } catch (IllegalStateException ex) {
                throw new ServiceFailure("4150", "BaseExceptionHandler.deserializeXml: " + ex.getMessage());
            }

        }


        //  ****** Handle response from the servlet  *********
        //  response will be either a systemMD object, or a D1 error object
        //  need to handle both

        // read the incoming sysMD stream
        // and convert from bytearray to xmlSource
        ByteArrayInputStream origXMLIn = new ByteArrayInputStream(origXML);
        SystemMetadata systemMetadata = null;
        try {
            systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, origXMLIn);
        } catch (InstantiationException ex) {
            throw new ServiceFailure("4150", "InstantiationException marshalling SystemMetadata: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new ServiceFailure("4150", "IllegalAccessException marshalling SystemMetadata: " + ex.getMessage());
        } catch (MarshallingException ex) {
            throw new ServiceFailure("4150", "Error parsing /meta output: " + ex.getMessage());
        }


        // ---------------- create DOM doc out of SysMD XML

        // parse the input stream, determining if it's sysMD or error
        Identifier identifier = systemMetadata.getIdentifier();


        ObjectLocationList objectLocationList = createObjectLocationList(identifier.getValue(), systemMetadata.getReplicaList());

        //The HTTP header "Location" MUST be set, and it's value SHOULD be the full get()
        // URL for retrieving the object from the first location in the resolve response
        
        response.setStatus(successStatusCode);
        response.setHeader("Location", objectLocationList.getObjectLocation(0).getUrl());
        try {
            TypeMarshaller.marshalTypeToOutputStream(objectLocationList, response.getOutputStream());
        } catch (MarshallingException ex) {
            throw new ServiceFailure("4150", "error marshalling ObjectLocationList to Response OutputStream: " + ex.getMessage());
        } catch (IOException ex) {
            throw new ServiceFailure("4150", "error marshalling ObjectLocationList to Response OutputStream: " + ex.getMessage());
        }
        response.flushBuffer();
    }

    // TODO: Implement JSON formatted output, based on Accept type in the request
    private ObjectLocationList createObjectLocationList(String idString, List<Replica> nodes) throws ServiceFailure, NotFound {
        ObjectLocationList objectLocationList = new ObjectLocationList();
        Identifier identifier = new Identifier();
        identifier.setValue(idString);
        objectLocationList.setIdentifier(identifier);
        logger.debug("resolving for " + idString);
        if (nodes == null || nodes.isEmpty()) {
            // assuming there should be at least one location to retrieve, so will throw an error
            throw new NotFound("4140", "The requested object is not presently available: " + idString);
        }
        for (Replica replica : nodes) {
            if (replica.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                NodeReference nodeReference = replica.getReplicaMemberNode();
                String baseURLString = lookupBaseURLbyNode(nodeReference.getValue());
                String versionedbaseURL = lookupVersionedBaseURLbyNode(nodeReference.getValue());
                if ((baseURLString == null) || (versionedbaseURL == null)) {
                    continue;
                }
                
                // the id is put into the path portion of the url, so encoding thusly ;)
                String encodedIdString = EncodingUtilities.encodeUrlPathSegment(idString);

                String urlString = versionedbaseURL + "object/" + encodedIdString;

                ObjectLocation objectLocation = new ObjectLocation();
                objectLocation.setNodeIdentifier(nodeReference);
                objectLocation.setBaseURL(baseURLString);
                objectLocation.setUrl(urlString);
                objectLocation.setVersionList(supportedVersionMap.get(nodeReference.getValue()));
                objectLocationList.addObjectLocation(objectLocation);
                // A weighting parameter that provides a hint to the caller
                // for the relative preference for nodes from which the content should be retrieved.
                // XXX How to set the preference???
                // objectLocation.setPreference(filterConfig)

            }

        }
        if (objectLocationList.getObjectLocationList() == null || objectLocationList.getObjectLocationList().isEmpty()) {
            // assuming there should be at least one location to retrieve, so will throw an error
            throw new NotFound("4140", "The requested object is not presently available: " + idString);
        }
        return objectLocationList;
    }

    //  ------   Getters and Setters --------------//
    public NodeRegistryService getNodeListRetrieval() {
        return nodeListRetrieval;
    }

    public void setNodeListRetrieval(NodeRegistryService nodeListRetrieval) {
        this.nodeListRetrieval = nodeListRetrieval;
    }

    /**
     *  @return the refresh interval for the nodelist information cache, in seconds
     */
    public Integer getNodelistRefreshIntervalSeconds() {
        return nodelistRefreshIntervalSeconds;
    }

    /**
     *
     * @param i  in seconds, the minimum interval between nodelist information cache refreshes
     */
    public void setNodelistRefreshIntervalSeconds(Integer nodelistRefreshIntervalSeconds) {
        this.nodelistRefreshIntervalSeconds = nodelistRefreshIntervalSeconds;
    }

    public boolean isUseSchemaValidation() {
        return useSchemaValidation;
    }

    public void setUseSchemaValidation(boolean useSchemaValidation) {
        this.useSchemaValidation = useSchemaValidation;
    }
}
