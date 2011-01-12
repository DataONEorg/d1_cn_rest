/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.controller.node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.service.cn.CoordinatingNodeRegister;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.NodeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Returns a list of nodes that have been registered with the DataONE infrastructure.
 * This list is also referred to as the registry.
 *
 * This package will also edit and add to the registry
 * @author waltz
 */
@Controller
public class NodeController {
        private static final String GET_NODELIST_PATH = "/node";
        private static final String GET_NODE_PATH = "/node/";
	@Autowired
	@Qualifier("registerService")
	CoordinatingNodeRegister registerService;


	@RequestMapping(value = GET_NODELIST_PATH, method = RequestMethod.GET, headers="accept=*/*")
	public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws Exception {
                AuthToken token = new AuthToken();
                token.setToken("public");
                NodeList nodeList = registerService.listNodes(token);

		return new ModelAndView("xmlMetaViewResolver", "org.dataone.service.types.NodeList", nodeList);

	}
	@RequestMapping(value = GET_NODE_PATH + "**", method = RequestMethod.GET, headers="accept=*/*")
	public void getNode(HttpServletRequest request, HttpServletResponse response) throws Exception {

		throw new Exception("search Not implemented Yet!");

	}
}
