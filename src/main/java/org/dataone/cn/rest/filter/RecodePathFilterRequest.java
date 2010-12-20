/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

/**
 *
 * @author waltz
 */
public class RecodePathFilterRequest extends HttpServletRequestWrapper
{

   private HttpServletRequest request = null;
   private String pathInfo = null;

   public RecodePathFilterRequest(HttpServletRequest request) throws DecoderException
   {
      super(request);
      URLCodec codec = new URLCodec();
      this.request = request;
       String uri = this.getRequestURI();

       int start = uri.indexOf("/"); /* presume this is how to find and ignore the context */
       this.pathInfo = codec.decode(uri.substring(start + 1));
   }

   @Override
   public String getPathInfo() {

       return this.pathInfo;
   }
}
