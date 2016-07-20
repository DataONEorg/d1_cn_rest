/**
 * This work was created by participants in the DataONE project, and is jointly copyrighted by participating
 * institutions in DataONE. For more information on DataONE, see our web site at http://dataone.org.
 *
 * Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * $Id$
 */
package org.dataone.cn.rest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.rest.oxm.StylesheetTypeMarshaller;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;

import org.junit.Test;

/**
 *
 * @author leinfelder
 *
 */
public class StylesheetTypeMarshallerTest {

    public static Log log = LogFactory.getLog(StylesheetTypeMarshallerTest.class);

    @Test
    public void testMarshal() throws Exception {
        try {

            // construct our test object
            Person person = new Person();
            person.addEmail("test");
            person.addGivenName("test");
            Subject subject = new Subject();
            subject.setValue("test");
            person.setSubject(subject);
            person.setFamilyName("test");

            // created the marshaller
            StylesheetTypeMarshaller stm = new StylesheetTypeMarshaller();
            String stylesheet = "test.xsl";
            stm.setStylesheet(stylesheet);

            // marshal the object
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Result result = new StreamResult(out);
            stm.marshal(person, result);

            // check the results
            String content = out.toString("UTF-8");
            assertTrue(content.contains(stylesheet));
            log.warn(content);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
