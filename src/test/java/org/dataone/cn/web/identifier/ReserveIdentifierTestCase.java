/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.identifier;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.ldap.v1.SubjectLdapPopulation;
import org.dataone.cn.rest.proxy.service.impl.mock.ProxyCNReadServiceImpl;
import org.dataone.cn.rest.web.identifier.ReserveIdentifierController;
import org.dataone.cn.auth.X509CertificateGenerator;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.util.EncodingUtilities;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
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
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/identifier/mockIdentifier-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class ReserveIdentifierTestCase {

    public static Log log = LogFactory.getLog(ReserveIdentifierTestCase.class);
    /** the servlet */
    private ReserveIdentifierController testController;
    private SubjectLdapPopulation cnLdapPopulation;
    private ProxyCNReadServiceImpl proxyCNReadService;
    private X509CertificateGenerator x509CertificateGenerator;
    private ClassPathResource readSystemMetadataResource;

    @Resource
    public void setCNLdapPopulation(SubjectLdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }

    @Resource
    public void setProxyCNReadServiceImpl(ProxyCNReadServiceImpl proxyCNReadService) {
        this.proxyCNReadService = proxyCNReadService;
    }

    @Resource
    public void setTestController(ReserveIdentifierController testController) {
        this.testController = testController;
    }

    @Resource
    public void setX509CertificateGenerator(X509CertificateGenerator x509CertificateGenerator) {
        this.x509CertificateGenerator = x509CertificateGenerator;
    }

    @Resource
    public void readSystemMetadataResource(ClassPathResource readSystemMetadataResource) {
        this.readSystemMetadataResource = readSystemMetadataResource;
    }

    @Test
    public void failReserveIdentifier() throws Exception {
        log.info("Test failReserveIdentifier");
        Object bogus = new Object();
        X509Certificate cert = x509CertificateGenerator.generateDataOneCert("test1");
        X509Certificate[] certificates = {cert};
        // should fail with a IdentifierNotUnique because it is able to discover metadata
        this.proxyCNReadService.setNodeSystemMetadataResource(readSystemMetadataResource);
        String pidValue = "MD_ORNLDAAC_122_030320010095920";

        Identifier pid = new Identifier();
        pid.setValue(pidValue);
        ByteArrayOutputStream pidOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(pid, pidOutput);
        MockMultipartFile pidFile = new MockMultipartFile("pid", pidOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/reserve");
        request.setAttribute("javax.servlet.request.ssl_session", bogus);
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
        request.addFile(pidFile);
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        Identifier result = null;
        try {
            ModelAndView mav = testController.reserveIdentifier(request, response);
            result = (Identifier) mav.getModel().get("org.dataone.service.types.v1.Identifier");
        } catch (IdentifierNotUnique ex) {
            assertEquals(ex.getDescription(), "The given pid is already in use: MD_ORNLDAAC_122_030320010095920");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

    }

    @Test
    public void reserveIdentifier() throws Exception {
        log.info("Test reserveIdentifier");
        Object bogus = new Object();
        X509Certificate cert = x509CertificateGenerator.generateDataOneCert("test1");
        X509Certificate[] certificates = {cert};
        ClassPathResource nonExistantMetadata = new ClassPathResource("nothere");
        this.proxyCNReadService.setNodeSystemMetadataResource(nonExistantMetadata);
        String pidValue = "test" + Long.toHexString(System.currentTimeMillis());
        
        Identifier pid = new Identifier();
        pid.setValue(pidValue);
        ByteArrayOutputStream pidOutput= new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(pid, pidOutput);
        MockMultipartFile pidFile = new MockMultipartFile("pid", pidOutput.toByteArray());

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setContextPath("/Mock/reserve");
        request.setAttribute("javax.servlet.request.ssl_session", bogus);
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
        request.addFile(pidFile);
        
        // the ssl_session may be important, but it does not appear to have a
        // purpose in the CertificateManager other than to print
        // a null if not found

        MockHttpServletResponse response = new MockHttpServletResponse();

        Identifier result = null;
        try {
            ModelAndView mav = testController.reserveIdentifier(request, response);
            result = (Identifier) mav.getModel().get("org.dataone.service.types.v1.Identifier");
            this.cnLdapPopulation.deleteReservation(pidValue);
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(result);
        assertEquals(pidValue, result.getValue());
        
    }
    
    @Test
    public void generateIdentifier() throws Exception {
        log.info("Test generateIdentifier");
        ClassPathResource nonExistantMetadata = new ClassPathResource("nothere");
        this.proxyCNReadService.setNodeSystemMetadataResource(nonExistantMetadata);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/generate");
        request.addParameter("scheme", "UUID");

        // the ssl_session may be important, but it does not appear to have a
        // purpose in the CertificateManager other than to print
        // a null if not found

        Object bogus = new Object();
        request.setAttribute("javax.servlet.request.ssl_session", bogus);

        X509Certificate cert = x509CertificateGenerator.generateDataOneCert("test1");
        X509Certificate[] certificates = {cert};
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Identifier result = null;
        try {
            ModelAndView mav = testController.generateIdentifier(request, response);
            result = (Identifier) mav.getModel().get("org.dataone.service.types.v1.Identifier");
            assertNotNull(result);
            this.cnLdapPopulation.deleteReservation(result.getValue());
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertTrue(result.getValue().startsWith("urn:uuid:"));
    }
}
