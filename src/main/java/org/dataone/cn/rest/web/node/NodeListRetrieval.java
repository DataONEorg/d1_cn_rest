/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.web.node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.proxy.http.ProxyServletResponseWrapper;
import org.dataone.cn.rest.proxy.service.ProxyObjectService;
import org.dataone.cn.rest.proxy.util.AcceptType;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.NodeList;
import org.dataone.service.types.SystemMetadata;
import org.jibx.runtime.JiBXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author waltz
 */
@Component
@Qualifier("nodeListRetrieval")
public class NodeListRetrieval {
    private String nodeListIdentifier = "registry";
    @Autowired
    @Qualifier("proxyObjectService")
    ProxyObjectService proxyObjectService;
    public NodeList retrieveNodeList (HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws ServiceFailure{
                NodeList nodeList = null;
        SystemMetadata systemMetadata = null;
        String metaNodeListPath = null;
        String currentNodeListIdentifier = nodeListIdentifier;
        AuthToken token = new AuthToken();
        token.setToken("public");
        // Flip through all the registry entries to find the most up-to-date!
        // XXX probably need to do determine a way to cache this so that
        // we don't always have to query metacat for it,
        // but to make it efficient, we will have to base a cache upon
        // some kind of signal of update with metacat, So re-evaluate code
        // when JMS is implemented
        do {
            // we have made at least one pass of the loop if systemMetadata is not null
            if (systemMetadata != null) {
                List<Identifier> obsoletedBy = systemMetadata.getObsoletedByList();
                currentNodeListIdentifier = obsoletedBy.get(obsoletedBy.size() - 1).getValue();
            }
            ProxyServletResponseWrapper metaResponse = new ProxyServletResponseWrapper(response);
            try {
                proxyObjectService.getSystemMetadata(servletContext, request, metaResponse, currentNodeListIdentifier, AcceptType.XML);
            } catch (BaseException ex) {
                throw new ServiceFailure(ex.getDetail_code(), "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getDescription());
            } catch (Exception ex) {
                throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
            }
            ByteArrayInputStream inputStream;

            inputStream = new ByteArrayInputStream(metaResponse.getData());

            try {
                systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, inputStream);
            } catch (IOException ex) {
                throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
            } catch (InstantiationException ex) {
                throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
            } catch (IllegalAccessException ex) {
                throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
            } catch (JiBXException ex) {
                throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
            }
            response.resetBuffer();
            response.reset();
        } while (!(systemMetadata.getObsoletedByList().isEmpty()));
        ProxyServletResponseWrapper objectResponse = new ProxyServletResponseWrapper(response);
        try {
            proxyObjectService.get(servletContext, request, objectResponse, systemMetadata.getIdentifier().getValue(), AcceptType.XML);
        } catch (BaseException ex) {
            throw new ServiceFailure(ex.getDetail_code(), "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getDescription());
        } catch (Exception ex) {
            throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        }
        // XXX Node list will grow, so this may need to be handled in some other fashion in future
        // maybe wrap the unmarshalTypeFromStream with an output stream call as well?
        ByteArrayInputStream inputStream;

        inputStream = new ByteArrayInputStream(objectResponse.getData());

        try {
            nodeList = TypeMarshaller.unmarshalTypeFromStream(NodeList.class, inputStream);
        } catch (IOException ex) {
            throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (InstantiationException ex) {
            throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        } catch (JiBXException ex) {
            throw new ServiceFailure("4801", "Proxied from CoordinatingNodeRegisterImpl.listNodes:" + ex.getMessage());
        }
        return nodeList;
    }
}
