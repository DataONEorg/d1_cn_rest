package org.dataone.cn.rest.filter;

//import NodeListLookup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.dataone.service.exceptions.*;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public class ResolveFilter implements Filter {
    Logger logger = Logger.getLogger(ResolveFilter.class);
	private FilterConfig filterConfig = null;
    private HashMap<String, String> baseUrlMap = null;
    private Integer nodelistRefreshInterval = 5;
    private boolean useSchemas = true;
    private String nodelistLocation = "/var/lib/dataone/nodeList.xml";
    private String nodelistSchemaLocation = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5/nodelist.xsd";
    private String systemmetadataSchemaLocation = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5/systemmetadata.xsd";
    private static String d1namespaceVersion = "http://dataone.org/service/types/ObjectLocationList/0.5";
    private static String objectlocationlistSchemaURL = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5/objectlocationlist.xsd";
    
    private String targetEnvironment = "prod";
    private XPathFactory xFactory = null;
    
	public void destroy() {
		this.filterConfig = null;
		this.baseUrlMap = null;	
		this.nodelistLocation = null;
		this.xFactory = null;
/*
		this.nodelistRefreshInterval = null;
		this.nodelistSchemaLocation = null;
		this.targetEnvironment = null;
	*/
	}

	/*
	 *    An error handler to raise schema validation errors for xsd schemas
	 */
	class XSDValidationErrorHandler extends DefaultHandler  {

		public void error(SAXParseException e) throws SAXParseException {
			throw e;
		}
	}
	
	
	/* -------------------------------------------
	 * Init's main job in this case is to read and parse the nodelist
	 * into a map for looking up baseURLs by node IDs.
	 * Also some housekeeping parameters related to how long
	 * to cache the nodelist for, and where to find it.
	 * The Nodelist should be relatively stable (the baseURL at least)
	 * but it is important to not to assume too much.
	 * 
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	
	public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("init ResolveFilter");
        this.filterConfig = filterConfig;
        

    // read in parameters to the filter
        if ( filterConfig.getInitParameter("nodelistRefreshInterval") != null )  
        	try {
        		this.nodelistRefreshInterval = Integer.parseInt(filterConfig.getInitParameter("nodelistRefreshInterval"));
        	} catch (NumberFormatException e) {
        		throw new ServletException(e);
        	}
        
        if (filterConfig.getInitParameter("nodelistLocation") != null)
        	this.nodelistLocation = filterConfig.getInitParameter("nodelistLocation");
        
        if (filterConfig.getInitParameter("targetEnvironment") != null)
        	this.targetEnvironment = filterConfig.getInitParameter("targetEnvironment");
        
        if (filterConfig.getInitParameter("useSchemas") != null) {
        	if (filterConfig.getInitParameter("useSchemas") == "false")  this.useSchemas = false;
        	else if (filterConfig.getInitParameter("useSchemas") == "true")  this.useSchemas = true;
        	else throw new ServletException("bad value for input parameter 'useSchemas'");
        }
        if (filterConfig.getInitParameter("nodelistSchemaLocation") != null)
        	this.nodelistSchemaLocation = filterConfig.getInitParameter("nodelistSchemaLocation");
        
        if (filterConfig.getInitParameter("systemmetadataSchemaLocation") != null)
        	this.systemmetadataSchemaLocation = filterConfig.getInitParameter("systemmetadataSchemaLocation");

		this.xFactory = XPathFactory.newInstance();

    }
  
    private void cacheNodeListURLs() throws ServiceFailure { 
		
    	// expire the map if refresh interval is exceeded
    	if (isTimeForRefresh())  this.baseUrlMap = null;
    	
    	// create new map by reading the nodelist file and parsing out the baseURL
    	// TODO: implement logic for excluding nodes in the wrong target environment
		if (this.baseUrlMap == null) {

			// build the XML parser
			Schema schema = createXsdSchema(this.nodelistSchemaLocation,this.useSchemas);
			DocumentBuilder parser = createStdValidatingDOMParser(schema,this.useSchemas);

	        // ----------- parse the nodelist into DOM-style document
	        Document document;
			try {
				document = parser.parse(this.nodelistLocation);
			} catch (SAXException e) {
				throw new ServiceFailure("4150","Cannot parse NodeList");
			} catch (IOException e) {
				e.printStackTrace();
				throw new ServiceFailure("4150","Cannot open NodeList: " + this.nodelistLocation);
			}


			// ---------- compile the XPath expression that selects nodes
	        Object result;
			try {
		        XPath xpath = xFactory.newXPath();
			//	XPathExpression expr = xpath.compile("/node[evironment='" + targetEnvironment + "']");
				XPathExpression expr = xpath.compile("//node");
				result = expr.evaluate(document, XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				throw new ServiceFailure("4150","error compiling the xpath expression for selecting nodes.");
			}
	        org.w3c.dom.NodeList targetNodes = (org.w3c.dom.NodeList) result;
	        
	        this.baseUrlMap = new HashMap<String,String>();	
	        
	        for (int i = 0; i < targetNodes.getLength(); i++) {
	            org.w3c.dom.NodeList d1nodeElements = targetNodes.item(i).getChildNodes(); 
	            String nodeID = null;
	            String baseURL = null;
	            for (int j = 0; j < d1nodeElements.getLength(); j++) {
	            	if (d1nodeElements.item(j).getNodeName() == "identifier") {
	            		nodeID = d1nodeElements.item(j).getTextContent();
	            	}
	            	if (d1nodeElements.item(j).getNodeName() == "baseURL") {
	            		baseURL = d1nodeElements.item(j).getTextContent();
	            	}
	            }
	            if (nodeID == null || nodeID.isEmpty()) {
	            	baseUrlMap = null;
	            	throw new ServiceFailure("4150","Error parsing Nodelist: cannot get nodeID for node " + i + " of " + targetNodes.getLength());
	            } else if (baseURL == null || baseURL.isEmpty()) {
	            	baseUrlMap = null;
	            	throw new ServiceFailure("4150","Error parsing Nodelist: cannot get baseURL for nodeID " + nodeID);
	            } else {
	            	baseUrlMap.put(nodeID, baseURL);
	            }
	        }
	        if (baseUrlMap.isEmpty()) {
	        	throw new ServiceFailure("4150","baseUrlMap is empty. Cannot service the resolve method.");
	        }		
		}
	}

    private Boolean isTimeForRefresh() {
    	// TODO: implement cache timeout logic.
    	return false;
    }
        
    /* ------------------------------------------------------
     * 
     *     Runtime (doFilter) methods and routines
     * 
     *  -----------------------------------------------------*/
      
    public String lookupBaseURLbyNode(String nodeID) throws ServiceFailure {
    	cacheNodeListURLs();
		return baseUrlMap.get(nodeID);
	}
 
    private Schema createXsdSchema(String xsdUrlString, boolean useSchema) throws ServiceFailure {
        Schema schema;
    	
        if (!useSchema) return null;
        
    	// create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			URL xsdUrl = new URL(xsdUrlString);
			URLConnection xsdUrlConnection = xsdUrl.openConnection();
			InputStream xsdUrlStream = xsdUrlConnection.getInputStream();
			Source schemaFile = new StreamSource(xsdUrlStream);
			schema = factory.newSchema(schemaFile);			
		} catch (MalformedURLException e) {
			throw new ServiceFailure("4150","error: malformed URL for schema: " + xsdUrlString);	
		} catch (IOException e) {
			throw new ServiceFailure("4150","Error connecting to schema: " + xsdUrlString);
		} catch (SAXException e) {
			throw new ServiceFailure("4150","error parsing schema for validation: " + xsdUrlString );
		}       
        return schema;
    }
        
    private DocumentBuilder createStdValidatingDOMParser(Schema xsdSchema, boolean useSchema) throws ServiceFailure {
        
    	DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        documentBuilderFactory.setNamespaceAware(true);
        if (useSchema) {
        	documentBuilderFactory.setSchema(xsdSchema);
        	documentBuilderFactory.setValidating(false);
        }
        
        DocumentBuilder parser;
		try {
			parser = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new ServiceFailure("4150","Error creating a document parser");
		}
        return parser;
    }
     
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    	
    	try {
			doFilterDelegate(req,res,chain);
		} catch (BaseException e) {
			
			byte[] errorMsgXML = e.serialize(BaseException.FMT_XML).getBytes();
			
			HttpServletResponse response = (HttpServletResponse) res;
			
			response.setContentLength(errorMsgXML.length);
//			response.setContentType(type);
			response.getOutputStream().write(errorMsgXML);
			response.flushBuffer( );			
		}

    }
    
    public void doFilterDelegate(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException, ServiceFailure, NotFound {

		// return if init has not been called - it's the setter for filterConfig
        if (filterConfig == null) return;
	
        // compiles without the subtyping to Http versions of request and response.
        // why is the check here?
		if (!(res instanceof HttpServletResponse) || !(req instanceof HttpServletRequest)) {
            throw new ServletException("This filter only supports HTTP");
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;


        //  ****** Handle request before passing control to next filter or servlet  *********
        						   
        // we are going to return xml no matter what

        
        //  ******* pass control to next filter in the chain  ********
        
        BufferedHttpResponseWrapper responseWrapper =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);
        
        chain.doFilter(req, responseWrapper);

        // we're using tomcat 6.  Is the workaround still necessary?
        
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
            chain.doFilter(req, response);
            return;
        }

        //  ****** Handle response from the servlet  *********
        //  response will be either a systemMD object, or a D1 error object
        //  need to handle both

        // read the incoming sysMD stream
        // and convert from bytearray to xmlSource
        ByteArrayInputStream origXMLIn = new ByteArrayInputStream(origXML);
        InputSource xmlSource = new InputSource(origXMLIn);

        
        // ---------------- create DOM doc out of SysMD XML
    
        // parse the input stream, determining if it's sysMD or error
        Schema sysMDschema = createXsdSchema(this.systemmetadataSchemaLocation,this.useSchemas);
        DocumentBuilder sysMDparser = createStdValidatingDOMParser(sysMDschema,this.useSchemas);
		XSDValidationErrorHandler xsdveh = new XSDValidationErrorHandler();
        sysMDparser.setErrorHandler(xsdveh);
		Document metacatDoc = null;
		try {
			metacatDoc = sysMDparser.parse(xmlSource);
		} catch (SAXException e) {
			throw new ServiceFailure("4150","Error parsing /meta output: " + e);
		}
		
		// ---------------- extract info from document

		// compile the xpath expressions we'll use later to extract from the document
		XPathExpression idExpr;
		XPathExpression replicaExpr;
		XPathExpression errorExpr;
		try {
			XPath xpath = this.xFactory.newXPath();
			idExpr = xpath.compile("//identifier");
			replicaExpr = xpath.compile("//replicaMemberNode[../replicationStatus = 'completed']");
			errorExpr = xpath.compile("//error");
		} catch (XPathExpressionException e) {
			throw new ServiceFailure("4150", "error compiling the xpath expressions");
		}

		// apply expressions to get data from systemmetadata 
		ArrayList<String> targetID = null;
		ArrayList<String> replicaIDs = null;
		ArrayList<String> errorXML = null;
		try {
			errorXML = extractLeafElementStrings(metacatDoc,errorExpr);
			targetID = extractLeafElementStrings(metacatDoc,idExpr);
			replicaIDs = extractLeafElementStrings(metacatDoc,replicaExpr);
		} catch (XPathExpressionException e2) {
			throw new ServiceFailure("4150","error extracting data from metcat response. " + e2);
		}

		
		
		// -------------------- create the response content
		
		Document returnDoc = null;
		if (targetID.isEmpty()) {
			if (errorXML.isEmpty()) {	
				throw new ServiceFailure("4150","Unexpected content from metacat returned");
			} else {
	            returnDoc = metacatDoc;
			}
		} else {
			String targetIdentifier = targetID.get(0);
			returnDoc = createObjectLocationList(targetIdentifier, replicaIDs);
		}
						
		// -------- transform return Document to XML  -----------
		
		DOMSource domSource = new DOMSource(returnDoc);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer;
		try {
			serializer = tf.newTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");
				
			// create byte-array output stream to feed to transformer
			ByteArrayOutputStream resultBuf = new ByteArrayOutputStream( );
 
			serializer.transform(domSource, new StreamResult(resultBuf));

//        logger.info("ResolveFilter: response from /meta: " + response + "...");        	
 
			response.setContentLength(resultBuf.size( ));
//      	      response.setContentType(type);
			response.getOutputStream().write(resultBuf.toByteArray( ));
			response.flushBuffer( );
		} catch (TransformerConfigurationException e) {
			throw new ServiceFailure("4150","error setting up the document transformer");
		} catch (TransformerException e) {
			throw new ServiceFailure("4150","error serializing output");
		} 
	}

	private ArrayList<String> extractLeafElementStrings(Document d, XPathExpression expr ) throws XPathExpressionException {
		
		// evaluate the xpath expression
		org.w3c.dom.NodeList nl  = (org.w3c.dom.NodeList) expr.evaluate(d, XPathConstants.NODESET);
		
		ArrayList<String> resultStrings = new ArrayList<String>();
		for (int i=0; i<nl.getLength(); i++) {
			resultStrings.add(nl.item(i).getTextContent());
		}		
		return resultStrings;
	}
	
	private Document createObjectLocationList(String idString, ArrayList<String> nodes) throws ServiceFailure, NotFound {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			throw new ServiceFailure("4150","Error creating ObjectLocationList");
		}
		DOMImplementation impl = builder.getDOMImplementation();

		//document
		Document doc = impl.createDocument(null,null,null);
		//root element
//		org.w3c.dom.Element root = doc.getDocumentElement();
		org.w3c.dom.Element oll = doc.createElement("d1:objectLocationList");
		doc.appendChild(oll);
		oll.setAttribute("xmlns:d1",d1namespaceVersion);
		oll.setAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
		oll.setAttribute("xsi:schemaLocation", d1namespaceVersion + " " + objectlocationlistSchemaURL);
		
		org.w3c.dom.Element id = doc.createElement("identifier");
		id.setTextContent(idString);
		oll.appendChild(id);

		if (nodes.size() == 0) {
			// assuming there should be at least one location to retrieve, so will throw an error
			throw new NotFound("4140","The requested object is not presently available: " + idString);
		}
		
		for(int i=0; i<nodes.size(); i++) {
			String nodeIDstring = nodes.get(i); //.toString();
			String baseURLstring = lookupBaseURLbyNode(nodeIDstring);
			if (baseURLstring == null) {
				throw new ServiceFailure("4150","unregistered Node identifier (" + nodeIDstring + ") in systemmetadata document for object: " + idString);
			}
			String urlString = baseURLstring + "/object/" + idString;
			
			org.w3c.dom.Element loc = doc.createElement("objectLocation");

			org.w3c.dom.Element nodeID = doc.createElement("nodeIdentifier");
			org.w3c.dom.Node n = doc.createTextNode(nodeIDstring);
			nodeID.appendChild(n);
			loc.appendChild(nodeID);
						
			org.w3c.dom.Element baseURL = doc.createElement("baseURL");
			org.w3c.dom.Node b = doc.createTextNode(baseURLstring);
			baseURL.appendChild(b);
			loc.appendChild(baseURL);

			org.w3c.dom.Element url = doc.createElement("url");
			org.w3c.dom.Node u = doc.createTextNode(urlString);
			url.appendChild(u);
			loc.appendChild(url);
			
/*			org.w3c.dom.Element preference = doc.createElement("preference");
			org.w3c.dom.Node p = doc.createTextNode(thePreference);
			preference.appendChild(p);
			loc.appendChild(preference);
	*/			
			oll.appendChild(loc);
		}
		return doc;
	}
	
	
	/* -------------------------------------
	 *    Getters and Setters
	 * -------------------------------------*/

	public Integer getRefreshInterval() {
		return nodelistRefreshInterval;
	}
	
	public void setRefreshInterval(Integer i) {
		nodelistRefreshInterval = i;
	}
}
