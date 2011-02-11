/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.dataone.service.exceptions.ServiceFailure;

/**
 *
 * @author waltz
 */
public class RecodePathFilterRequest extends HttpServletRequestWrapper {

    private HttpServletRequest request = null;
    private String _pathInfo = "";
    private URLCodec urlDecoder = new URLCodec();
    private Charset utf8 = Charset.forName("UTF-8");

    /**
     * expecting the pathInfo to contain /<method>[/*]
     * and requestURI to contain /cn/metacat/<method>[/*]
     *                        or /cn/<method>[/*]
     * @param request
     * @throws DecoderException
     * @throws ServiceFailure
     */
    public RecodePathFilterRequest(HttpServletRequest request) throws EncoderException, ServiceFailure {
        super(request);

        String pathInfo = super.getPathInfo();
        System.out.println("original pathInfo: " + pathInfo);

        String reqUri = this.getRequestURI();
        System.out.println("original requestURI: " + reqUri);
        System.out.println("original ContextPath: " + this.getContextPath());
        System.out.println("original ServletPath: " + this.getServletPath());
        System.out.println(this.getPathTranslated());
        System.out.println(this.getRequestURL());

        try {
            this._pathInfo = "/" + urlDecoder.encode(pathInfo.substring(1), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            this._pathInfo = "/" + urlDecoder.encode(pathInfo.substring(1));
        }


        System.out.println("new pathinfo: " + this._pathInfo);

    }

    @Override
    public String getPathInfo() {
        System.out.println("diagnostic getPathInfo: " + super.getPathInfo());
        System.out.println("diagnostic ContextPath: " + this.getContextPath());
        System.out.println("diagnostic ServletPath: " + this.getServletPath());

        return this._pathInfo;
    }
}
