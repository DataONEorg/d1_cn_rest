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

package org.dataone.cn.rest;

//import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.core.io.ClassPathResource;

public class ResolveServlet extends HttpServlet {

    /**
     * For unit testing, build a 2 member filter chain consisting of ResolveFilter
     * and this servlet (the endpoint).  This replaces the urlrewrite to metacat chain
     * that comprise the inner layers of the calling chain.
     * Returns a systemMetadata xml file.
     */
    private static final long serialVersionUID = 1L;
    private BufferedReader br;
    private InputStream is;
    private FileInputStream byteInput;
    static final int SIZE = 8192;

    public ResolveServlet (ClassPathResource xmlResource) throws IOException {
        super();
        is = xmlResource.getInputStream();
    }
    public void setOutput(String file) throws IOException {
        ClassPathResource resource = new ClassPathResource(file);
        this.is = resource.getInputStream();
    }

    public void doHead(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (this.is == null) {
            throw new ServletException("output not set.  call setOutput(file) before doPost( )");
        }

        res.setContentType("text/xml");
        res.setCharacterEncoding("UTF-8");

        ServletOutputStream out = res.getOutputStream();
        ArrayList<Byte> byteArray = new ArrayList<Byte>();
        StringBuffer errorBuffer = new StringBuffer();
        try {
            BufferedInputStream f = new BufferedInputStream(is);
            byte[] barray = new byte[SIZE];
            int nRead;
            while ((nRead = f.read(barray, 0, SIZE)) != -1) {
                errorBuffer.append(new String(Arrays.copyOf(barray, nRead)));
                out.write(barray, 0, nRead);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        if (errorBuffer.toString().contains("<error")) {
            res.setStatus(res.SC_NOT_FOUND);
        } else {
            res.setStatus(res.SC_OK);
        }
    }
}
