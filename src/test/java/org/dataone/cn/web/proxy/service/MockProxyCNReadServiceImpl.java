/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.proxy.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.rest.proxy.service.ProxyCNReadService;
import org.dataone.cn.rest.proxy.util.AcceptType;
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
public class MockProxyCNReadServiceImpl implements ProxyCNReadService {

    static final int SIZE = 8192;
    @Autowired
    @Qualifier("nodeSystemMetadataResource")
    private Resource nodeSystemMetadataResource;


    @Override
    public void get(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getSystemMetadata(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        try {
            this.writeToResponse(nodeSystemMetadataResource.getInputStream(), response.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ServiceFailure("1090", ex.getMessage());
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
    @Override
    public void assertRelation(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, String pid, AcceptType acceptType) throws ServiceFailure, NotFound {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void writeToResponse(InputStream in, OutputStream out) throws IOException {
        try {
            BufferedInputStream f = new BufferedInputStream(in);
            byte[] barray = new byte[SIZE];
            int nRead;
            while ((nRead = f.read(barray, 0, SIZE)) != -1) {
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

    public Resource getNodeSystemMetadataResource() {
        return nodeSystemMetadataResource;
    }

    public void setNodeSystemMetadataResource(Resource nodeSystemMetadataResource) {
        this.nodeSystemMetadataResource = nodeSystemMetadataResource;
    }


}
