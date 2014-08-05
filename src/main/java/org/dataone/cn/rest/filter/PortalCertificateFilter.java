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

package org.dataone.cn.rest.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.InvalidToken;

/**
 * Adds client certificate to the request when: 1. It is not already available
 * 2. It is available from the portal service
 * 
 * @author leinfelder
 */
public class PortalCertificateFilter implements Filter {

    Logger logger = Logger.getLogger(PortalCertificateFilter.class);

    @Override
    public void init(FilterConfig fc) throws ServletException {
        logger.info("init PortalCertificateFilter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc)
            throws IOException, ServletException {

        logger.debug("Performing certificate filter");

        try {
            PortalCertificateManager.getInstance().putPortalCertificateOnRequest(
                    (HttpServletRequest) request);
        } catch (InvalidToken ex) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            // If everything beyond this filter has been implemented correctly,
            // then the InvalidToken will be caught by the rest endpoint whose
            // job
            // it is to validate any certificate it finds on the request.
            // But pass it on the PortalCertificateManager in case that
            // component
            // knows what to do
            logger.error("Invalid Token in the PortalCertificateFilter. Passing to endpoint "
                    + httpRequest.getRequestURI() + " for triage");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        	// do something different?
            e.printStackTrace();
        }

        // continue the request
        fc.doFilter(request, response);

    }

    @Override
    public void destroy() {
        logger.info("destroy PortalCertificateFilter");
    }
}
