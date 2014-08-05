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
 * $Id: PortalCertificateFilter.java 8327 2012-04-23 23:06:58Z pippin $
 */

package org.dataone.cn.rest.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;

/**
 * Short-circuits the request to return a ServiceFailure  
 * @author rnahf
 */
public class CNServiceDisableFilter implements Filter {

    Logger logger = Logger.getLogger(CNServiceDisableFilter.class);
    boolean disableService = Settings.getConfiguration().getBoolean("cn.service.disable", /* default value */ false);
    byte[] serializedException;
    
    @Override
    public void init(FilterConfig fc) throws ServletException {
        logger.info("init CNServiceDisableFilter");
        ServiceFailure sf = new ServiceFailure("0","This node is set to disabled.");
		serializedException = sf.serialize(BaseException.FMT_XML).getBytes(Charset.forName("UTF-8"));	
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
		
    	logger.debug("Performing CNServiceDisable filter");

    	if (disableService) {
    		response.setContentType("text/xml");
    		((HttpServletResponse) response).setStatus(500);
    		OutputStream os = response.getOutputStream();
    		try {
    			os.write(this.serializedException);
    		} 
    		finally {
    			os.flush();
    			os.close();
    		}
    		
    	} else {
    		// continue the request
    		fc.doFilter(request, response);
    	}
    }

    @Override
    public void destroy() {
        logger.info("destroy CNServiceDisableFilter");
    }
}
