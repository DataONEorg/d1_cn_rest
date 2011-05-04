/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.node;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.cn.rest.proxy.controller.AbstractProxyController;
import org.dataone.service.Constants;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.NodeList;
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
    @Autowired
    @Qualifier("nodeListRetrieval")
    NodeListRetrieval  nodeListRetrieval;
    @RequestMapping(value = {GET_NODELIST_PATH, GET_NODE_PATH}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure{

        NodeList nodeList = nodeListRetrieval.retrieveNodeList(request, response, servletContext);

        return new ModelAndView("xmlNodeListViewResolver", "org.dataone.service.types.NodeList", nodeList);

    }

    @RequestMapping(value = GET_NODE_PATH + "**", method = RequestMethod.GET)
    public void getNode(HttpServletRequest request, HttpServletResponse response) throws Exception {

        throw new Exception("search Not implemented Yet!");

    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
