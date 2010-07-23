package org.dataone.cn.rest.filter;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;


public class ResolveFilter implements Filter {
	private FilterConfig filterConfig = null;
	private HashMap<String,String> xsltFileNameMap;
	private Enumeration<String> requiredAccepts = (
			'application/json',
			'text/xml',
//			'application/rdf+xml',
			'tex/csv',
			'text/html');
	
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        xsltFileNameMap = new HashMap<String,String>();
        
        // init will register all of the transformation mappings from
        // the deployment descriptor (currently found in the WEB-INF/web.xml file) 
        // and check for required mappings

        // the assumption is that all init-params are transformation mappings
        // the reason for doing this is to be able to add new mappings 
        // without having to copy and paste if-then statements for each
        // new case.

        Enumeration initParamNames = filterConfig.getParameterNames();
        while (initParamNames.hasMoreElements()) {
           	String acceptType = initParamNames.nextElement();
           	
           	String path = filterConfig.getInitParameter(acceptType);
           	String realPath = filterConfig.getServletContext().getRealPath(path);
        	if (!new File(realPath).exists()) {
        		throw new UnavailableException("Unable to locate stylesheet: " + realPath, 30);
        	}
        	xsltFileNameMap.put(accept, realPath);
        }
        
        // test that required accept types are handled (have stylesheets)
        while (requiredAccepts.hasMoreElements()) {
        	String anAccept =requiredAccepts.nextElement();
        	if (xsltFileNameMap.get(anAccept) == null) {
        		throw new UnavailableException(		
                    "Require an xslt mapping for accept type '" +  anAccept
                    + "' Please check the veracity of the init-params "
                    + "in the deployment descriptor.");
        	}
        }
        
 /*       
        // xsltPath should be something like "/WEB-INF/xslt/a.xslt"
        String xsltCsvPath = filterConfig.getInitParameter("xsltCsvPath");
        if (xsltCsvPath == null) {
            throw new UnavailableException(
                    "xsltCsvPath is a required parameter. Please "
                    + "check the deployment descriptor.");
        }
        String xsltJsonPath = filterConfig.getInitParameter("xsltJsonPath");
        if (xsltJsonPath == null) {
            throw new UnavailableException(
                    "xsltJsonPath is a required parameter. Please "
                    + "check the deployment descriptor.");
        }
        String xsltXmlPath = filterConfig.getInitParameter("xsltXmlPath");
        if (xsltXmlPath == null) {
            throw new UnavailableException(
                    "xsltXmlPath is a required parameter. Please "
                    + "check the deployment descriptor.");
        }
        
        
        // convert the context-relative path to a physical path name
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        if (xsltFileNameMap.get("contentType") == null ||
                !new File(xsltFileNameMap.get("contentType")).exists( )) {
            throw new UnavailableException(
                    "Unable to locate stylesheet: " + xsltXmlPath, 30);
        }
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        // see above examples to fill in below calls
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        xsltFileNameMap.put("contentType", filterConfig.getServletContext( ).getRealPath(xsltXmlPath));
        // verify that the file exists
*/
    }

	public void destroy() {
		this.filterConfig = null;
	}
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		
        if (!(res instanceof HttpServletResponse) || !(req instanceof HttpServletRequest)) {
            throw new ServletException("This filter only supports HTTP");
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (filterConfig == null) return;
	

        //  ****** Handle request before passing control to next filter or servlet  *********
        
        String contentType;
        String styleSheet;

        // make sure we can return the requested contentType
        String type = request.getHeader("Accept");
        
        if (xsltFileNameMap.containsKey(type)) {
            styleSheet = xsltFileNameMap.get(type);
        } else {
            throw new Exceptions.NotImplemented(
            		"Unable to provide a response for the Accept header media type of " + type, 406);
        }

        //  ******* pass control to next filter in the chain  ********
        
        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);
        
        chain.doFilter(request, responseWrapper);

        // Tomcat 4.0 reuses instances of its HttpServletResponse
        // implementation class in some scenarios. For instance, hitting
        // reload( ) repeatedly on a web browser will cause this to happen.
        // Unfortunately, when this occurs, output is never written to the
        // BufferedHttpResponseWrapper's OutputStream. This means that the
        // XML output array is empty when this happens. The following
        // code is a workaround:
        
        byte[] origXML = responseWrapper.getBuffer( );
        if (origXML == null || origXML.length == 0) {
            // just let Tomcat deliver its cached data back to the client
            chain.doFilter(request, response);
            return;
        }

        //  ****** Handle response from the servlet  *********
        

        Source styleSource = new StreamSource(styleSheet);	

        try {

        	TransformerFactory tf = TransformerFactory.newInstance();
        	Transformer transformer = tf.newTransformer(styleSource);

            ByteArrayInputStream origXMLIn = new ByteArrayInputStream(origXML);
            Source xmlSource = new StreamSource(origXMLIn);

            ByteArrayOutputStream resultBuf = new ByteArrayOutputStream( );
            transformer.transform(xmlSource, new StreamResult(resultBuf));

            response.setContentLength(resultBuf.size( ));
            response.setContentType(type);
            response.getOutputStream().write(resultBuf.toByteArray( ));
            response.flushBuffer( );

        } catch (TransformerException te) {
            throw new ServletException(te);
        }
    }


}
