/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.web.notify;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.service.cn.CoordinatingNodeDataReplication;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author waltz
 *
 */
@Controller
public class NotifyController extends AbstractWebController {
    /*
     * To change this template, choose Tools | Templates
     * and open the template in the editor.
     */
     static final Logger logger = Logger.getLogger(NotifyController.class.getName());
    @Autowired
    @Qualifier("multipartRequestResolver")
    MultipartRequestResolver multipartRequestResolver;

    @Autowired
    @Qualifier("dataReplicationService")
    CoordinatingNodeDataReplication dataReplicationService;

    @RequestMapping(value = {"/notify", "/notify/"}, method = RequestMethod.POST, headers = "accept=*/*")
    public void getNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthToken token = new AuthToken();
        token.setToken("public");
        Identifier pid = new Identifier();
        MultipartFile sytemMetaDataMultipart = null;
        MultipartFile objectMultipart = null;
        if (multipartRequestResolver.isMultipartContent(request)) {
          MultipartRequest multipartRequest =  multipartRequestResolver.resolveMultipart(request);
          for (String key : multipartRequest.getMultipartParameters().keySet()) {
            logger.debug("Parameter Key: " + key + ": value:" + multipartRequest.getMultipartParameters().get(key));
          }
          for (String key : multipartRequest.getMultipartFiles().keySet()) {
            logger.debug("MultiPartFile Key: " + key + ": value:" + multipartRequest.getMultipartParameters().get(key));
          }
//        if (dataReplicationService.setReplicationStatus(token, pid, ReplicationStatus.QUEUED)) {
//            response.setStatus(HttpServletResponse.SC_OK);
//        }
        } else {
            logger.debug("Request is not Multipart!");
        }
    }
}
