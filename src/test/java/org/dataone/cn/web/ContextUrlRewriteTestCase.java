/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.*;
import static org.junit.Assert.*;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author rwaltz
 */
public class ContextUrlRewriteTestCase {

    HashMap<String, HashMap<String, String>> tests = new HashMap<String, HashMap<String, String>>();

    @Before
    public void before() throws Exception {
        /* TODO add some that will test failure as well as other conditions */

        HashMap<String, String> test = new HashMap<String, String>();
        test.put("urlpath", "/object");
        test.put("pathresult", "/metacat/object/");
        tests.put("^/object/?$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/metacat/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/object/([^\\?]{1}.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/metacat/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/meta/([^\\?]{1}.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/metacat/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/meta/([^\\?]{1}.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/resolve/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/metacat/resolve/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/resolve/([^\\?]{1}.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/object/?qt=path&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/metacat/object/?qt=path&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/object/?\\?(.*qt=path.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/mercury/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/object/?\\?(.*qt=solr.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/node");
        test.put("pathresult", "/nodeList.xml");
        tests.put("^/node/?$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/metacat/object/");
        test.put("pathresult", "/object/");
        tests.put("^/metacat/object/$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/metacat/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/metacat/object/(.+)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/metacat/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/meta/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/metacat/meta/(.+)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/metacat/resolve/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/resolve/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/metacat/resolve/(.+)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/mercury/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/mercury/object/(.+)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/nodeList.xml");
        test.put("pathresult", "/node/");
        tests.put("^/nodeList\\.xml$", test);


    }

    @Test
    public void parseUrlRewrite() throws Exception {
        File rewriteXml = ResourceUtils.getFile("src/main/webapp/WEB-INF/config/urlrewrite.xml");

        DocumentBuilder parser;
        // create a SchemaFactory capable of understanding WXS schemas
        Document document;

        // load a WXS schema, represented by a Schema instance

        // InputStream resourceStream = this.getClass().getResourceAsStream(xsdDocument);


        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(false);

        parser = documentBuilderFactory.newDocumentBuilder();

        // load in the file to validate
//        document = parser.parse(this.getClass().getResourceAsStream(xmlDocument));
        document = parser.parse(rewriteXml);

        // create a Validator instance, which can be used to validate an instance document

        Element root = document.getDocumentElement();
        NodeList urlrewriteChildren = root.getChildNodes();
        for (int i = 0; i < urlrewriteChildren.getLength(); ++i) {
            Node urlrewrite = urlrewriteChildren.item(i);
            if (urlrewrite.getNodeType() != urlrewrite.ELEMENT_NODE)
            {
                continue;
            }
            NodeList ruleChildren = urlrewrite.getChildNodes();
            String fromString = "";
            String toString = "";
            for (int j = 0; j < ruleChildren.getLength(); ++j) {
                Node rule = ruleChildren.item(j);
                if (rule.getNodeName().equals("from")) {
                    fromString = rule.getTextContent();
                } else if (rule.getNodeName().equals("to")) {
                    toString = rule.getTextContent();
                }
            }
            HashMap<String, String> test = tests.get(fromString);
            if (test != null) {
                String testResult = test.get("urlpath").replaceAll(fromString, toString);
                System.out.println("Testing " + urlrewrite.getNodeName() + " from '" + fromString + "' to '" + toString + "' with the result: " + testResult);
                assertTrue(testResult.equals(test.get("pathresult")));
            } else {
                System.err.println("Cannot test " + urlrewrite.getNodeName() + " from '" + fromString + "' to '" + toString + "'");
                throw new Exception("Cannot test " + urlrewrite.getNodeName() + " from '" + fromString + "' to '" + toString + "'");
            }
        }
    }
}
