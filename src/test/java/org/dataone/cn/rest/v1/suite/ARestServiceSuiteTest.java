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

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateAuthenticator;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.dataone.cn.rest.v2.suite.SuiteTestUnit7Core;
import org.dataone.test.apache.directory.server.integ.ApacheDSSuiteRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 * This is a test of Junit Suite cooperating with Spring Junit Testing
 *
 * In particular this Suite test confirms that the DataONE Test Resource SuiteTestARestService can be used in conjuction
 * with Spring JUnit tests
 *
 * @author waltz
 *
 */
@RunWith(ApacheDSSuiteRunner.class)
@SuiteClasses({SuiteTestUnit1DisableFilter.class,
 SuiteTestUnit2Core.class,
 SuiteTestUnit3Registry.class,
 SuiteTestUnit4Identity.class,
 SuiteTestUnit6ResolveFilter.class,
 SuiteTestUnit5ReadController.class,
 SuiteTestUnit7Core.class,
 }) 

@CreateDS(allowAnonAccess = false, enableAccessControl = true, authenticators = {
    @CreateAuthenticator(type = SimpleAuthenticator.class)}, name = "org", partitions = {
    @CreatePartition(name = "org", suffix = "dc=org")})
@ApplyLdifFiles({"org/dataone/test/apache/directory/server/dataone-schema.ldif",
    "org/dataone/test/apache/directory/server/dataone-base-data.ldif",
    "org/dataone/test/services/types/v1/nodes/ldif/devNodeList.ldif",
    "org/dataone/test/services/types/v1/person/ldif/devTestPrincipal.ldif",
    "org/dataone/test/services/types/v1/group/ldif/testAdminGroup.ldif",
    "org/dataone/cn/samples/v1/testNodes.ldif"})
@CreateLdapServer(transports = {
    @CreateTransport(address = "localhost", protocol = "LDAP", port = 10389)})
public class ARestServiceSuiteTest {
    /*
     * 
     * To Run With Spring  Test Suite
     * 
     * 
    
     1) Have a Base test Class which has @Runwith() and @Contextconfiguration() .
     @RunWith(SpringJUnit4ClassRunner.class)
     @ContextConfiguration(locations = { "classpath:/org/dataone/configuration/testApplicationContext.xml" })
     Extend this base class to all the test cases required. 
     The sub class test case should not have @Runwith and @ContextConfiguration annotations
    
     2) Create a test suite with
     @Runwith (Suite.class)
     @Suiteclasses({A.class, B.class})
     Where A.class and B.class are the individual unit tests to be run with a spring context
     now the the context will be loaded and Autowire will work .

     Above way worked for me . Just wanted to share . 
     */
}
