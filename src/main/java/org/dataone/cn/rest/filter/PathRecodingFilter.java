/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.filter;

import java.io.IOException;
import java.util.logging.Level;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.DecoderException;
import org.apache.log4j.Logger;
import org.dataone.service.exceptions.ServiceFailure;

/**
 *
 * @author waltz
 */
public class PathRecodingFilter implements Filter {
    Logger logger = Logger.getLogger(PathRecodingFilter.class);
    private FilterConfig filterConfig = null;
    @Override
    public void init(FilterConfig fc) throws ServletException {
        logger.info("init ResolveFilter");
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
            //Get the request
            RecodePathFilterRequest recodedPathRequest;
        try {
            recodedPathRequest = new RecodePathFilterRequest((HttpServletRequest) request);
        } catch (DecoderException ex) {
            logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        } catch (ServiceFailure e) {
            logger.error(e.getMessage());
        	throw new RuntimeException(e);
		}

            //continue the request
            fc.doFilter(recodedPathRequest,response);

    }

    @Override
    public void destroy() {
        logger.info("init ResolveFilter");
    }
}
