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
import org.dataone.cn.auth.X509CertificateGenerator;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.cn.ldap.v1.SubjectLdapPopulation;
import org.dataone.cn.rest.web.identity.IdentityController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
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
    private SubjectLdapPopulation subjectLdapPopulation;
    private X509CertificateGenerator x509CertificateGenerator;
    @Resource
    public void setCNLdapPopulation(SubjectLdapPopulation subjectLdapPopulation) {
        this.subjectLdapPopulation = subjectLdapPopulation;
    }
    @Resource
    public void setTestController(IdentityController testController) {
        this.testController = testController;
    }
    @Resource
    public void setX509CertificateGenerator(X509CertificateGenerator x509CertificateGenerator) {
        this.x509CertificateGenerator = x509CertificateGenerator;
    }
    @Before
    public void before() throws Exception {
        subjectLdapPopulation.populateTestIdentities();
    }
    @After
    public void after() throws Exception {
        //subjectLdapPopulation.deleteAllSubjects();
        subjectLdapPopulation.deletePopulatedSubjects();
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
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.v1.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        for (Person person : subjectList.getPersonList()) {
            log.info("ListSubject: " + person.getSubject().getValue());
        }
        assertNotNull(subjectList);
        assertTrue(subjectList.getPersonList().size() > 0);

    }

    @Test
    public void getSubjectInfo() throws Exception {
        log.info("Test getSubjectInfo");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock/accounts/CN=Dracula,DC=dataone,DC=org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectList subjectList = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectList = (SubjectList) mav.getModel().get("org.dataone.service.types.v1.SubjectList");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectList);
        assertTrue(subjectList.getPersonList().size() > 0);
        
    }

    @Test
    public void registerAccount() throws Exception {
        log.info("Test registerAccount");
    	String subjectValue = "CN=Test1,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);


        MockMultipartFile mockPersonFile = new MockMultipartFile("person",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/accounts");
        request.addFile(mockPersonFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));

        subjectLdapPopulation.testSubjectList.add(subjectValue);
        
    }
    
    @Test
    public void registerAccountFromCertificate() throws Exception {
        log.info("Test registerAccountFromCertificate");
        x509CertificateGenerator.storeSelfSignedCertificate();
        String subjectValue = CertificateManager.getInstance().loadCertificate().getSubjectDN().toString();
        
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);
        MockMultipartFile mockPersonFile = new MockMultipartFile("person",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/accounts");
        request.addFile(mockPersonFile);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));
        subjectLdapPopulation.testSubjectList.add(subjectValue);
        x509CertificateGenerator.cleanUpFiles();
    }

    @Test
    public void updateAccount() throws Exception {
        log.info("Test updateAccount");
    	// register
    	registerAccount();
    	
    	// now update
    	String subjectValue = "CN=Test1,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("update");
        person.setFamilyName("update");
        person.addEmail("update@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);
        MockMultipartFile mockPersonFile = new MockMultipartFile("person",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("PUT");
        request.setContextPath("/Mock/accounts");
        request.addFile(mockPersonFile);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.updateAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));        

    }

    @Ignore
    @Test
    public void verifyAccount() throws Exception {
         log.info("Test verifyAccount");
    	// create the account first
    	registerAccount();
    	
    	String subjectValue = "cn=Dracula,dc=cilogon,dc=org";
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

    	String value = "CN=Test1,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject, baos);

        MockMultipartFile mockSubjectFile = new MockMultipartFile("subject",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/accounts/map");
        request.addFile(mockSubjectFile);

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
    	
    	String value = "CN=Test2,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject, baos);
        MockMultipartFile mockSubjectFile = new MockMultipartFile("subject",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/accounts/confirm");
        request.addFile(mockSubjectFile);


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

    	String value = "cn=testGroup,dc=cilogon,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);
        String groupString = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/groups/");
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

    	String value = "cn=testGroup,dc=cilogon,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        String subjectValue = "CN=Test1,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        SubjectList members = new SubjectList();
        members.addPerson(person);
        
        ByteArrayOutputStream groupBaos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, groupBaos);
        MockMultipartFile mockGroupFile = new MockMultipartFile("groupName",groupBaos.toByteArray());
        
        ByteArrayOutputStream membersBaos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(members, membersBaos);
        MockMultipartFile mockMembersFile = new MockMultipartFile("members",groupBaos.toByteArray());


        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("PUT");
        request.setContextPath("/Mock/groups");
        request.addFile(mockGroupFile);
        request.addFile(mockMembersFile);

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

    	String value = "cn=testGroup,dc=cilogon,dc=org";
        Subject group = new Subject();
        group.setValue(value);
        
        String subjectValue = "CN=Test1,O=Test,C=US,DC=cilogon,DC=org";
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("test");
        person.setFamilyName("test");
        person.addEmail("test@dataone.org");
        SubjectList members = new SubjectList();
        members.addPerson(person);
        
        ByteArrayOutputStream groupBaos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, groupBaos);
        MockMultipartFile mockGroupFile = new MockMultipartFile("groupName",groupBaos.toByteArray());

        ByteArrayOutputStream membersBaos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(members, membersBaos);
        MockMultipartFile mockMembersFile = new MockMultipartFile("members",groupBaos.toByteArray());


        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("DELETE");
        request.setContextPath("/Mock/groups");
        request.addFile(mockGroupFile);
        request.addFile(mockMembersFile);
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
