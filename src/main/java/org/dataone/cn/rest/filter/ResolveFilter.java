package org.dataone.cn.rest.filter;

import java.io.*;
import java.util.HashMap;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;


public class ResolveFilter implements Filter {
	private FilterConfig filterConfig = null;
	private HashMap<String,String> xsltFileNameMap;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        xsltFileNameMap = new HashMap<String,String>();
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
	
	// not ready for consumption yet...
	// (consider this a placeholder)
	
	String contentType;
	String styleSheet;

	String type = request.getHeader("Accept");
        
        if (xsltFileNameMap.containsKey(type)) {
            styleSheet = xsltFileNameMap.get(type);
        } else {
            throw new UnavailableException(
                    "Unable to locate stylesheet for Accept header media type of " + type, 30);
        }

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



//	String stylepath=filterConfig.getServletContext().getRealPath(styleSheet);
	Source styleSource = new StreamSource(styleSheet);
/*
	PrintWriter out = response.getWriter();
	CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse)response);
	chain.doFilter(request, responseWrapper);
	// Get response from servlet
	StringReader sr = new StringReader(new String(responseWrapper.getData()));
	Source xmlSource = new StreamSource((Reader)sr);
	*/
	try {

	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer(styleSource);

            ByteArrayInputStream origXMLIn = new ByteArrayInputStream(origXML);
            Source xmlSource = new StreamSource(origXMLIn);

//	    transformer.transform(xmlSource, response);
  //          ByteArrayInputStream origXMLIn = new ByteArrayInputStream(origXML);
  //          Source xmlSource = new StreamSource(origXMLIn);

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
