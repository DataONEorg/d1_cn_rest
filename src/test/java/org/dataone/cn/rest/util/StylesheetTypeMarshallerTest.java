/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author leinfelder
 * 
 */
public class StylesheetTypeMarshallerTest {
    public static Log log = LogFactory.getLog(StylesheetTypeMarshallerTest.class);
  
    @Before
    public void before() throws Exception {
    }
    
    @After
    public void after() throws Exception {
    }
    
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
			assertTrue(content .contains(stylesheet));
			log.warn(content);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

    }
    
}
