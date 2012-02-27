/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.web.base;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.io.OutputStream;
import org.dataone.cn.rest.web.AbstractWebController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
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
public class BaseController extends AbstractWebController implements ServletContextAware{

    public static Log logger = LogFactory.getLog(BaseController.class);
    @Autowired
    @Qualifier("cnNodeRegistry")
    NodeRegistryService  nodeRetrieval;
    private ServletContext servletContext;

    @Value("${cn.nodeId}")
    String nodeIdentifier;
    NodeReference nodeReference;
    private static final String RESOURCE_MONITOR_PING_V1 = "/v1/" + Constants.RESOURCE_MONITOR_PING;
    private static final String RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 = "/v1/" + Constants.RESOURCE_CHECKSUM;

    SimpleDateFormat pingDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getNode(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{

        Node node;

        node = nodeRetrieval.getNode(nodeReference);

        return new ModelAndView("xmlNodeViewResolver", "org.dataone.service.types.v1.Node", node);

    }
    @RequestMapping(value = {RESOURCE_MONITOR_PING_V1, RESOURCE_MONITOR_PING_V1 + "/" }, method = RequestMethod.GET)
    public void ping(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{
        OutputStream responseStream = null;
        boolean throwFailure = false;
        String failureMessage = "";
        try {
            Date today = new Date();
            response.addDateHeader("Date", today.getTime());
            response.addIntHeader("Expires", -1);
            response.addHeader("Cache-Control", "private, max-age=0");
            response.addHeader("Content-Type", "text/xml");
            responseStream = response.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
            failureMessage = ex.getMessage();
            throwFailure = true;
        } finally {
            try {
                responseStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                failureMessage = ex.getMessage();
                throwFailure = true;
            }
        }
        if (throwFailure) {
            throw new ServiceFailure("2042", failureMessage);
        }

    }

    @RequestMapping(value = {RESOURCE_LIST_CHECKSUM_ALGORITHM_V1, RESOURCE_LIST_CHECKSUM_ALGORITHM_V1 + "/" }, method = RequestMethod.GET)
    public ModelAndView listChecksumAlgorithms(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented, NotFound{
        ChecksumAlgorithmList checksumAlgorithmList = new ChecksumAlgorithmList();
        String cnChecksumList = Settings.getConfiguration().getString("cn.checksumAlgorithmList");
        String[] checksums = cnChecksumList.split(";");
        for (int i = 0; i < checksums.length; i++) {
             checksumAlgorithmList.addAlgorithm(checksums[i]);
        }
        return new ModelAndView("xmlNodeViewResolver", "org.dataone.service.types.v1.ChecksumAlgorithmList", checksumAlgorithmList);

    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

}
