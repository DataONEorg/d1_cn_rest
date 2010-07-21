package org.dataone.cn.rest.filter;

import javax.servlet.Filter;

public class resolveFilter implements Filter {
	private FilterConfig filterConfig = null;
	
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;	
	}
	
	public void destroy() {
		this.filterConfig = null;
	}
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	throws IOException, ServletException {
	
	if (filterConfig == null) return;
	
	// not ready for consumption yet...
	// (consider this a placeholder)
	
	String contentType;
	String styleSheet;
	String type = request.getParameter("type");
	if (type == null || type.equals("")) {
	    contentType = "xml";
	    styleSheet = "/xml/resolve-filter-xml.xsl";
	} else {
	    if (type.equals("csv")) {
	    	contentType = "text/plain";	
	    	styleSheet = "/xml/resolve-filter-csv.xsl";
	    } else {
	    	if (type.equals("json")) {
	    		contentType = "text/plain";
	    		styleSheet = "/xml/resolve-filter-json.xsl";
	    	} else {
	    		contentType = "xml";
	    		styleSheet = "/xml/resolve-filter-xml.xsl";
	    	}
	    }
	}
	response.setContectType(contentType);
	String stylepath=filterConfig.getServletContext().getRealPath(StyleSheet);
	Source styleSource = new StreamSource(stylePath);
	
	PrintWriter out = response.getWriter();
	CharResponseWrapper = responseWrapper = new CharResponseWrapper((HttpServletResponse)response);
	chain.doFilter(request, wrapper);
	// Get response from servlet
	StringReader sr = new StringReader(new String(wrapper.getData()));
	Source xmlSource = new StreamSource((Reader)sr);
	
	try {
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer(styleSource);
	    CharArrayWriter caw = new CharArrayWriter();
	    StreamResult res = new StreamResult(caw);
	    transformer.transform(xmlSource, res);
	    response.setContentLength(caw.toString().length());
	    out.write(caw.toString());
	} catch(Exception ex) {
	    out.println(ex.toString());
	    out.write(Wrapper.toString());
	}
}
