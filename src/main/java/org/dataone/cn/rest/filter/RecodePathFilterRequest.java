/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.dataone.service.exceptions.ServiceFailure;

/**
 *
 * @author waltz
 */
public class RecodePathFilterRequest extends HttpServletRequestWrapper
{

   private HttpServletRequest request = null;
   private String pathInfo = null;

   
   /**
    * expecting the pathInfo to contain /<method>[/*] 
    * and requestURI to contain /cn/metacat/<method>[/*]
    *                        or /cn/<method>[/*]
    * @param request
    * @throws DecoderException
 * @throws ServiceFailure 
    */
   public RecodePathFilterRequest(HttpServletRequest request) throws DecoderException, ServiceFailure
   {
      super(request);
      URLCodec codec = new URLCodec();
      this.request = request;
       String uri = this.getRequestURI();
       System.out.println("RecodePathFilter: RequestURI = " + uri);
       int start = -1;
       if (uri.startsWith("/cn/metacat/"))
    	   start = 11;
       else if (uri.startsWith("/cn/"))
    	   start = 3;
       else
    	   throw new ServiceFailure("4150", "unexpected requestURI start: " + uri);
       
       this.pathInfo = uri.substring(start);
       System.out.println("RecodePathFilter: new pathInfo = " + this.pathInfo);
       this.pathInfo = codec.decode(this.pathInfo);
       System.out.println("RecodePathFilter: decoded pathInfo = " + this.pathInfo);
   }

   @Override
   public String getPathInfo() {

       return this.pathInfo;
   }
}
