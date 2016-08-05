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

package org.dataone.cn.rest.v1;


import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dataone.cn.rest.proxy.ResolveForwarder;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.util.Constants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;


/**
 * 
 * The CNRead API mostly is implemented by Metacat.
 * In order to implement resolve, the request must be
 * forwarded to Metacat in order to filter it
 * into ObjectLocationList on the way back.
 * 
 * The reason for the filter strategy is that the
 * d1-cn-rest has direct access to LDAP
 *
 * @author waltz
 *
 */

@Controller("cnReadControllerV1")
public class ReadController  implements ServletContextAware {

    private ServletContext servletContext;
    
    private static final String RESOLVE_PATH = "/v1/" + Constants.RESOURCE_RESOLVE +  "/";

    Logger logger = Logger.getLogger(ReadController.class);
    
    ResolveForwarder resolveForwarder = null;
    /*
     * initialize class scope variables immediately after the controller has
     * been initialized by Spring
     * 
     * @author waltz
     * @returns void
     */
    @PostConstruct
    public void init() {
        resolveForwarder = new ResolveForwarder(servletContext);
    }
    /*
     * Resolve is proxied through Metacat's getSystemMetadata method and then filtered
     * to produce the correct ObjectLocationList
     *
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String acceptType
     * @return void
     * @exception
     */
    @RequestMapping(value = RESOLVE_PATH + "**", method = RequestMethod.GET, headers = "Accept=*/*")
    public void resolve(HttpServletRequest request, HttpServletResponse response,
            @RequestHeader("Accept") String acceptType) throws ServiceFailure, NotFound, NotImplemented, InvalidRequest {
        resolveForwarder.forward(request, response, "v1");
 
    }
    /*
     * Resolve is proxied through Metacat's getSystemMetadata method and then filtered
     * to produce the correct ObjectLocationList
     *
     * @author Robert Patrick Waltz (waltz)
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @return void
     * @exception
     */

    @RequestMapping(value = RESOLVE_PATH + "**", method = RequestMethod.GET)
    public void resolve(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotFound, NotImplemented, InvalidRequest {
        resolveForwarder.forward(request, response, "v1");
    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

}
