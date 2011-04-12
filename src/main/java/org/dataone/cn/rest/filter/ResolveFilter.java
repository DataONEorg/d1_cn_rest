package org.dataone.cn.rest.filter;

// TODO: refactor to split out nodelist logic into separate class - CachedNodeList
// using bridge pattern, perhaps to swap out jax implementation with jibx one when the time comes
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.dataone.cn.rest.web.node.NodeController;
import org.dataone.service.EncodingUtilities;
import org.dataone.service.exceptions.*;
import org.dataone.service.types.Node;
import org.dataone.service.types.NodeList;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * ResolveFilter interoperates with UrlRewriteFilter to handle cn/resolve calls.
 * Using a Tomcat / Spring filter chain, UrlRewriteFilter redirects the 
 * cn/resolve call to cn/meta, and ResolveFilter intercepts the response and
 * transforms it into an ojbectLocationList.  To accomplish this, it also 
 * caches baseURL/nodeID pairs obtained from the dataone nodeList, refreshing 
 * the cache periodically, based on a minimum refresh interval, and the nodelist
 * modication date
 * 
 * @param targetEnvironment:          for nodelist selection: prod,staging,test
 * @param nodelistLocation:           URI for where to find the nodelist  (URL or file)
 * @param nodelistSchemaLocation:            a URI for the nodelist schema (URL or file)
 * @param systemmetadataSchemaLocation:      a URI for the sysMD schema     (URL or file)
 * @param objectlocationlistSchemaLocation:  a URI for the objloclist schema (URL or file)
 * @param nodelistRefreshIntervalSeconds:    the nodelist cache will be refreshed
 *      									    after this number of seconds
 * @param useSchemaValidation:        if false, bypass validating data objects
 *                                    against their schemas
 *                                  
 * @author rnahf 
 *
 */
public class ResolveFilter implements Filter {

    Logger logger = Logger.getLogger(ResolveFilter.class);
    private FilterConfig filterConfig = null;
    private HashMap<String, String> baseUrlMap = null;
    private XPathFactory xFactory = null;
    private long lastRefreshTimeMS = 0;
    // parameters and their default values  (defaulting for production environment)
    // (see d1_cn_rest/src/main/webapp/WEB-INF/web.xml for std settings of these parameters) 
    private Integer nodelistRefreshIntervalSeconds = 2 * 60;
    private String nodelistSchemaLocation = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5_1/dataoneTypes.xsd";
    private String systemmetadataSchemaLocation = "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5_1/dataoneTypes.xsd";
    private String targetEnvironment = "prod";
    private boolean useSchemaValidation = true;
    // static for this deployment of the dataone architecture
    // if you are changing this, you better look at the procedure to 
    // create the objectLocationList
    private static String oll_d1namespaceVersion = "http://dataone.org/service/types/0.5.1";
    private static String oll_publicSchemaLocation =
            "https://repository.dataone.org/software/cicore/tags/D1_SCHEMA_0_5_1/dataoneTypes.xsd";

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void destroy() {
        this.filterConfig = null;
        this.baseUrlMap = null;
        this.xFactory = null;
    }

    /*
     * @see javax.servlet.Filter#destroy(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("init ResolveFilter");
        this.filterConfig = filterConfig;

        if (filterConfig.getInitParameter("nodelistRefreshIntervalSeconds") != null) {
            try {
                this.nodelistRefreshIntervalSeconds = Integer.parseInt(filterConfig.getInitParameter("nodelistRefreshIntervalSeconds"));
            } catch (NumberFormatException e) {
                throw new ServletException(e);
            }
        }

        // TODO: implement targetEnvironment in node selection
        if (filterConfig.getInitParameter("targetEnvironment") != null) {
            this.targetEnvironment = filterConfig.getInitParameter("targetEnvironment");
        }

        if (filterConfig.getInitParameter("useSchemaValidation") != null) {
            if (filterConfig.getInitParameter("useSchemaValidation") == "false") {
                this.useSchemaValidation = false;
            } else if (filterConfig.getInitParameter("useSchemaValidation") == "true") {
                this.useSchemaValidation = true;
            } else {
                throw new ServletException("bad value for input parameter 'useSchemaValidation'");
            }
        }
        if (filterConfig.getInitParameter("nodelistSchemaLocation") != null) {
            this.nodelistSchemaLocation = filterConfig.getInitParameter("nodelistSchemaLocation");
        }

        if (filterConfig.getInitParameter("systemmetadataSchemaLocation") != null) {
            this.systemmetadataSchemaLocation = filterConfig.getInitParameter("systemmetadataSchemaLocation");
        }

        this.xFactory = XPathFactory.newInstance();
    }

    /**
     *
     * @throws ServiceFailure
     */
    private void cacheNodeListURLs(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure {
        NodeList nodeList = null;

        if (isTimeForRefresh()) {
            this.baseUrlMap = null;
        }

        if (this.baseUrlMap == null) {
            baseUrlMap = new HashMap<String, String>();
            WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());

            NodeController nodeController = (NodeController) wac.getBean("nodeController");
            try {
                ModelAndView mav = nodeController.getNodeList(request, response);
                nodeList = (NodeList) mav.getModel().get("org.dataone.service.types.NodeList");
            } catch (ServiceFailure sf) {
                sf.setDetail_code("4150");
                throw sf;
            }
            for (Node node : nodeList.getNodeList()) {
                if (node.getBaseURL().isEmpty()) {
                    throw new ServiceFailure("4150", "Error parsing Nodelist: cannot get baseURL for node id: " + node.getIdentifier().getValue());
                }
                baseUrlMap.put(node.getIdentifier().getValue(), node.getBaseURL());
            }
        }

    }

    /**
     * determines if it is time to refresh the nodelist information cache.  The combination of refresh interval
     * and the modification date on the nodelist file both help to minimize unnecessary refreshes and checks.
     * 
     * @return boolean.  true if time to refresh 
     */
    private Boolean isTimeForRefresh() {
        Date now = new Date();
        long nowMS = now.getTime();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.format(now);

        // convert seconds to milliseconds
        long refreshIntervalMS = getRefreshInterval() * 1000;
        if (nowMS - this.lastRefreshTimeMS > refreshIntervalMS) {
            this.lastRefreshTimeMS = nowMS;
            logger.info("  nodelist refresh: new cached time: " + df.format(now));
            return true;
        } else {
            return false;
        }
    }

    /**
     *  procedure to lookup a BaseURL by the node id.  Meant to be called by
     *  internally, but made public for testing sake, and no apparent reason to restrict it.
     *  
     *  @param String nodeID  - the registered nodeID
     *  @return String theBaseURL mapped to the nodeID
     *  @exception org.dataone.service.exceptions.ServiceFailure
     * 
     **/
    public String lookupBaseURLbyNode(String nodeID) throws ServiceFailure {

        return baseUrlMap.get(nodeID);
    }

    private Schema createXsdSchema(String xsdUrlString, boolean useSchema) throws ServiceFailure {
        Schema schema;

        if (!useSchema) {
            return null;
        }

        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Source schemaFile;
            if (xsdUrlString.startsWith("http")) {
                URL xsdUrl = new URL(xsdUrlString);
                URLConnection xsdUrlConnection = xsdUrl.openConnection();
                InputStream xsdUrlStream = xsdUrlConnection.getInputStream();
                schemaFile = new StreamSource(xsdUrlStream);
            } else {
                schemaFile = new StreamSource(new File(xsdUrlString));
            }
            schema = factory.newSchema(schemaFile);
        } catch (MalformedURLException e) {
            throw new ServiceFailure("4150", "error: malformed URL for schema: " + xsdUrlString);
        } catch (IOException e) {
            throw new ServiceFailure("4150", "Error connecting to schema: " + xsdUrlString);
        } catch (SAXException e) {
            throw new ServiceFailure("4150", "error parsing schema for validation: " + xsdUrlString);
        }
        return schema;
    }

    private DocumentBuilder createNSDOMParser() throws ServiceFailure {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder parser;
        try {
            parser = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServiceFailure("4150", "Error creating a document parser");
        }
        return parser;
    }

    /**
     *  location of the translation logic that transforms systemMetadata to an objectlocationList
     *  or passes through or serializes any error condition that arose at runtime
     *  
     *  For general information on doFilter:
     *  @see javax.servlet.Filter#doFilter(javax.servlet.FilterConfig)
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        try {
            doFilterDelegate(req, res, chain);
        } catch (BaseException e) {

            byte[] errorMsgXML = e.serialize(BaseException.FMT_XML).getBytes();

            HttpServletResponse response = (HttpServletResponse) res;
//			response.sendError(e.getCode(), "ResolveFilter: " + e.getMessage());
            response.setStatus(e.getCode());
            response.setContentLength(errorMsgXML.length);
//			response.setContentType(type);
            response.getOutputStream().write(errorMsgXML);
            response.flushBuffer();
        }

    }

    private void doFilterDelegate(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException, ServiceFailure, NotFound {

        // return if init has not been called - it's the setter for filterConfig
        if (filterConfig == null) {
            return;
        }

        // compiles without the subtyping to Http versions of request and response.
        // why is the check here?
        if (!(res instanceof HttpServletResponse) || !(req instanceof HttpServletRequest)) {
            throw new ServletException("This filter only supports HTTP");
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        cacheNodeListURLs(request, response);
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

        byte[] origXML = responseWrapper.getBuffer();
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
        Schema sysMDschema = createXsdSchema(this.systemmetadataSchemaLocation, this.useSchemaValidation);
        DocumentBuilder sysMDparser = createNSDOMParser();

        Document metaDoc = null;
        try {
            metaDoc = sysMDparser.parse(xmlSource);
        } catch (SAXException e) {
            throw new ServiceFailure("4150", "Error parsing /meta output: " + e);
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
            errorExpr = xpath.compile("//error/@errorCode");
        } catch (XPathExpressionException e) {
            throw new ServiceFailure("4150", "error compiling the xpath expressions");
        }

        // apply expressions to get data from systemmetadata
        ArrayList<String> targetID = null;
        ArrayList<String> replicaIDs = null;
        ArrayList<String> errorXML = null;
        try {
            errorXML = extractLeafElementStrings(metaDoc, errorExpr);
            targetID = extractLeafElementStrings(metaDoc, idExpr);
            replicaIDs = extractLeafElementStrings(metaDoc, replicaExpr);
        } catch (XPathExpressionException e2) {
            throw new ServiceFailure("4150", "error extracting data from metcat response. " + e2);
        }


        // -------------------- create the response content

        Document returnDoc = null;
        if (targetID.isEmpty()) {
            if (errorXML.isEmpty()) {
                throw new ServiceFailure("4150", "Unexpected content from /meta service returned");
            } else {
                response.setStatus(Integer.parseInt(errorXML.get(0).trim()));
                returnDoc = metaDoc;
            }
        } else {
            if (useSchemaValidation) {
                try {
                    Validator sysMDvalidator = sysMDschema.newValidator();
                    sysMDvalidator.validate(new DOMSource(metaDoc));
                } catch (SAXException e1) {
                    throw new ServiceFailure("4150", "document invalid against SystemMetadata schema (" + this.systemmetadataSchemaLocation + ") " + e1);
                } catch (IOException e1) {
                    throw new ServiceFailure("4150", "IO error during schema validation: " + e1);
                }
            }
            String targetIdentifier = targetID.get(0);
            targetIdentifier = EncodingUtilities.decodeXmlDataItems(targetIdentifier);
            returnDoc = createObjectLocationList(targetIdentifier, replicaIDs);
        }

        // -------- transform return Document to XML  -----------

        DOMSource domSource = new DOMSource(returnDoc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer;
        try {
            serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");

            // create byte-array output stream to feed to transformer
            ByteArrayOutputStream resultBuf = new ByteArrayOutputStream();

            serializer.transform(domSource, new StreamResult(resultBuf));

//        logger.info("ResolveFilter: response from /meta: " + response + "...");        	

            response.setContentLength(resultBuf.size());
//      	      response.setContentType(type);
            response.getOutputStream().write(resultBuf.toByteArray());
            response.flushBuffer();
        } catch (TransformerConfigurationException e) {
            throw new ServiceFailure("4150", "error setting up the document transformer");
        } catch (TransformerException e) {
            throw new ServiceFailure("4150", "error serializing output");
        }
    }

    private ArrayList<String> extractLeafElementStrings(Document d, XPathExpression expr) throws XPathExpressionException {

        // evaluate the xpath expression
        org.w3c.dom.NodeList nl = (org.w3c.dom.NodeList) expr.evaluate(d, XPathConstants.NODESET);

        ArrayList<String> resultStrings = new ArrayList<String>();
        for (int i = 0; i < nl.getLength(); i++) {
            resultStrings.add(nl.item(i).getTextContent());
        }
        return resultStrings;
    }

    // TODO: Implement JSON formatted output, based on Accept type in the request
    private Document createObjectLocationList(String idString, ArrayList<String> nodes) throws ServiceFailure, NotFound {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServiceFailure("4150", "Error creating ObjectLocationList");
        }
        DOMImplementation impl = builder.getDOMImplementation();

        //document
        Document doc = impl.createDocument(null, null, null);
        //root element
//		org.w3c.dom.Element root = doc.getDocumentElement();
        org.w3c.dom.Element oll = doc.createElement("d1:objectLocationList");
        doc.appendChild(oll);
        oll.setAttribute("xmlns:d1", oll_d1namespaceVersion);
        oll.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        oll.setAttribute("xsi:schemaLocation", oll_d1namespaceVersion + " " + oll_publicSchemaLocation);

        org.w3c.dom.Element id = doc.createElement("identifier");
        id.setTextContent(idString);
        oll.appendChild(id);

        if (nodes.size() == 0) {
            // assuming there should be at least one location to retrieve, so will throw an error
            throw new NotFound("4140", "The requested object is not presently available: " + idString);
        }

        for (int i = 0; i < nodes.size(); i++) {
            String nodeIDstring = nodes.get(i); //.toString();
            String baseURLstring = lookupBaseURLbyNode(nodeIDstring);
            if (baseURLstring == null) {
                throw new ServiceFailure("4150", "unregistered Node identifier ("
                        + nodeIDstring + ") in systemmetadata document for object: " + idString);
            }
            // the id is put into the path portion of the url, so encoding thusly ;)
            String encodedIdString = EncodingUtilities.encodeUrlPathSegment(idString);
            String urlString;
            if (baseURLstring.endsWith("/")) {
                urlString = baseURLstring + "object/" + encodedIdString;
            } else {
                urlString = baseURLstring + "/object/" + encodedIdString;
            }

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

            // TODO: implement preference
/*			org.w3c.dom.Element preference = doc.createElement("preference");
            org.w3c.dom.Node p = doc.createTextNode(thePreference);
            preference.appendChild(p);
            loc.appendChild(preference);
             */
            oll.appendChild(loc);
        }
        return doc;
    }

    //  ------   Getters and Setters --------------//
    /**
     *  @return the refresh interval for the nodelist information cache, in seconds
     */
    public Integer getRefreshInterval() {
        return this.nodelistRefreshIntervalSeconds;
    }

    /**
     *
     * @param i  in seconds, the minimum interval between nodelist information cache refreshes
     */
    public void setRefreshInterval(Integer i) {
        this.nodelistRefreshIntervalSeconds = i;
    }
}
