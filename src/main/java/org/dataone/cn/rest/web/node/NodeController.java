/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.node;

import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.service.Constants;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.NodeList;
import org.jibx.runtime.JiBXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
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

    private static final String GET_NODE_PATH = "/" + Constants.RESOURCE_NODE + "/";
    private static final String GET_NODELIST_PATH = "/" + Constants.RESOURCE_NODE;
    private ServletContext servletContext;
    /* This should be uncommented out when a real solution is in place
    @Autowired
    @Qualifier("nodeListRetrieval")
    NodeListRetrieval  nodeListRetrieval;
    @RequestMapping(value = {GET_NODELIST_PATH, GET_NODE_PATH}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure{

        NodeList nodeList = nodeListRetrieval.retrieveNodeList(request, response, servletContext);

        return new ModelAndView("xmlNodeListViewResolver", "org.dataone.service.types.NodeList", nodeList);

    }
    */
    private org.springframework.core.io.Resource nodeRegistryResource;

    // this is the junk solution that is no good for anybody
    @RequestMapping(value = {GET_NODELIST_PATH, GET_NODE_PATH}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, IOException, InstantiationException, IllegalAccessException, JiBXException{

        NodeList nodeList = TypeMarshaller.unmarshalTypeFromStream(NodeList.class, nodeRegistryResource.getInputStream());
        return new ModelAndView("xmlNodeListViewResolver", "org.dataone.service.types.NodeList", nodeList);

    }
    @RequestMapping(value = GET_NODE_PATH + "**", method = RequestMethod.GET)
    public void getNode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        throw new Exception("search Not implemented Yet!");

    }
    // corresponding junk resource to aforementioned solution
    @Resource
    public void setTestLogFilePersistDataName(org.springframework.core.io.Resource nodeRegistryResource) {
        this.nodeRegistryResource = nodeRegistryResource;
    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
