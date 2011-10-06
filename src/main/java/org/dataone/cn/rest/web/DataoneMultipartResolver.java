/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.web;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileUpload;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 *
 * @author waltz
 */
public class DataoneMultipartResolver extends CommonsMultipartResolver  implements  MultipartResolver, ServletContextAware  {
     @Override
     public boolean isMultipart (
            HttpServletRequest request) {
        if ("post".equals(request.getMethod().toLowerCase()) || "put".equals(request.getMethod().toLowerCase())) {
            String contentType = request.getContentType();
            if (contentType == null) {
                return false;
            }
            if (contentType.toLowerCase().startsWith(FileUpload.MULTIPART)) {
                return true;
            }
        }
        return false;

    }
}
