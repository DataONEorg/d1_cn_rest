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

package org.dataone.cn.rest.proxy;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.dataone.cn.servlet.http.ProxyServletRequestWrapper;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.AuthenticationTimeout;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedMetadataType;
import org.dataone.service.exceptions.UnsupportedType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.ServletContextAware;

/**
 * Takes a request and then forwards the request to another web app.
 * Specifically, for d1-cn-rest, it will take a request for getSystemMetadata
 * and forward the request to Metacat
 * 
 * The only subclass is the ResolveForwarder, but the abstract class is
 * generic enough to be reused for other contexts.
 * 
 * @author waltz
 */
public abstract class AbstractProxyForwarder implements ServletContextAware {

    /*
     *
    400 	Bad Request         Bad request if the request REST operation is invalid, serialization is erroneous, mime type is not supported, or resource is not supported.
    401 	Unauthorized        Authentication failure. Credentials are required or were invalid.
    403 	Forbidden           The current user does not have the right to perform the requested action.
    404 	Not Found           The object does not exist.
    405 	Method not allowed  The HTTP method used is not allowed on this resource. Response must include an Allow header indicating valid HTTP methods.
    406 	Not Acceptable      The resource identified by the request is only capable of generating response entities which have content characteristics not acceptable according to the accept headers sent in the request.
    408 	Request Timeout     The client did not produce a request within the time that the server was prepared to wait.
    409 	Conflict            The request could not be completed due to a conflict with the current state of the resource.
    410 	Gone                The resource is known to be permanently deleted (as opposed to 404 which indicates uncertainty about the state of the object).
    413 	Request Entity Too Large 	The server is refusing to process a request because the request entity is larger than the server is willing or able to process.
    415 	Unsupported Media Type 	The server is refusing to service the request because the entity of the request is in a format not supported by the requested resource for the requested method.
    500 	Internal Server Error 	The server encountered an unexpected condition which prevented it from fulfilling the request.
    501 	Not Implemented 	The server does not support the functionality required to fulfill the request. This is the appropriate response when the server does not recognize the request method and is not capable of supporting it for any resource.
     *
     *
     */
    Logger logger = Logger.getLogger(AbstractProxyForwarder.class.getName());
    
    /* A 'www-form-urlencoded' URL encoder/decoder */
    protected URLCodec urlCodec = new URLCodec();
    
    /* */
    protected ServletContext servletContext;
    
    public AbstractProxyForwarder(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    abstract public void forward(HttpServletRequest request, HttpServletResponse response, String version)
            throws ServiceFailure, NotFound, NotImplemented, InvalidRequest;
    
    protected void forwardRequest(ServletContext servletContext, String contextName, ProxyServletRequestWrapper proxyServletWrapper, HttpServletResponse response) throws ServletException, IOException, NotFound, ServiceFailure {
        ServletContext proxyServletContext = servletContext.getContext(contextName);
        if (proxyServletContext != null) {
            logger.info("CN Getting proxy...");

            try {
                logger.info("CN Dispatching: " + proxyServletWrapper.getServletPath() + proxyServletWrapper.getPathInfo());
                RequestDispatcher dispatch =
                        proxyServletContext.getRequestDispatcher(proxyServletWrapper.getServletPath() + proxyServletWrapper.getPathInfo());

                if (dispatch != null) {
                    logger.info("CN Dispatching proxy...");
                    dispatch.forward(proxyServletWrapper, response);
                    logger.info("CN Completed proxy...");

                } else {
                    throw new NotFound("n/a", "Servlet path " + proxyServletWrapper.getServletPath() + proxyServletWrapper.getPathInfo() + " of contextName " + contextName + " not found! "
                            + servletContext.getServerInfo());
                }
            } catch (IllegalArgumentException iae) {
                throw new NotFound("n/a", iae.getMessage());
            }
        } else {
            throw new ServiceFailure("n/a", contextName + " context not found! "
                    + servletContext.getServerInfo());
        }

    }

    protected void debugWrapper(HttpServletRequest request, ProxyServletRequestWrapper proxyServletWrapper) {
        /*     see values with just a plain old request object being sent through */
        logger.debug("proxy.request RequestURL: " + request.getRequestURL());
        logger.debug("proxy.request RequestURI: " + request.getRequestURI());
        logger.debug("proxy.request PathInfo: " + request.getPathInfo());
        logger.debug("proxy.request PathTranslated: " + request.getPathTranslated());
        logger.debug("proxy.request QueryString: " + request.getQueryString());
        logger.debug("proxy.request ContextPath: " + request.getContextPath());
        logger.debug("proxy.request ServletPath: " + request.getServletPath());
        logger.debug("proxy.request Method: " + request.getMethod());

        /*      uncomment to see what the parameters of servlet passed in are  */
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        for (String key : requestParameterMap.keySet()) {
            String[] values = request.getParameterValues(key);
            for (int i = 0; values.length > i; ++i) {
                logger.debug("proxy.request.ParameterMap: " + key + " " + values[i]);
            }
        }
        logger.debug("");
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            logger.debug("proxy.request " + attributeName + ": " + request.getAttribute(attributeName));
        }
        /*      values of proxyServletWrapper request object to be sent through */
        logger.debug("");
        logger.debug("proxy.wrapper RequestURL: " + proxyServletWrapper.getRequestURL());
        logger.debug("proxy.wrapper RequestURI: " + proxyServletWrapper.getRequestURI());
        logger.debug("proxy.wrapper PathInfo: " + proxyServletWrapper.getPathInfo());
        logger.debug("proxy.wrapper PathTranslated: " + proxyServletWrapper.getPathTranslated());
        logger.debug("proxy.wrapper QueryString: " + proxyServletWrapper.getQueryString());
        logger.debug("proxy.wrapper ContextPath: " + proxyServletWrapper.getContextPath());
        logger.debug("proxy.wrapper ServletPath: " + proxyServletWrapper.getServletPath());
        logger.debug("proxy.wrapper Method: " + proxyServletWrapper.getMethod());

        /*      uncomment to see what the parameters of servlet passed in are  */
        Map<String, String> parameterMap = proxyServletWrapper.getParameterMap();

        for (String key : parameterMap.keySet()) {
            String[] values = proxyServletWrapper.getParameterValues(key);
            for (int i = 0; values.length > i; ++i) {
                logger.debug("proxy.wrapper.ParameterMap: " + key + " " + values[i]);
            }
        }
        logger.debug("");

        attributeNames = proxyServletWrapper.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            logger.debug("proxy.wrapper " + attributeName + ": " + proxyServletWrapper.getAttribute(attributeName));
        }
        logger.debug("");
    }
    /* Exceptions.AuthenticationTimeout 	408 	The authentication request timed out. */
    @ResponseStatus(value = HttpStatus.REQUEST_TIMEOUT)
    @ExceptionHandler(AuthenticationTimeout.class)
    public void handleException(AuthenticationTimeout exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.IdentifierNotUnique 	409 	The provided identifier conflicts with an existing identifier in the DataONE system. When serializing, the identifier in conflict should be rendered in traceInformation as the value of an identifier key. */
    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ExceptionHandler(IdentifierNotUnique.class)
    public void handleException(IdentifierNotUnique exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.InsufficientResources 	413 	There are insufficient resources at the node to support the requested operation. */
    @ResponseStatus(value = HttpStatus.REQUEST_ENTITY_TOO_LARGE)
    @ExceptionHandler(InsufficientResources.class)
    public void handleException(InsufficientResources exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.InvalidCredentials 	401 	Indicates that the credentials supplied (to CN_crud.login() for example) are invalid for some reason. */
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidCredentials.class)
    public void handleException(InvalidCredentials exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.InvalidRequest 	400 	The parameters provided in the call were invalid. The names and values of parameters should included in traceInformation. */
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequest.class)
    public void handleException(InvalidRequest exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.InvalidSystemMetadata 	400 	The supplied system metadata is invalid. This could be because some required field is not set, the metadata document is malformed, or the value of some field is not valid. The content of traceInformation should contain additional information about the error encountered (e.g. name of the field with bad value, if the document is malformed). */
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidSystemMetadata.class)
    public void handleException(InvalidSystemMetadata exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.InvalidToken 	401 	The supplied authentication token could not be verified as being valid. */
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidToken.class)
    public void handleException(InvalidToken exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.NotAuthorized 	401 	The supplied identity information is not authorized for the requested operation. */
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotAuthorized.class)
    public void handleException(NotAuthorized exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.NotFound 	404 	Used to indicate that an object is not present on the node where the exception was raised. The error message should include a reference to the CN_crud.resolve() method URL for the objec */
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFound.class)
    public void handleException(NotFound exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.NotImplemented 	501 	A method is not implemented, or alternatively, features of a particular method are not implemented. */
    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(NotImplemented.class)
    public void handleException(NotImplemented exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.ServiceFailure 	500 	Some sort of system failure occurred that is preventing the requested operation from completing successfully. This error can be raised by any method in the DataONE API. */
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ServiceFailure.class)
    public void handleException(ServiceFailure exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* Exceptions.UnsupportedMetadataType 	400 	The science metadata document submitted is not of a type that is recognized by the DataONE system. */
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnsupportedMetadataType.class)
    public void handleException(UnsupportedMetadataType exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }
    /* Exceptions.UnsupportedType 	400 	The information presented appears to be unsupported. This error might be encountered when attempting to register unrecognized science metadata for e */

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnsupportedType.class)
    public void handleException(UnsupportedType exception, HttpServletRequest request, HttpServletResponse response) {
        handleBaseException((BaseException) exception, request, response);
    }

    /* UnsupportedOperationException 	501 	A method is not implemented, or alternatively, features of a particular method are not implemented. */
    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(UnsupportedOperationException.class)
    public void handleException(UnsupportedOperationException exception, HttpServletRequest request, HttpServletResponse response) {
        NotImplemented notImplemented = new NotImplemented("000", exception.getMessage());
        handleBaseException((BaseException) notImplemented, request, response);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public void handleException(IOException exception, HttpServletRequest request, HttpServletResponse response) {
        ServiceFailure serviceFailure = new ServiceFailure("000", exception.getMessage());
        handleBaseException((BaseException) serviceFailure, request, response);
    }
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(MarshallingException.class)
    public void handleException(MarshallingException exception, HttpServletRequest request, HttpServletResponse response) {
        ServiceFailure serviceFailure = new ServiceFailure("000", exception.getMessage());
        handleBaseException((BaseException) serviceFailure, request, response);
    }
    
    public void handleBaseException(BaseException exception, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(exception.getCode());
        if (request.getHeader("Accept") != null && request.getHeader("Accept").equalsIgnoreCase("text/xml")) {
            try {
                response.getOutputStream().write(exception.serialize(BaseException.FMT_XML).getBytes());
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        } else if (request.getHeader("Accept") != null && request.getHeader("Accept").equalsIgnoreCase("application/json")) {
            try {
                response.getOutputStream().write(exception.serialize(BaseException.FMT_JSON).getBytes());
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        } else if (request.getHeader("Accept") != null && request.getHeader("Accept").equalsIgnoreCase("text/html")) {
            try {
                response.getOutputStream().write(exception.serialize(BaseException.FMT_HTML).getBytes());
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        } else {
            try {
                response.getOutputStream().write(exception.serialize(BaseException.FMT_XML).getBytes());
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    
    protected void debugRequest(HttpServletRequest request) {
        /*     see values with just a plain old request object being sent through */
        logger.debug("request RequestURL: " + request.getRequestURL());
        logger.debug("request RequestURI: " + request.getRequestURI());
        logger.debug("request PathInfo: " + request.getPathInfo());
        logger.debug("request PathTranslated: " + request.getPathTranslated());
        logger.debug("request QueryString: " + request.getQueryString());
        logger.debug("request ContextPath: " + request.getContextPath());
        logger.debug("request ServletPath: " + request.getServletPath());
        logger.debug("request toString: " + request.toString());
        logger.debug("request Method: " + request.getMethod());
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            logger.debug("request " + attributeName + ": " + request.getAttribute(attributeName));
        }
        /*      values of proxyServletWrapper request object to be sent through */
        logger.debug("");


        /*      uncomment to see what the parameters of servlet passed in are  */
        Map<String, String[]> parameterMap = request.getParameterMap();

        for (Object key : parameterMap.keySet()) {
            String[] values = parameterMap.get((String) key);
            for (int i = 0; i < values.length; ++i) {
                logger.debug("request ParameterMap: " + (String) key + " = " + values[i]);
            }

        }
        logger.debug("");

    }
    
    /* (non-Javadoc)
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;

    }
    /* (non-Javadoc)
     * @see org.springframework.web.context.ServletContextAware#getServletContext()
     */
    public ServletContext getServletContext(ServletContext servletContext) {
        return this.servletContext;

    }
}
