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

package org.dataone.cn.rest.http;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileUpload;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * CommonsMultipartResolver does not implement isMultipart such that 
 * put can handle multipart content. 
 * 
 * override the isMultipart method to allow put requests to succeed
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
