/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            // Sooo... set the session to something bogus for now
            // because PortalCertificateManager shouldn't know what to do
            // and because this filter does not know how each rest endpoint
            // handles InvalidTokens.
            logger.error("Invalid Token in the PortalCertificateFilter. Passing to endpoint " + httpRequest.getRequestURI() + " for triage");
            session = new Session();
        }
    	if (session == null) {
	    	// try to get the certificate from the portal instead
	        X509Certificate certificate = null;
			try {
				certificate = PortalCertificateManager.getInstance().getCertificate((HttpServletRequest) request);
				logger.debug("Proxy certificate for the request = " + certificate);
				if (certificate != null) {
					request.setAttribute("javax.servlet.request.X509Certificate", certificate);
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
