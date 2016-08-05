/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.rest.exceptions;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.AbstractServiceController;
import org.dataone.service.exceptions.AuthenticationTimeout;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.core.io.ClassPathResource;

/**
 * Handle any error that is thrown by Tomcat and transform it into a DataONE Exception response
 * 
 * @author waltz
 */
@Controller
public class ExceptionController extends AbstractServiceController {
    public static Log log = LogFactory.getLog(ExceptionController.class);
    static final int SIZE = 8192;
    
    @RequestMapping(value = "/{errorId}")
    public void get(HttpServletRequest request, HttpServletResponse response, @PathVariable String errorId) throws NotFound, ServiceFailure, NotImplemented, InvalidRequest, InvalidCredentials, NotAuthorized, AuthenticationTimeout, InsufficientResources {

            int status = 500;
            log.info("received error of " + errorId);
            NumberFormat nf = NumberFormat.getInstance();
            nf.setParseIntegerOnly(true);
            try {
                Number nstatus = nf.parse(errorId);
                status = nstatus.intValue();
            } catch (ParseException ex) {
                throw new ServiceFailure("500", "Could not determine the server error thrown");
            }
            response.setStatus(status);
            switch (status) {
                case 400: {
                    throw new InvalidRequest("400", "Bad Request: The request could not be understood by the server due to malformed syntax.");
                }
                case 401: {
                    throw new InvalidCredentials("401", "Unauthorized: The request requires user authentication.");
                }
                case 403: {
                    throw new NotAuthorized("403", "Forbidden: The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated.");
                }
                case 404: {
                    throw new NotFound("404", "Not Found: The server has not found anything matching the Request-URI.");
                }
                case 405: {
                    throw new InvalidRequest("405", "Method Not Allowed: The method specified in the Request-Line is not allowed for the resource identified by the Request-URI.");
                }
                case 406: {
                    throw new InvalidRequest("406", "Not Acceptable: The resource identified by the request is only capable of generating response entities which have content characteristics not acceptable according to the accept headers sent in the request.");
                }
                case 407: {
                    throw new NotAuthorized("407", "Proxy Authentication Required: The client must first authenticate itself with the proxy.");
                }
                case 408: {
                    throw new AuthenticationTimeout("408", "Request Timeout: The client did not produce a request within the time that the server was prepared to wait.");
                }
                case 409: {
                    throw new InvalidRequest("409", "Conflict: The request could not be completed due to a conflict with the current state of the resource.");
                }
                case 410: {
                    throw new NotFound("410", "Gone: The requested resource is no longer available at the server and no forwarding address is known.");
                }
                case 411: {
                    throw new InvalidRequest("411", "Length Required: The server refuses to accept the request without a defined Content-Length.");
                }
                case 412: {
                    throw new InvalidRequest("412", "Precondition Failed: The precondition given in one or more of the request-header fields evaluated to false when it was tested on the server.");
                }
                case 413: {
                    throw new InsufficientResources("413", "Request Entity Too Large: The server is refusing to process a request because the request entity is larger than the server is willing or able to process.");
                }
                case 414: {
                    throw new InvalidRequest("414", "Request-URI Too Long: The server is refusing to service the request because the Request-URI is longer than the server is willing to interpret.");
                }
                case 415: {
                    throw new InvalidRequest("415", "Unsupported Media Type: The server is refusing to service the request because the entity of the request is in a format not supported by the requested resource for the requested method.");
                }
                case 416: {
                    throw new InvalidRequest("416", "Requested Range Not Satisfiable: A server SHOULD return a response with this status code if a request included a Range request-header field.");
                }
                case 417: {
                    throw new InvalidRequest("417", "Expectation Failed: The expectation given in an Expect request-header field could not be met by this server.");
                }
                case 500: {
                    throw new ServiceFailure("500", "Internal Server Error: The server encountered an unexpected condition which prevented it from fulfilling the request.");
                }
                case 501: {
                    throw new NotImplemented("501", "Not Implemented: The server does not support the functionality required to fulfill the request.");
                }
                case 502: {
                    throw new ServiceFailure("502", "Bad Gateway: The server, while acting as a gateway or proxy, received an invalid response from the upstream server it accessed in attempting to fulfill the request.");
                }
                case 503: {
                    throw new ServiceFailure("503", "Service Unavailable: The server is currently unable to handle the request due to a temporary overloading or maintenance of the server.");
                }
                case 504: {
                    throw new ServiceFailure("504", "Gateway Timeout: The server, while acting as a gateway or proxy, did not receive a timely response from the upstream server specified by the URI (e.g. HTTP, FTP, LDAP) or some other auxiliary server (e.g. DNS) it needed to access in attempting to complete the request.");
                }
                case 505: {
                    throw new ServiceFailure("505", "HTTP Version Not Supported: The server does not support, or refuses to support, the HTTP protocol version that was used in the request message.");
                }
                default: {
                    throw new ServiceFailure("500", "Could not determine the server error thrown");
                }
            }

                //            this.writeToResponse(errorXml.getInputStream(), response.getOutputStream());

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
