/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.node;

import java.io.IOException;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.util.Constants;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Session;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Returns a list of nodes that have been registered with the DataONE infrastructure.
 * This list is also referred to as the registry.
 *
 * This package will also edit and add to the registry
 * @author waltz
 */
@Controller("nodeController")
public class NodeController extends AbstractProxyController implements ServletContextAware {

     Logger logger = Logger.getLogger(NodeController.class.getName());

    private static final String NODE_PATH_V1 = "/v1/" + Constants.RESOURCE_NODE + "/";
    private static final String NODELIST_PATH_V1 = "/v1/" + Constants.RESOURCE_NODE;
    private ServletContext servletContext;

    CertificateManager certificateManager = CertificateManager.getInstance();
    MultipartRequestResolver multipartRequestResolver = new MultipartRequestResolver("/tmp", 1000000000, 0);
    static final int SMALL_BUFF_SIZE = 25000;
    static final int MED_BUFF_SIZE = 50000;
    static final int LARGE_BUFF_SIZE = 100000;

    @Autowired
    @Qualifier("cnNodeRegistry")
    NodeRegistryService  nodeRegistry;
    
    @RequestMapping(value = {NODELIST_PATH_V1, NODE_PATH_V1}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented{

        //NodeList nodeList = nodeListRetrieval.retrieveNodeList(request, response, servletContext);
        NodeList nodeList;
 
        nodeList = nodeRegistry.listNodes();

        return new ModelAndView("xmlNodeListViewResolver", "org.dataone.service.types.v1.NodeList", nodeList);

    }


    @RequestMapping(value = NODE_PATH_V1 + "**", method = RequestMethod.GET)
    public void getNode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        throw new Exception("search Not implemented Yet!");

    }
    @RequestMapping(value = {NODELIST_PATH_V1, NODE_PATH_V1}, method = RequestMethod.POST)
    public ModelAndView register(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, NotImplemented, InvalidRequest, NotAuthorized, IdentifierNotUnique{
        Node node = null;
        MultipartFile nodeDataMultipart = null;
        Session session = certificateManager.getSession(fileRequest);
        Set<String> keys = fileRequest.getFileMap().keySet();
        for (String key : keys) {
            logger.info("Found filepart " + key);
            if (key.equalsIgnoreCase("node")) {
                nodeDataMultipart = fileRequest.getFileMap().get(key);
            }
        }
        if (nodeDataMultipart != null) {
            try {
                node = TypeMarshaller.unmarshalTypeFromStream(Node.class, nodeDataMultipart.getInputStream());
            } catch (IOException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (InstantiationException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (IllegalAccessException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (JiBXException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            }

        } else {
            throw new InvalidRequest("4843", "New Node Xml not found in MultiPart request");
        }
        NodeReference nodeReference = nodeRegistry.register(node);
        return new ModelAndView("xmlNodeReferenceViewResolver", "org.dataone.service.types.v1.NodeReference", nodeReference);
    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
