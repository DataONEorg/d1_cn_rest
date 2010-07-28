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
	private String[] requiredAccepts = {
			"application/json",
//			"text/html",
//			"application/rdf+xml",
			"text/csv",
			"text/xml"
			};
	
    @SuppressWarnings("unchecked")
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

        Enumeration<String> initParamNames = filterConfig.getInitParameterNames();
        while (initParamNames.hasMoreElements()) {
           	String acceptType = (String) initParamNames.nextElement();
           	
           	String path = filterConfig.getInitParameter(acceptType);
//           	String mybase = filterConfig.getServletContext().getResource(path);
           	String realPath = filterConfig.getServletContext().getRealPath(path);
        	if (!new File(realPath).exists()) {
        		throw new UnavailableException("Unable to locate stylesheet: " + realPath, 30);
        	}
        	xsltFileNameMap.put(acceptType, realPath);
        }
        
        // test that required accept types are handled (have stylesheets)
        for (int i=0; i< requiredAccepts.length; i++) {
        	if (xsltFileNameMap.get(requiredAccepts[i]) == null) {
        		throw new UnavailableException(		
                    "Require an xslt mapping for accept type '" +  requiredAccepts[i]
                    + "' Please check the veracity of the init-params "
                    + "in the deployment descriptor.");
        	}
        }
    }

	public void destroy() {
		this.filterConfig = null;
	}
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        if (filterConfig == null) return;
		
		if (!(res instanceof HttpServletResponse) || !(req instanceof HttpServletRequest)) {
            throw new ServletException("This filter only supports HTTP");
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;


        //  ****** Handle request before passing control to next filter or servlet  *********
        						   
        String styleSheet;

        // make sure we can return the requested contentType
        String type = request.getHeader("Accept");
        
        if (xsltFileNameMap.containsKey(type)) {
            styleSheet = xsltFileNameMap.get(type);
        } else {
            throw new UnavailableException(
            		"Unable to provide a response for the "
            		+ "Accept header media type of " + type, 406);
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
//       String forDebug = new String(origXML);
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
//            String forDebug2 = transformer.getOutputProperty(OutputKeys.METHOD);
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
