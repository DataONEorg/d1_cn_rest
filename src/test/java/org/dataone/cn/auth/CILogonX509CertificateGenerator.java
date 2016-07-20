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

package org.dataone.cn.auth;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.springframework.stereotype.Service;

/**
 *
 * @author waltz
 */
@Service("ciLogonX509CertificateGenerator")
public class CILogonX509CertificateGenerator extends AbstractX509CertificateGenerator {

    public CILogonX509CertificateGenerator() {
        super();
    }

    protected X500NameBuilder getMockCANameBuilder() {

        X500NameStyle template = RFC4519Style.INSTANCE;
        X500NameBuilder nameBuilder = new X500NameBuilder(template);
        nameBuilder.addRDN(RFC4519Style.dc, "org");
        nameBuilder.addRDN(RFC4519Style.dc, "cilogon");
        nameBuilder.addRDN(RFC4519Style.c, "US");
        nameBuilder.addRDN(RFC4519Style.o, "Test");
        nameBuilder.addRDN(RFC4519Style.cn, "Mock CIlogon Root CA");

        return nameBuilder;
    }

    protected X500NameBuilder getMockDNBuilder(String subject) {

        X500NameStyle template = RFC4519Style.INSTANCE;
        X500NameBuilder nameBuilder = new X500NameBuilder(template);
        nameBuilder.addRDN(RFC4519Style.dc, "org");
        nameBuilder.addRDN(RFC4519Style.dc, "cilogon");
        nameBuilder.addRDN(RFC4519Style.c, "US");
        nameBuilder.addRDN(RFC4519Style.o, "Test");
        nameBuilder.addRDN(RFC4519Style.cn, subject);

        return nameBuilder;
    }
}
