/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.batch.utils.TypeMarshaller;
import org.dataone.cn.ldap.LdapPopulation;
import org.dataone.cn.rest.web.identifier.ReserveIdentifierController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.Identifier;
import org.junit.After;
import org.junit.Before;
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
@ContextConfiguration(locations = {"classpath:/org/dataone/cn/resources/web/identifier/mockIdentifier-beans.xml"}, loader = ProxyWebApplicationContextLoader.class)
public class ReserveIdentifierTestCase {
    public static Log log = LogFactory.getLog(ReserveIdentifierTestCase.class);
    /** the servlet */
    private ReserveIdentifierController testController;
    private LdapPopulation cnLdapPopulation;
    @Resource
    public void setCNLdapPopulation(LdapPopulation ldapPopulation) {
        this.cnLdapPopulation = ldapPopulation;
    }
    @Resource
    public void setTestController(ReserveIdentifierController testController) {
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
    public void reserveIdentifier() throws Exception {
        log.info("Test reserveIdentifier");
        
    	Identifier pid = new Identifier();
    	pid.setValue("test");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(pid, baos);
        String pidValue = baos.toString("UTF-8");
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Mock/reserve");
        request.addParameter("pid", pidValue);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Identifier result = null;
        try {
            ModelAndView mav = testController.reserveIdentifier(request, response);
            result = (Identifier) mav.getModel().get("org.dataone.service.types.Identifier");
        } catch (ServiceFailure ex) {
            fail("Test misconfiguration" + ex);
        }

        assertNotNull(result);
        assertEquals(pid.getValue(), result.getValue());
        
    }
    
}
