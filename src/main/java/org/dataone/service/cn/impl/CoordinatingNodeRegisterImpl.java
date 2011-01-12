/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.service.cn.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.proxy.service.ProxyObjectService;
import org.dataone.cn.rest.proxy.util.AcceptType;
import org.dataone.cn.rest.http.ProxyHttpServletRequest;
import org.dataone.cn.rest.http.ProxyHttpServletResponse;
import org.dataone.cn.rest.http.ProxyHttpSession;
import org.dataone.service.cn.CoordinatingNodeRegister;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.Node;
import org.dataone.service.types.NodeList;
import org.dataone.service.types.Services;
import org.jibx.runtime.JiBXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author waltz
 */
public class CoordinatingNodeRegisterImpl implements CoordinatingNodeRegister {
    @Autowired
    @Qualifier("proxyObjectService")
    ProxyObjectService proxyObjectService;
    private ServletContext servletContext;
    static final Logger logger = Logger.getLogger(CoordinatingNodeRegisterImpl.class);
    @Override
    public NodeList listNodes(AuthToken token) throws NotImplemented, ServiceFailure {
        NodeList nodeList = null;
//        System
        ProxyHttpServletResponse proxyResponse = new ProxyHttpServletResponse();
        ProxyHttpSession httpSession = new ProxyHttpSession(servletContext);
        ProxyHttpServletRequest proxyRequest = new ProxyHttpServletRequest( httpSession, servletContext);
//        metaNodeListPath = "/meta/register";
 //   do {
        proxyRequest.setPath("/meta/nodelist");
        try {
            proxyObjectService.get(servletContext, proxyRequest, proxyResponse, "nodelist", AcceptType.XML);
        } catch (BaseException ex) {
            logger.error(ex.serialize(ex.FMT_XML));
            throw new ServiceFailure(ex.getDetail_code(),"Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getDescription());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new ServiceFailure("34501","Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        }
        // XXX save this to a file first.. i'll need to do some checking or something to cache it
        // against the Systemmetadata's lastupdatedate to determine whether or not to retrieve
        // it again...
        ByteArrayInputStream inputStream = new ByteArrayInputStream(proxyResponse.getBinaryContent());
        try {
            nodeList = TypeMarshaller.unmarshalTypeFromStream(NodeList.class, inputStream);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            throw new ServiceFailure("34502","Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (InstantiationException ex) {
            logger.error(ex.getMessage());
            throw new ServiceFailure("34503","Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.error(ex.getMessage());
            throw new ServiceFailure("34504","Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (JiBXException ex) {
            logger.error(ex.getMessage());
            throw new ServiceFailure("34505","Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        }
//    } while (nodeList.)
        return nodeList;
    }

    @Override
    public Node get(AuthToken token, Identifier pid) throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addNodeCapabilities(AuthToken token, Identifier pid, Services capabilities) throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier register(AuthToken token, Services capabilities) throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}
