/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.identity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.ldap.LdapPopulation;
import org.dataone.cn.rest.web.identity.IdentityController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.Person;
import org.dataone.service.types.Subject;
import org.dataone.service.types.SubjectList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author leinfelder
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/identity/mockIdentity-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class IdentityTestCase {
    public static Log log = LogFactory.getLog( IdentityTestCase.class);
    /** the servlet */
    private IdentityController testController;
    private LdapPopulation cnLdapPopulation;
    @Resource
    public void setCNLdapPopulation(LdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }
    @Resource
    public void setTestController(IdentityController testController) {
        this.testController = testController;
    }
    @Before
    public void before() throws Exception {
        cnLdapPopulation.populateTestIdentities();
    }
    @After
    public void after() throws Exception {
        cnLdapPopulation.deletePopulatedSubjects();
    }
    @Test
    public void listSubjects() throws Exception {
        log.info("Test listSubjects");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/accounts");
        request.setParameter("query", "");
        request.setParameter("start", "0");
        request.setParameter("count", "10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectList subjectList = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectList);
        assertTrue(subjectList.getPersonList().size() > 0);

    }

    @Test
    public void getSubjectInfo() throws Exception {
        log.info("Test getSubjectInfo");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/accounts/cn=Dracula,dc=dataone,dc=org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectList subjectList = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectList);
        assertTrue(subjectList.getPersonList().size() > 0);
        
    }

    @Test
    public void registerAccount() throws Exception {
        log.info("Test registerAccount");
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

        // I have the spring ldap configured to assume the dn and only account for the rdn
    	String subjectValuePop = "test1";
        Subject subjectPop = new Subject();
        subjectPop.setValue(subjectValuePop);
        cnLdapPopulation.testSubjectList.add(subjectPop);
        
    }
    
    @Test
    public void registerAccountFromCertificate() throws Exception {
        log.info("Test registerAccountFromCertificate");
        String subjectValue = CertificateManager.getInstance().loadCertificate().getSubjectDN().toString();
        
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
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
    public void updateAccount() throws Exception {
        log.info("Test updateAccount");
    	// register
    	registerAccount();
    	
    	// now update
    	String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("update");
        person.setFamilyName("update");
        person.addEmail("update@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);
        String personValue = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/Mock/accounts");
        request.addParameter("person", personValue);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.updateAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));        
        
    }

    @Test
    public void verifyAccount() throws Exception {
         log.info("Test verifyAccount");
    	// create the account first
    	registerAccount();
    	
    	String subjectValue = "cn=test1,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/accounts/" + subjectValue);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean success = false;
        try {
            testController.verifyAccount(request, response);
            success = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertTrue(success);
        
    }
    
    /**
     * Requires session in order to work!
     * @throws Exception
     */
    @Ignore
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
            testController.mapIdentity(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertTrue(result);
        
    }
    
    /**
     * Requires session in order to work!
     * @throws Exception
     */
    @Ignore
    @Test
    public void confirmMapIdentity() throws Exception {

    	// make the mapping request before trying to confirm
    	this.mapIdentity();
    	
    	String value = "cn=test2,dc=dataone,dc=org";
        Subject subject = new Subject();
        subject.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject, baos);
        String subjectString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/accounts/confirm");
        request.addParameter("subject", subjectString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = false;
        try {
            testController.confirmMapIdentity(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertTrue(result);
        
    }
    @Ignore
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
            testController.createGroup(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertTrue(result);
        
    }
    @Ignore
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
            testController.addGroupMembers(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertTrue(result);
        
    }

    @Ignore
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
            testController.removeGroupMembers(request, response);
            result = true;
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration: " + ex);
        }

        assertTrue(result);
        
    }
    
}
