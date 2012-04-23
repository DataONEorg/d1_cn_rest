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
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SubjectList;
import org.dataone.service.util.Constants;
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
import java.security.cert.X509Certificate;
import org.dataone.cn.ldap.v1.NodeLdapPopulation;

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
    private NodeLdapPopulation cnLdapPopulation;
    private String primarySubject = Settings.getConfiguration().getString("testIdentity.primarySubject");
    private String secondarySubject = Settings.getConfiguration().getString("testIdentity.secondarySubject");
    private String groupName = Settings.getConfiguration().getString("testIdentity.groupName");
    private static final String ACCOUNT_MAPPING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING;
    private static final String ACCOUNT_MAPPING_PENDING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING_PENDING;
    private static final String ACCOUNTS_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNTS;
    private static final String GROUPS_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS;
    private static final String ACCOUNT_VERIFICATION_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_VERIFICATION;
    @Resource
    public void setCNLdapPopulation(NodeLdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock" + ACCOUNTS_PATH_V1);
        request.setParameter("query", "");
        request.setParameter("start", "0");
        request.setParameter("count", "10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectInfo subjectInfo = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectInfo = (SubjectInfo) mav.getModel().get("org.dataone.service.types.v1.SubjectInfo");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        for (Person person : subjectInfo.getPersonList()) {
            log.info("ListSubject: " + person.getSubject().getValue());
        }
        assertNotNull(subjectInfo);
        assertTrue(subjectInfo.getPersonList().size() > 0);

    }

    @Test
    public void getSubjectInfo() throws Exception {
        log.info("Test getSubjectInfo");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock" + ACCOUNTS_PATH_V1 + "/CN=Dracula,DC=dataone,DC=org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SubjectInfo subjectInfo = null;
        try {
            ModelAndView mav = testController.listSubjects(request, response);
            subjectInfo = (SubjectInfo) mav.getModel().get("org.dataone.service.types.v1.SubjectInfo");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(subjectInfo);
        assertTrue(subjectInfo.getPersonList().size() > 0);
        
    }

    @Test
    public void registerAccount() throws Exception {
        log.info("Test registerAccount of subject" + primarySubject);
        Subject subject = new Subject();
        subject.setValue(primarySubject);
        Person person = new Person();
        person.setSubject(subject);
        person.setFamilyName("test1");
        person.addGivenName("test1");
        person.addEmail("test1@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);


        MockMultipartFile mockPersonFile = new MockMultipartFile("person",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock" + ACCOUNTS_PATH_V1);
        request.addFile(mockPersonFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }
        subjectLdapPopulation.testSubjectList.add(primarySubject);
        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(primarySubject));

    }
    
    @Test
    public void registerSecondAccount() throws Exception {
        log.info("Test registerAccount of subject" + secondarySubject);
        Subject subject = new Subject();
        subject.setValue(secondarySubject);
        Person person = new Person();
        person.setSubject(subject);
        person.setFamilyName("test2");
        person.addGivenName("test2");
        person.addEmail("test2@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);


        MockMultipartFile mockPersonFile = new MockMultipartFile("person",baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock" + ACCOUNTS_PATH_V1);
        request.addFile(mockPersonFile);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }
        subjectLdapPopulation.testSubjectList.add(secondarySubject);
        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(secondarySubject));

    }
    @Test
    public void registerAccountFromCertificate() throws Exception {
        log.info("Test registerAccountFromCertificate");
        x509CertificateGenerator.storeSelfSignedCertificate();

        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};
        String subjectValue = CertificateManager.getInstance().getSubjectDN(certificate[0]);
        
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
        request.setContextPath("/Mock" + ACCOUNTS_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addFile(mockPersonFile);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Subject retSubject = null;
        try {
            ModelAndView mav = testController.registerAccount(request, response);
            retSubject = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }
        subjectLdapPopulation.testSubjectList.add(subjectValue);
        x509CertificateGenerator.cleanUpFiles();
        assertNotNull(retSubject);
        assertTrue(retSubject.getValue().equals(subjectValue));

    }

    @Test
    public void updateAccount() throws Exception {
        log.info("Test updateAccount");
    	// register
    	registerAccount();
    	
    	// now update
        Subject subject = new Subject();
        subject.setValue(primarySubject);
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
        request.setContextPath("/Mock" + ACCOUNTS_PATH_V1);
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
        assertTrue(retSubject.getValue().equals(primarySubject));

    }

    @Test
    public void verifyAccount() throws Exception {
         log.info("Test verifyAccount");
    	// create the account first
    	registerAccount();
    	
        Subject subject = new Subject();
        subject.setValue(primarySubject);
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock"  + ACCOUNT_VERIFICATION_PATH_V1 + "/"+ primarySubject);
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
    @Test
    public void mapAndDeleteIdentity() throws Exception {
        log.info("Test mapAndDeleteIdentity");
        registerAccount();
        registerSecondAccount();
        cnLdapPopulation.populateTestCN();
        x509CertificateGenerator.storeSelfSignedCertificate();

        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};

        Subject subject1 = new Subject();
        subject1.setValue(primarySubject);
        Subject subject2 = new Subject();
        subject2.setValue(secondarySubject);

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject1, baos1);

        MockMultipartFile mockSubject1File = new MockMultipartFile("primarySubject",baos1.toByteArray());

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(subject2, baos2);

        MockMultipartFile mockSubject2File = new MockMultipartFile("secondarySubject",baos2.toByteArray());
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock" + ACCOUNT_MAPPING_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addFile(mockSubject1File);
        request.addFile(mockSubject2File);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = false;
        try {
            testController.mapIdentity(request, response);
            result = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test fail" + ex);
        }

        assertTrue(result);

        // Now Try and Delete the identities mapped
         log.info("Test removeMapIdentity");
    	// create the account first

        MockHttpServletRequest deleteRequest = new MockHttpServletRequest("DELETE", "/Mock" + ACCOUNT_MAPPING_PATH_V1 + "/"+ secondarySubject);
        deleteRequest.setAttribute("javax.servlet.request.X509Certificate", certificate);
        MockHttpServletResponse deleteResponse = new MockHttpServletResponse();

        boolean success = false;
        try {
            testController.removeMapIdentity(deleteRequest, deleteResponse);
            success = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test fail" + ex);
        }
        x509CertificateGenerator.cleanUpFiles();
        cnLdapPopulation.deletePopulatedNodes();
        assertTrue(success);

    }
    

    
    @Test
    public void createGroup() throws Exception {

        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};
        Subject groupSubject = new Subject();
        groupSubject.setValue(groupName);

        Group group = new Group();
        group.setGroupName(groupName);
        group.setSubject(groupSubject);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);

        MockMultipartFile mockGroupFile = new MockMultipartFile("group",baos.toByteArray());
        

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock" + GROUPS_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addFile(mockGroupFile);

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = false;
        try {
            testController.createGroup(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }
        subjectLdapPopulation.testSubjectList.add(groupName);
        x509CertificateGenerator.cleanUpFiles();
        assertTrue(result);

    }


    @Test
    public void updateGroupMembers() throws Exception {

        this.createGroup();
        x509CertificateGenerator.storeSelfSignedCertificate();
        X509Certificate certificate[] = {CertificateManager.getInstance().loadCertificate()};
        Subject groupSubject = new Subject();
        groupSubject.setValue(groupName);

        Group group = new Group();
        group.setGroupName(groupName);
        group.setSubject(groupSubject);

        Subject newMember = new Subject();
        newMember.setValue(secondarySubject);
        SubjectList newMembers = new SubjectList();
        newMembers.addSubject(newMember);

        group.setHasMemberList(newMembers.getSubjectList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);
        MockMultipartFile mockGroupFile = new MockMultipartFile("group",baos.toByteArray());


        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("PUT");
        request.setContextPath("/Mock" + GROUPS_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addFile(mockGroupFile);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = false;
        try {
            testController.updateGroup(request, response);
            result = true;
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }
        x509CertificateGenerator.cleanUpFiles();
        assertTrue(result);
        
    }

}
