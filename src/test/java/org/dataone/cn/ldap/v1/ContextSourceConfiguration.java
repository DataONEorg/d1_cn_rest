package org.dataone.cn.ldap.v1;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.naming.NamingException;
import org.dataone.configuration.Settings;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 *
 * @author waltz
 */
public class ContextSourceConfiguration {

    LdapContextSource ldapContextSource;
    // look up defaults from configuration
    private String server = Settings.getConfiguration().getString("restTest.ldap.server");
    private String admin = Settings.getConfiguration().getString("restTest.ldap.admin");
    private String password = Settings.getConfiguration().getString("restTest.ldap.password");
    private String base = Settings.getConfiguration().getString("restTest.ldap.base");

    public ContextSourceConfiguration() throws NamingException {
        ldapContextSource = new LdapContextSource();
        ldapContextSource.setDirObjectFactory(DefaultDirObjectFactory.class);
        ldapContextSource.setUrl(server);
        ldapContextSource.setBase(base);
        ldapContextSource.setUserDn(admin);
        ldapContextSource.setPassword(password);
    }

    public LdapContextSource getLdapContextSource() {
        return ldapContextSource;
    }
}