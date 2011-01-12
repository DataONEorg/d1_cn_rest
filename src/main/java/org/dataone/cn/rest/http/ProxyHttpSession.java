/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.http;

/**
 * Ripped off and slightly modified version of MockHttpSession from wicket- http://wicket.apache.org/
 * 
 * @author waltz
 */

/*
 * $Id: MockHttpSession.java 458548 2006-01-08 23:26:07Z jonl $
 * $Revision: 458548 $
 * $Date: 2006-01-09 00:26:07 +0100 (Mon, 09 Jan 2006) $
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
import java.io.Serializable;
import java.rmi.server.UID;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * Mock implementation of the <code>WebSession</code> interface for use by
 * the test harnesses.
 *
 * @author Chris Turner
 */
public class ProxyHttpSession implements HttpSession, Serializable {

    private static final long serialVersionUID = 1L;
    private final HashMap attributes = new HashMap();
    private final ServletContext context;
    private final long creationTime = System.currentTimeMillis();
    private final String id = (new UID()).toString();
    private long lastAccessedTime = 0;

    /**
     * Create the session.
     *
     * @param context
     */
    public ProxyHttpSession(final ServletContext context) {
        this.context = context;
    }

    /**
     * Get the attribute with the given name.
     *
     * @param name
     *            The attribute name
     * @return The value or null
     */
    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    /**
     * Get the names of the attributes in the session.
     *
     * @return The attribute names
     */
    @Override
    public Enumeration getAttributeNames() {
        return Collections.enumeration(attributes.keySet());

    }

    /**
     * Get the creation time of the session.
     *
     * @return The creation time
     */
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Return the id of this session.
     *
     * @return The id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get the time the session was last accessed.
     *
     * @return The last accessed time
     */
    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * NOT USED. Sessions never expire in the test harness.
     *
     * @return Always returns 0
     */
    @Override
    public int getMaxInactiveInterval() {
        return 0;
    }

    /**
     * Return the servlet context for the session.
     *
     * @return The servlet context
     */
    @Override
    public ServletContext getServletContext() {
        return context;
    }

    /**
     * NOT USED.
     *
     * @return Always null
     * @deprecated
     */
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    }

    /**
     * Get the value for the given name.
     *
     * @param name
     *            The name
     * @return The value or null
     * @deprecated use getAttribute(String) instead
     */
    @Override
    public Object getValue(final String name) {
        return getAttribute(name);
    }

    /**
     * Get the names of the values in the session.
     *
     * @return The names of the attributes
     * @deprecated use getAttributeNames() instead
     */
    @Override
    public String[] getValueNames() {
        String[] result = new String[attributes.size()];
        return (String[]) attributes.keySet().toArray(result);
    }

    /**
     * Invalidate the session.
     */
    @Override
    public void invalidate() {
        attributes.clear();
    }

    /**
     * Check if the session is new.
     *
     * @return Always false
     */
    @Override
    public boolean isNew() {
        return false;
    }

    /**
     * Set a value.
     *
     * @param name
     *            The name of the value
     * @param o
     *            The value
     * @deprecated Use setAttribute(String, Object) instead
     */
    @Override
    public void putValue(final String name, final Object o) {
        setAttribute(name, o);
    }

    /**
     * Remove an attribute.
     *
     * @param name
     *            The name of the attribute
     */
    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    /**
     * Remove a value.
     *
     * @param name
     *            The name of the value
     * @deprecated Use removeAttribute(String) instead
     */
    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * Set an attribute.
     *
     * @param name
     *            The name of the attribute to set
     * @param o
     *            The value to set
     */
    @Override
    public void setAttribute(final String name, final Object o) {
        attributes.put(name, o);
    }

    /**
     * NOT USED. Sessions never expire in the test harness.
     *
     * @param i
     *            The value
     */
    @Override
    public void setMaxInactiveInterval(final int i) {
    }

    /**
     * Set the last accessed time for the session.
     */
    public void timestamp() {
        lastAccessedTime = System.currentTimeMillis();
    }
}
