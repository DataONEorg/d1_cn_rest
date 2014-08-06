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

package org.dataone.cn.rest.proxy.service.impl.mock;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.proxy.service.ProxyCNReadService;
import org.dataone.cn.rest.proxy.util.AcceptType;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
/**
 *
 * @author waltz
 */
@Service("mockProxyCNReadServiceImpl")
@Qualifier("proxyCNReadService")
public class ProxyCNReadServiceImpl implements ProxyCNReadService {
    public static Log log = LogFactory.getLog(ProxyCNReadServiceImpl.class);
/**
 *
 * @author waltz
 */

    static final int SIZE = 8192;
    @Autowired
    @Qualifier("readSystemMetadataResource")
    private Resource readSystemMetadataResource;
    @Autowired
    @Qualifier("readScienceMetadataResource")
    private Resource readScienceMetadataResource;

    @Override
    public void get(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        try {
            this.writeToResponse(readScienceMetadataResource.getInputStream(), response.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new NotFound("1020", ex.getMessage());
        }
    }
    
    @Override
    public void view(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void listViews(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, AcceptType acceptType) throws ServiceFailure, InvalidRequest {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getSystemMetadata(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        log.info("Mock Proxy getSystemMetadata");
        try {
            this.writeToResponse(readSystemMetadataResource.getInputStream(), response.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new NotFound("1060", ex.getMessage());
        }
        
    }

    @Override
    public void resolve(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getChecksum(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeToResponse(InputStream in, OutputStream out) throws IOException {
        try {
            BufferedInputStream f = new BufferedInputStream(in);
            byte[] barray = new byte[SIZE];
            int nRead;
            while ((nRead = f.read(barray, 0, SIZE)) != -1) {
                String printit = new String(Arrays.copyOf(barray, nRead));
                log.info(printit);
                out.write(barray, 0, nRead);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    public void assertRelation(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public void listObjects(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, AcceptType acceptType) throws ServiceFailure, InvalidRequest {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public Resource getNodeScienceMetadataResource() {
        return readScienceMetadataResource;
    }

    public void setNodeScienceMetadataResource(Resource readScienceMetadataResource) {
        this.readScienceMetadataResource = readScienceMetadataResource;
    }

    public Resource getNodeSystemMetadataResource() {
        return readSystemMetadataResource;
    }

    public void setNodeSystemMetadataResource(Resource readSystemMetadataResource) {
        this.readSystemMetadataResource = readSystemMetadataResource;
    }

    @Override
    public void describe(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void create(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registerSystemMetadata(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void updateSystemMetadata(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure {
        throw new UnsupportedOperationException("Not supported yet.");
    }




}

