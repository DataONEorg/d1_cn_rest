/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.web.base;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

/**
 * This controller will provide a default xml serialization of the Node that is
 * represented by this CN. This functionality has not yet been defined in
 * an API or documentation but was suggested in July, 2011.
 *
 * The node also acts as a
 *
 * @author waltz
 */
@Controller("baseController")
public class BaseController implements ServletContextAware{

    @Autowired
    @Qualifier("cnNodeRegistry")
    NodeRegistryService  nodeRetrieval;
    private ServletContext servletContext;

    @Value("${cn.nodeId}")
    String nodeIdentifier;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getNode(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{

        Node node;

        node = nodeRetrieval.getNode(nodeIdentifier);

        return new ModelAndView("xmlNodeViewResolver", "org.dataone.service.types.v1.Node", node);

    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

}
