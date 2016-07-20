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
package org.dataone.cn.rest.v1.suite;

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
import org.dataone.cn.rest.v1.IdentityController;
import org.dataone.cn.rest.proxy.ProxyWebApplicationContextLoader;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.util.Constants;
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
import org.dataone.service.cn.v1.CNIdentity;
import org.dataone.service.types.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author leinfelder
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/rest/mock-dispatcher.xml", "classpath:/org/dataone/cn/rest/v1/mockIdentityController-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class SuiteTestUnit4Identity {

    public static Log log = LogFactory.getLog(SuiteTestUnit4Identity.class);
    /**
     * the servlet
     */

    private X509CertificateGenerator x509CertificateGenerator;

    private final String primarySubject = Settings.getConfiguration().getString("testIdentity.primarySubject");
    private final String primarySubjectCN = Settings.getConfiguration().getString("testIdentity.primarySubjectCN");

    private final String secondarySubject = Settings.getConfiguration().getString("testIdentity.secondarySubject");
    private final String secondarySubjectCN = Settings.getConfiguration().getString("testIdentity.secondarySubjectCN");

    private final String thirdSubject = Settings.getConfiguration().getString("testIdentity.tertiarySubject");
    private final String thirdSubjectCN = Settings.getConfiguration().getString("testIdentity.tertiarySubjectCN");

    private final String quartarySubject = Settings.getConfiguration().getString("testIdentity.quartarySubject");
    private final String quartarySubjectCN = Settings.getConfiguration().getString("testIdentity.quartarySubjectCN");

    private final String groupName = Settings.getConfiguration().getString("testIdentity.groupName");

    private static final String ACCOUNT_MAPPING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING;
    private static final String ACCOUNT_MAPPING_PENDING_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_MAPPING_PENDING;
    private static final String ACCOUNTS_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNTS;
    private static final String GROUPS_PATH_V1 = "/v1/" + Constants.RESOURCE_GROUPS;
    private static final String ACCOUNT_VERIFICATION_PATH_V1 = "/v1/" + Constants.RESOURCE_ACCOUNT_VERIFICATION;

    private IdentityController testController;

    @Resource
    public void setTestController(IdentityController testController) {
        this.testController = testController;
    }

    @Resource
    @Qualifier("ciLogonX509CertificateGenerator")
    public void setX509CertificateGenerator(X509CertificateGenerator x509CertificateGenerator) {
        this.x509CertificateGenerator = x509CertificateGenerator;
    }

    @Resource
    @Qualifier("testAdminGroup")
    private ClassPathResource testAdminGroup;

    @Autowired
    @Qualifier("identityServiceV1")
    private CNIdentity cnIdentity;

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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Mock" + ACCOUNTS_PATH_V1 + "/" + secondarySubject);
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
        log.info("Test registerAccount of subject");
        Subject subject = new Subject();
        subject.setValue("CN=TestFIVE,O=Test,C=US,DC=cilogon,DC=org");
        Person person = new Person();
        person.setSubject(subject);
        person.setFamilyName("test2");
        person.addGivenName("test2");
        person.addEmail("test2@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);

        MockMultipartFile mockPersonFile = new MockMultipartFile("person", baos.toByteArray());

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

        assertNotNull(retSubject);
        assertTrue(retSubject.equals(subject));

    }

    @Test
    public void registerAccountFromCertificate() throws Exception {
        log.info("Test registerAccountFromCertificate");

        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate("TestThree");
        X509Certificate certificate[] = {x509Certificate};

        String subjectValue = CertificateManager.getInstance().getSubjectDN(certificate[0]);

        Subject subject = new Subject();
        subject.setValue(subjectValue);
        Person person = new Person();
        person.setSubject(subject);
        person.addGivenName("TestThree");
        person.setFamilyName("TestThree");
        person.addEmail("TestThree@dataone.org");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(person, baos);
        MockMultipartFile mockPersonFile = new MockMultipartFile("person", baos.toByteArray());

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
        assertNotNull(retSubject);
        assertTrue(retSubject.equals(subject));

    }

    @Test
    public void updateAccount() throws Exception {
        log.info("Test updateAccount");
        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

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
        MockMultipartFile mockPersonFile = new MockMultipartFile("person", baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("PUT");
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
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

        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        Subject subject = new Subject();
        subject.setValue(secondarySubject);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock" + ACCOUNT_VERIFICATION_PATH_V1 + "/" + secondarySubject);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        boolean success = false;
        try {
            testController.verifyAccount(request, response);
            success = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Test fail" + ex);
        }

        assertTrue(success);

    }

    /**
     * Requires session in order to work!
     *
     * @throws Exception
     */
    @Test
    public void mapAndDeleteIdentity() throws Exception {
        log.info("Test mapAndDeleteIdentity");
        Session session = new Session();

        String cnMapFirstIdentity = "CN=TestMappingONE,O=Test,C=US,DC=cilogon,DC=org";
        Subject cnMapFirstSubject = new Subject();
        cnMapFirstSubject.setValue(cnMapFirstIdentity);
        Person cnMapFirstPerson = new Person();
        cnMapFirstPerson.setSubject(cnMapFirstSubject);
        cnMapFirstPerson.setFamilyName("ONE");
        cnMapFirstPerson.addGivenName("TestMapping");
        cnMapFirstPerson.addEmail("one@dataone.org");

        session.setSubject(cnMapFirstSubject);
        cnIdentity.registerAccount(session, cnMapFirstPerson);

        String cnMapSecondIdentity = "CN=TestMappingTWO,O=Test,C=US,DC=cilogon,DC=org";
        Subject cnMapSecondSubject = new Subject();
        cnMapSecondSubject.setValue(cnMapSecondIdentity);
        Person cnMapSecondPerson = new Person();
        cnMapSecondPerson.setSubject(cnMapSecondSubject);
        cnMapSecondPerson.setFamilyName("ONE");
        cnMapSecondPerson.addGivenName("TestMapping");
        cnMapSecondPerson.addEmail("one@dataone.org");

        session.setSubject(cnMapSecondSubject);
        cnIdentity.registerAccount(session, cnMapSecondPerson);

        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};
        log.debug("loaded certificate of " + certificate[0].getSubjectDN().getName());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();

        request.setMethod("POST");
        request.setContextPath("/Mock" + ACCOUNT_MAPPING_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addParameter("primarySubject", primarySubject);
        request.addParameter("secondarySubject", secondarySubject);
        //request.addFile(mockSubject1File);
        //request.addFile(mockSubject2File);

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

        MockHttpServletRequest deleteRequest = new MockHttpServletRequest("DELETE", "/Mock" + ACCOUNT_MAPPING_PATH_V1 + "/" + secondarySubject);
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

        assertTrue(success);

    }

    @Test
    public void createGroup() throws Exception {
        log.info("Test createGroup");
        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate(thirdSubjectCN);
        X509Certificate certificate[] = {x509Certificate};
        Subject groupSubject = new Subject();
        groupSubject.setValue("CN=New Test Group,DC=dataone,DC=org");

        Group group = new Group();
        group.setSubject(groupSubject);
        group.setGroupName("New Test Group");
        group.addHasMember(groupSubject);
        group.addRightsHolder(groupSubject);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(group, baos);

        MockMultipartFile mockGroupFile = new MockMultipartFile("group", baos.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock" + GROUPS_PATH_V1);
        request.setAttribute("javax.servlet.request.X509Certificate", certificate);
        request.addFile(mockGroupFile);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Subject result = null;
        try {
            ModelAndView mav = testController.createGroup(request, response);

            result = (Subject) mav.getModel().get("org.dataone.service.types.v1.Subject");
        } catch (Exception ex) {
            fail("Test fail" + ex);
        }

        assertNotNull(result);
        assertTrue(result.equals(groupSubject));
    }

    @Test
    public void updateGroupMembers() throws Exception {

        Session session = new Session();
        Group testAdmin = new Group();
        testAdmin = TypeMarshaller.unmarshalTypeFromStream(testAdmin.getClass(), testAdminGroup.getInputStream());

        log.info("Test updateGroupMembers");

        // register first test subject
        String cnNewIdentity = "CN=TestMappingTHREE,O=Test,C=US,DC=cilogon,DC=org";
        Subject cnNewSubject = new Subject();
        cnNewSubject.setValue(cnNewIdentity);
        Person cnNewPerson = new Person();
        cnNewPerson.setSubject(cnNewSubject);
        cnNewPerson.setFamilyName("THREE");
        cnNewPerson.addGivenName("TestMapping");
        cnNewPerson.addEmail("three@dataone.org");

        testAdmin.addHasMember(cnNewSubject);

        X509Certificate x509Certificate = x509CertificateGenerator.getCertificate(primarySubjectCN);
        X509Certificate certificate[] = {x509Certificate};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(testAdmin, baos);
        MockMultipartFile mockGroupFile = new MockMultipartFile("group", baos.toByteArray());
        log.info(new String(baos.toByteArray()));

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

        assertTrue(result);

    }

}
