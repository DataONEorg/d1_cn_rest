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

package org.dataone.cn.rest.web.base.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.configuration.Settings;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
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
 * This controller will provide a default xml serialization of the Node that is
 * represented by this CN. This functionality has not yet been defined in
 * an API or documentation but was suggested in July, 2011.
 *
 * The controller also acts as a passthru mechanism for relatively static content
 * that is provided by the node.properties file.
 * Most content of the CN is provided by backend services, such as LDAP or metacat,
 * but some trivial content can be read directly 
 * from a properties file and easily exposed as xml
 *
 * @author waltz
 */
@Controller("baseController")
public class BaseController extends AbstractWebController implements ServletContextAware{

    public static Log logger = LogFactory.getLog(BaseController.class);
    @Autowired
    @Qualifier("cnNodeRegistry")
    NodeRegistryService  nodeRetrieval;
    private ServletContext servletContext;

    String nodeIdentifier = Settings.getConfiguration().getString("cn.nodeId");
    NodeReference nodeReference;
    private static final String RESOURCE_MONITOR_PING_V1 = "/v1/" + Constants.RESOURCE_MONITOR_PING;
    private static final String RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 = "/v1/" + Constants.RESOURCE_CHECKSUM;
    private static final String RESOURCE_LIST_QUERY_V1 = "/v1/" + Constants.RESOURCE_QUERY;
    SimpleDateFormat pingDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getNode(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{

        Node node;

        node = nodeRetrieval.getNode(nodeReference);

        return new ModelAndView("xmlNodeViewResolver", "org.dataone.service.types.v1.Node", node);

    }
    @RequestMapping(value = {RESOURCE_MONITOR_PING_V1, RESOURCE_MONITOR_PING_V1 + "/" }, method = RequestMethod.GET)
    public void ping(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{
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

    @RequestMapping(value = {RESOURCE_LIST_CHECKSUM_ALGORITHM_V1, RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 + "/" }, method = RequestMethod.GET)
    public ModelAndView listChecksumAlgorithms(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{
        ChecksumAlgorithmList checksumAlgorithmList = new ChecksumAlgorithmList();


        String[] checksums = Settings.getConfiguration().getStringArray("cn.checksumAlgorithmList");

        for (int i = 0; i < checksums.length; i++) {
             logger.info(checksums[i]);
             checksumAlgorithmList.addAlgorithm(checksums[i]);
        }
        return new ModelAndView("xmlChecksumAlgorithmListViewResolver", "org.dataone.service.types.v1.ChecksumAlgorithmList", checksumAlgorithmList);

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
    @RequestMapping(value = {RESOURCE_LIST_QUERY_V1, RESOURCE_LIST_QUERY_V1 + "/" }, method = RequestMethod.GET)
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
        return new ModelAndView("xmlQueryEngineListViewResolver", "org.dataone.service.types.v1_1.QueryEngineList", queryEngineList);

    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

}
