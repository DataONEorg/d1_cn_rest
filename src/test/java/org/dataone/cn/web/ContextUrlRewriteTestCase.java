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
        test.put("urlpath", "/v1/object");
        test.put("pathresult", "/v1/metacat/object/");
        tests.put("^/v1/object/?$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/v1/object/?qt=path&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/v1/metacat/object/?qt=path&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/v1/object/?\\?(.*qt=path.*)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/v1/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/v1/mercury/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/v1/object/?\\?(.*qt=solr.*)$", test);
        test = new HashMap<String, String>();

        test.put("urlpath", "/v1/metacat/object/");
        test.put("pathresult", "/v1/object/");
        tests.put("^/v1/metacat/object/$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/v1/metacat/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        test.put("pathresult", "/v1/object/http%3A%2F%2Ffoo.com%2Fmeta%2F18");
        tests.put("^/v1/metacat/object/(.+)$", test);
        test = new HashMap<String, String>();
        test.put("urlpath", "/v1/mercury/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        test.put("pathresult", "/v1/object/?qt=solr&q=carbon&start=0&rows=20&sort=dateSysMetadataModified%20desc,%20size%20asc");
        tests.put("^/v1/mercury/object/(.+)$", test);



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
