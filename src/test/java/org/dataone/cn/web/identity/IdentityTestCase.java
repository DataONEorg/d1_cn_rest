/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.web.identity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.dataone.cn.rest.web.identity.IdentityController;
import org.dataone.cn.web.proxy.ProxyWebApplicationContextLoader;
import org.dataone.service.exceptions.ServiceFailure;
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
 * @author rwaltz
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
    
}
