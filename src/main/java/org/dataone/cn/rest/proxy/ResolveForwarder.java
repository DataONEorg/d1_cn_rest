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
package org.dataone.cn.rest.proxy;

import org.dataone.cn.rest.proxy.AbstractProxyForwarder;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.servlet.http.ProxyServletRequestWrapper;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.util.Constants;

/**
 * Take a resolve request from a client and proxy it through metacat's getSystemMetadata endpoint.
 *
 * @author waltz
 */
public class ResolveForwarder extends AbstractProxyForwarder {

    private final String metacatServletPath = "/d1/cn";
    private final String metacatServletContextName = "/metacat";

    public ResolveForwarder(ServletContext servletContext){
        super(servletContext);
    }
    @Override
    public void forward(HttpServletRequest request, HttpServletResponse response, String version) throws ServiceFailure, NotFound, NotImplemented, InvalidRequest {
        ProxyServletRequestWrapper proxyServletWrapper = new ProxyServletRequestWrapper(request);
        String requestURI = request.getRequestURI();
        int objectStart = requestURI.indexOf(Constants.RESOURCE_RESOLVE);
        if (objectStart == -1) {
            throw new ServiceFailure("4150", ResolveForwarder.class.getName() + ": unable to determine the beginning of the subpath" + Constants.RESOURCE_RESOLVE);
        }
        String pid = requestURI.substring(objectStart + Constants.RESOURCE_RESOLVE.length(), requestURI.length());

        // the call to resolve needs to be sent to metacat as a call to getSystemMetadata
        String systemMetadataPathInfo = "/" + Constants.RESOURCE_META  + pid;
        proxyServletWrapper.setContextPath(metacatServletContextName);
        proxyServletWrapper.setRequestURI(metacatServletContextName + metacatServletPath + "/" + version + systemMetadataPathInfo);
        proxyServletWrapper.setServletPath(metacatServletPath + "/" + version);
        proxyServletWrapper.setPathInfo(systemMetadataPathInfo );
        debugWrapper(request, proxyServletWrapper);
        logger.info("proxy resolve: " + proxyServletWrapper.getRequestURI());
        try {
            forwardRequest(servletContext, metacatServletContextName, proxyServletWrapper, response);
        } catch (ServletException ex) {
            throw new ServiceFailure("4150", ResolveForwarder.class.getName() + ":\n" + ex.getClass().getSimpleName() + ":\n" + ex.getMessage());
        } catch (IOException ex) {
            throw new ServiceFailure("4150", ResolveForwarder.class.getName() + ":\n" + ex.getClass().getSimpleName() + ":\n" + ex.getMessage());
        } catch (NotFound ex) {
            ex.setDetail_code("4140");
            throw ex;
        } catch (ServiceFailure ex) {
            ex.setDetail_code("4150");
            throw ex;
        }
    }
}
