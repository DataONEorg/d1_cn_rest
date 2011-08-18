/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.web.error;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.service.exceptions.NotFound;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author waltz
 */
@Controller
public class ErrorController {
    public static Log log = LogFactory.getLog(ErrorController.class);
    static final int SIZE = 8192;
    
    @RequestMapping(value = "/{errorId}", method = RequestMethod.GET)
    public void get(HttpServletRequest request, HttpServletResponse response, @PathVariable String errorId) throws NotFound {
        ClassPathResource errorXml = new ClassPathResource("/org/dataone/cn/rest/resources/error/"+errorId +".xml");
        try {
            this.writeToResponse(errorXml.getInputStream(), response.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new NotFound("101", ex.getMessage());
        }
    }
    public void writeToResponse(InputStream in, OutputStream out) throws IOException {
        try {
            BufferedInputStream f = new BufferedInputStream(in);
            byte[] barray = new byte[SIZE];
            int nRead;
            while ((nRead = f.read(barray, 0, SIZE)) != -1) {
                String printit = new String(Arrays.copyOf(barray, nRead));
                log.info(printit);
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
}
