/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.http;

/**
 * Ripped off and slightly modified version of MockHttpServletRequest from wicket- http://wicket.apache.org/
 * 
 * @author waltz
 */

/*
 * $Id: MockHttpServletRequest.java 462122 2006-09-09 12:40:00Z frankbille $
 * $Revision: 462122 $
 * $Date: 2006-09-09 14:40:00 +0200 (Sat, 09 Sep 2006) $
 *
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mock servlet request. Implements all of the methods from the standard
 * HttpServletRequest class plus helper methods to aid setting up a request.
 *
 * @author Chris Turner
 */
public class ProxyHttpServletRequest implements HttpServletRequest {

    /**
     * A holder class for an uploaded file.
     *
     * @author Frank Bille (billen)
     */
    /** Logging object */
    private static final Log log = LogFactory.getLog(ProxyHttpServletRequest.class);
    /** The application */
    private HashMap attributes = null;
    private String authType;
    private String characterEncoding;
    private ServletContext context;
    private List cookies = null;
    private HashMap<String, List> headers = null;
    private String method;
    private HashMap<String, String> parameters = null;
    private String path;
    private HttpSession session;

    /**
     * Create the request using the supplied session object.
     *
     * @param application
     *            The application that this request is for
     * @param session
     *            The session object
     * @param context
     *            The current servlet context
     */
    public ProxyHttpServletRequest(
            HttpSession session, ServletContext context) {
        this.session = session;
        this.context = context;
        this.initialize();
    }

    /**
     * Add a new cookie.
     *
     * @param cookie
     *            The cookie
     */
    public void addCookie(final Cookie cookie) {
        cookies.add(cookie);
    }

    /**
     * Add a header to the request.
     *
     * @param name
     *            The name of the header to add
     * @param value
     *            The value
     */
    public void addHeader(String name, String value) {
        List list = (List) headers.get(name);
        if (list == null) {
            list = new ArrayList(1);
            headers.put(name, list);
        }
        list.add(value);
    }

    /**
     * Get an attribute.
     *
     * @param name
     *            The attribute name
     * @return The value, or null
     */
    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    /**
     * Get the names of all of the values.
     *
     * @return The names
     */
    @Override
    public Enumeration getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    // HttpServletRequest methods
    /**
     * Get the auth type.
     *
     * @return The auth type
     */
    @Override
    public String getAuthType() {
        return authType;
    }

    /**
     * Get the current character encoding.
     *
     * @return The character encoding
     */
    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * Return the length of the content. This is always -1 except if there has
     * been added uploaded files. Then the length will be the length of the
     * generated request.
     *
     * @return -1 if no uploaded files has been added. Else the length of the
     *         generated request.
     */
    @Override
    public int getContentLength() {
        return -1;
    }

    /**
     * return the correct content-type.
     * XXX
     */
    @Override
    public String getContentType() {
        return null;
    }

    /**
     * Get the context path. For this mock implementation the name of the
     * application is always returned.
     *
     * @return The context path
     */
    @Override
    public String getContextPath() {

        return context.getContextPath();
    }

    /**
     * Get all of the cookies for this request.
     *
     * @return The cookies
     */
    @Override
    public Cookie[] getCookies() {
        if (cookies.isEmpty()) {
            return null;
        }
        Cookie[] result = new Cookie[cookies.size()];
        return (Cookie[]) cookies.toArray(result);
    }

    /**
     * Get the given header as a date.
     *
     * @param name
     *            The header name
     * @return The date, or -1 if header not found
     * @throws IllegalArgumentException
     *             If the header cannot be converted
     */
    @Override
    public long getDateHeader(final String name)
            throws IllegalArgumentException {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }

        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        try {
            return df.parse(value).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Can't convert header to date " + name + ": "
                    + value);
        }
    }

    /**
     * Get the given header value.
     *
     * @param name
     *            The header name
     * @return The header value or null
     */
    @Override
    public String getHeader(final String name) {
        final List l = (List) headers.get(name);
        if (l == null || l.size() < 1) {
            return null;
        } else {
            return (String) l.get(0);
        }
    }

    /**
     * Get the names of all of the headers.
     *
     * @return The header names
     */
    @Override
    public Enumeration getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    /**
     * Get enumeration of all header values with the given name.
     *
     * @param name
     *            The name
     * @return The header values
     */
    @Override
    public Enumeration getHeaders(final String name) {
        List list = (List) headers.get(name);
        if (list == null) {
            list = new ArrayList();
        }
        return Collections.enumeration(list);
    }

    /**
     * Returns an input stream if there has been added some uploaded files. Use
     * {@link #addFile(String, File, String)} to add some uploaded files.
     *
     * @return The input stream
     * @throws IOException
     *             If an I/O related problem occurs
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {

        return new ServletInputStream() {

            @Override
            public int read() {
                return -1;
            }
        };
    }

    /**
     * Get the given header as an int.
     *
     * @param name
     *            The header name
     * @return The header value or -1 if header not found
     * @throws NumberFormatException
     *             If the header is not formatted correctly
     */
    @Override
    public int getIntHeader(final String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        return Integer.valueOf(value).intValue();
    }

    /**
     * Get the locale of the request. Attempts to decode the Accept-Language
     * header and if not found returns the default locale of the JVM.
     *
     * @return The locale
     */
    @Override
    public Locale getLocale() {
        final String header = getHeader("Accept-Language");
        if (header == null) {
            return Locale.getDefault();
        }

        final String[] firstLocale = header.split(",");
        if (firstLocale.length < 1) {
            return Locale.getDefault();
        }

        final String[] bits = firstLocale[0].split("-");
        if (bits.length < 1) {
            return Locale.getDefault();
        }

        final String language = bits[0].toLowerCase();
        if (bits.length > 1) {
            final String country = bits[1].toUpperCase();
            return new Locale(language, country);
        } else {
            return new Locale(language);
        }
    }

    /**
     * Return all the accepted locales. This implementation always returns just
     * one.
     *
     * @return The locales
     */
    @Override
    public Enumeration getLocales() {
        List list = new ArrayList(1);
        list.add(getLocale());
        return Collections.enumeration(list);
    }

    /**
     * Get the method.
     *
     * @return The method
     */
    @Override
    public String getMethod() {
        return method;
    }

    /**
     * Get the request parameter with the given name.
     *
     * @param name
     *            The parameter name
     * @return The parameter value, or null
     */
    @Override
    public String getParameter(final String name) {
        return parameters.get(name);
    }

    /**
     * Get the map of all of the parameters.
     *
     * @return The parameters
     */
    @Override
    public Map getParameterMap() {
        return parameters;
    }

    /**
     * Get the names of all of the parameters.
     *
     * @return The parameter names
     */
    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    /**
     * Get the values for the given parameter.
     *
     * @param name
     *            The name of the parameter
     * @return The return values
     */
    @Override
    public String[] getParameterValues(final String name) {
        Object value = parameters.get(name);
        if (value == null) {
            return new String[0];
        }

        if (value instanceof String[]) {
            return (String[]) value;
        } else {
            String[] result = new String[1];
            result[0] = value.toString();
            return result;
        }
    }

    /**
     * Get the path info.
     *
     * @return The path info
     */
    @Override
    public String getPathInfo() {
        return path;
    }

    /**
     * Always returns null.
     *
     * @return null
     */
    @Override
    public String getPathTranslated() {
        return null;
    }

    /**
     * Get the protocol.
     *
     * @return Always HTTP/1.1
     */
    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    /**
     * Get the query string part of the request.
     *
     * @return The query string
     */
    @Override
    public String getQueryString() {
        if (parameters.isEmpty()) {
            return null;
        } else {
            final StringBuffer buf = new StringBuffer();
            try {
                for (Iterator iterator = parameters.keySet().iterator(); iterator.hasNext();) {
                    final String name = (String) iterator.next();
                    final String value = parameters.get(name);
                    buf.append(URLEncoder.encode(name, "UTF-8"));
                    buf.append('=');
                    buf.append(URLEncoder.encode(value, "UTF-8"));
                    if (iterator.hasNext()) {
                        buf.append('&');
                    }
                }
            } catch (UnsupportedEncodingException e) {
                // Should never happen!
            }
            return buf.toString();
        }
    }

    /**
     * This feature is not implemented at this time as we are not supporting
     * binary servlet input. This functionality may be added in the future.
     *
     * @return The reader
     * @throws IOException
     *             If an I/O related problem occurs
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new CharArrayReader(new char[0]));
    }

    /**
     * Deprecated method - should not be used.
     *
     * @param name
     *            The name
     * @return The path
     * @deprecated Use ServletContext.getRealPath(String) instead.
     */
    @Override
    public String getRealPath(String name) {
        return context.getRealPath(name);
    }

    /**
     * Get the remote address of the client.
     *
     * @return Always 127.0.0.1
     */
    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    /**
     * Get the remote host.
     *
     * @return Always localhost
     */
    @Override
    public String getRemoteHost() {
        return "localhost";
    }

    /**
     * Get the name of the remote user from the REMOTE_USER header.
     *
     * @return The name of the remote user
     */
    @Override
    public String getRemoteUser() {
        return getHeader("REMOTE_USER");
    }

    /**
     * Return a dummy dispatcher that just records that dispatch has occured
     * without actually doing anything.
     *
     * @param name
     *            The name to dispatch to
     * @return The dispatcher
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String name) {
        return context.getRequestDispatcher(name);
    }

    /**
     * Get the requested session id. Always returns the id of the current
     * session.
     *
     * @return The session id
     */
    @Override
    public String getRequestedSessionId() {
        return session.getId();
    }

    /**
     * Get the request url. Always return the path value.
     *
     * @return The oath value
     */
    @Override
    public String getRequestURI() {
        return path;
    }

    /**
     * Try to build a rough URL.
     *
     * @return The url
     */
    @Override
    public StringBuffer getRequestURL() {
        final StringBuffer buf = new StringBuffer();
        buf.append("http://localhost");
        buf.append(getContextPath());
        if (getPathInfo() != null) {
            buf.append(getPathInfo());
        }

        final String query = getQueryString();
        if (query != null) {
            buf.append('?');
            buf.append(query);
        }
        return buf;
    }

    /**
     * Get the scheme.
     *
     * @return Always http
     */
    @Override
    public String getScheme() {
        return "http";
    }

    /**
     * Get the server name.
     *
     * @return Always localhost
     */
    @Override
    public String getServerName() {
        return "localhost";
    }

    /**
     * Get the server port.
     *
     * @return Always 80
     */
    @Override
    public int getServerPort() {
        return 80;
    }

    /**
     * The servlet path may either be the application name or /. For test
     * purposes we always return the servlet name.
     *
     * @return The servlet path
     */
    @Override
    public String getServletPath() {
        return getContextPath();
    }

    /**
     * Get the sessions.
     *
     * @return The session
     */
    @Override
    public HttpSession getSession() {
        return session;
    }

    /**
     * Get the session.
     *
     * @param b
     *            Ignored, there is always a session
     * @return The session
     */
    @Override
    public HttpSession getSession(boolean b) {
        return session;
    }

    /**
     * Get the user principal.
     *
     * @return A user principal
     */
    @Override
    public Principal getUserPrincipal() {
        final String user = getRemoteUser();
        if (user == null) {
            return null;
        } else {
            return new Principal() {

                @Override
                public String getName() {
                    return user;
                }
            };
        }
    }

    /**
     * Reset the request back to a default state.
     */
    private void initialize() {
        this.attributes = new HashMap();
        this.cookies = new ArrayList();
        this.headers = new HashMap<String, List>();
        this.parameters = new HashMap<String, String>();
        authType = null;
        method = "get";
        setDefaultHeaders();
        path = null;
        characterEncoding = "UTF-8";
    }

    /**
     * Check whether session id is from a cookie. Always returns true.
     *
     * @return Always true
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return true;
    }

    /**
     * Check whether session id is from a url rewrite. Always returns false.
     *
     * @return Always false
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    /**
     * Check whether session id is from a url rewrite. Always returns false.
     *
     * @return Always false
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    /**
     * Check whether the session id is valid.
     *
     * @return Always true
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        return true;
    }

    /**
     * Always returns false.
     *
     * @return Always false
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * NOT IMPLEMENTED.
     *
     * @param name
     *            The role name
     * @return Always false
     */
    @Override
    public boolean isUserInRole(String name) {
        return false;
    }

    /**
     * Remove the given attribute.
     *
     * @param name
     *            The name of the attribute
     */
    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    /**
     * Set the given attribute.
     *
     * @param name
     *            The attribute name
     * @param o
     *            The value to set
     */
    @Override
    public void setAttribute(final String name, final Object o) {
        attributes.put(name, o);
    }

    /**
     * Set the auth type.
     *
     * @param authType
     *            The auth type
     */
    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    /**
     * Set the character encoding.
     *
     * @param encoding
     *            The character encoding
     * @throws UnsupportedEncodingException
     *             If encoding not supported
     */
    @Override
    public void setCharacterEncoding(final String encoding)
            throws UnsupportedEncodingException {
        this.characterEncoding = encoding;
    }

    /**
     * Set the cookies.
     *
     * @param theCookies
     *            The cookies
     */
    public void setCookies(final Cookie[] theCookies) {
        cookies.clear();
        cookies.addAll(Arrays.asList(theCookies));
    }

    /**
     * Set the method.
     *
     * @param method
     *            The method
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * Set a parameter.
     *
     * @param name
     *            The name
     * @param value
     *            The value
     */
    public void setParameter(final String name, final String value) {
        parameters.put(name, value);
    }

    /**
     * Sets a map of parameters.
     *
     * @param parameters
     *            the parameters to set
     */
    public void setParameters(final Map parameters) {
        this.parameters.putAll(parameters);
    }

    /**
     * Set the path that this request is supposed to be serving. The path is
     * relative to the web application root and should start with a / charater
     *
     * @param path
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Set the complete url for this request.
     * The url will be analized.
     *
     * @param url
     */
    public void setURL(String url) {
        if (url.startsWith("http://")) {
            int index = url.indexOf("/", 7);
            url = url.substring(index);
        }
        if (url.startsWith(getContextPath())) {
            url = url.substring(getContextPath().length());
        }
        if (url.startsWith(getServletPath())) {
            url = url.substring(getServletPath().length());
        }

        int index = url.indexOf("?");
        if (index == -1) {
            path = url;
        } else {
            path = url.substring(0, index);

            String queryString = url.substring(index + 1);
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int tmp = token.indexOf("=");
                if (tmp != -1) {
                    setParameter(token.substring(0, tmp), token.substring(tmp + 1));
                }
            }

        }
    }

    /**
     * Initialise the request parameters from the given redirect string that
     * redirects back to a particular component for display.
     *
     * @param redirect
     *            The redirect string to display from
     */
    public void setRequestToRedirectString(final String redirect) {
        parameters.clear();

        final String paramPart = redirect.substring(redirect.indexOf('?') + 1);
        final String[] paramTuples = paramPart.split("&");
        for (int t = 0; t < paramTuples.length; t++) {
            final String[] bits = paramTuples[t].split("=");
            if (bits.length == 2) {
                try {
                    parameters.put(URLDecoder.decode(bits[0], "UTF-8"),
                            URLDecoder.decode(bits[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Should never happen
                }
            }
        }
    }

    /**
     * Helper method to create some default headers for the request
     */
    private void setDefaultHeaders() {
        addHeader(
                "Accept",
                "text/xml");
        addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        Locale l = Locale.getDefault();
        addHeader("Accept-Language", l.getLanguage().toLowerCase()
                + "-" + l.getCountry().toLowerCase() + ","
                + l.getLanguage().toLowerCase() + ";q=0.5");
    }

    @Override
    public int getRemotePort() {
        return 80;
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        return 80;
    }
}
