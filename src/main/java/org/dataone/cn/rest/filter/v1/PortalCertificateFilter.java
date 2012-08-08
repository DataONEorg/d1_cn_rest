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

package org.dataone.cn.rest.filter.v1;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.types.v1.Session;

/**
 * Adds client certificate to the request when:
 *  1. It is not already available
 *  2. It is available from the portal service
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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
		
    	logger.debug("Performing certificate filter");

    	// check if we have the certificate (session) already

    	Session session = null;
        try {
            session = CertificateManager.getInstance().getSession((HttpServletRequest) request);
        } catch (InvalidToken ex) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            // If everything beyond this filter has been implemented correctly,
            // then the InvalidToken will be caught by the rest endpoint whose job
            // it is to validate any certificate it finds on the request.
            // But pass it on the PortalCertificateManager in case that component
            // knows what to do
            logger.error("Invalid Token in the PortalCertificateFilter. Passing to endpoint " + httpRequest.getRequestURI() + " for triage");
        }
    	if (session == null) {
	    	// try to get the certificate from the portal instead
	        X509Certificate certificate = null;
			try {
				certificate = PortalCertificateManager.getInstance().getCertificate((HttpServletRequest) request);
				logger.debug("Proxy certificate for the request = " + certificate);
				if (certificate != null) {
				    X509Certificate[] x509Certificates = new X509Certificate[] { certificate };
					request.setAttribute("javax.servlet.request.X509Certificate", x509Certificates);
					logger.debug("Added proxy certificate to the request");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
    	}

        // continue the request
        fc.doFilter(request, response);

    }

    @Override
    public void destroy() {
        logger.info("destroy PortalCertificateFilter");
    }
}
