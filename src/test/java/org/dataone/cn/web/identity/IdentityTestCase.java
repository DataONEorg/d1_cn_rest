/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.identity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.rest.web.identity.IdentityController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.Person;
import org.dataone.service.types.Subject;
import org.dataone.service.types.SubjectList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author leinfelder
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/identity/mockIdentity-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class IdentityTestCase {

    /** the servlet */
    private WebApplicationContext wac;
    private IdentityController testController;

    @Before
    public void before() throws Exception {
        wac = WebApplicationContextUtils.getRequiredWebApplicationContext(ProxyWebApplicationContextLoader.SERVLET_CONTEXT);
        if (wac == null) {
            throw new Exception("cannot find Web Application Context!");
        }
        testController = wac.getBean(IdentityController.class);
    }

    @Test
    public void listSubjects() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectList subjectList = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectList);
        assertTrue(subjectList.getGroupList().size() > 0);
        
    }
    
    @Test
    public void getSubjectInfo() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/accounts/cn=testGroup,dc=dataone,dc=org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectList subjectList = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectList);
        assertTrue(subjectList.getGroupList().size() > 0);
        
    }
    
    @Test
    public void registerAccount() throws Exception {

    	String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);
        String personValue = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/accounts");
        request.addParameter("person", personValue);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));
        
    }
    
    @Test
    public void verifyAccount() throws Exception {

    	// create the account first
    	registerAccount();
    	
    	String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/accounts/" + subjectValue);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean success = false;
        try {
            ModelAndView mav = testController.verifyAccount(request, response);
            success = (Boolean) mav.getModel().get("java.lang.Boolean");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertTrue(success);
        
    }
    
    /**
     * Requires session in order to work!
     * @throws Exception
     */
    @Test
    public void mapIdentity() throws Exception {

    	String value = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject, baos);
        String subjectString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/accounts/map");
        request.addParameter("subject", subjectString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = false;
        try {
            ModelAndView mav = testController.mapIdentity(request, response);
            result = (Boolean) mav.getModel().get("java.lang.Boolean");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertTrue(result);
        
    }
    
    @Test
    public void createGroup() throws Exception {

    	String value = "cn=testGroup,dc=dataone,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);
        String groupString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/groups");
        request.addParameter("group", groupString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = false;
        try {
            ModelAndView mav = testController.createGroup(request, response);
            result = (Boolean) mav.getModel().get("java.lang.Boolean");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertTrue(result);
        
    }
    
    @Test
    public void addGroupMembers() throws Exception {

    	String value = "cn=testGroup,dc=dataone,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        SubjectList members = new SubjectList();
        members.addPerson(person);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);
        String groupString = baos.toString("UTF-8");
        
        baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(members, baos);
        String membersString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/Mock/groups");
        request.addParameter("groupName", groupString);
        request.addParameter("members", membersString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = false;
        try {
            ModelAndView mav = testController.addGroupMembers(request, response);
            result = (Boolean) mav.getModel().get("java.lang.Boolean");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration: " + ex);
        }

        assertTrue(result);
        
    }
    
    @Test
    public void removeGroupMembers() throws Exception {

    	String value = "cn=testGroup,dc=dataone,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        SubjectList members = new SubjectList();
        members.addPerson(person);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);
        String groupString = baos.toString("UTF-8");
        
        baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(members, baos);
        String membersString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/Mock/groups");
        request.addParameter("groupName", groupString);
        request.addParameter("members", membersString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = false;
        try {
            ModelAndView mav = testController.removeGroupMembers(request, response);
            result = (Boolean) mav.getModel().get("java.lang.Boolean");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration: " + ex);
        }

        assertTrue(result);
        
    }
    
}
