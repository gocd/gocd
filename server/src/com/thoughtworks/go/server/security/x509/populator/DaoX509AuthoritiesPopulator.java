/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.security.x509.populator;

import com.thoughtworks.go.server.security.x509.X509AuthoritiesPopulator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Populates the X509 authorities via an {@link UserDetailsService}.
 *
 * @author Luke Taylor
 * @deprecated This package is now deprecated. Use the X.509 authentication support in
 *              org.springframework.security.ui.preauth.x509 instead.
 * @version $Id: DaoX509AuthoritiesPopulator.java 2544 2008-01-29 11:50:33Z luke_t $
 */
public class DaoX509AuthoritiesPopulator implements X509AuthoritiesPopulator, InitializingBean, MessageSourceAware {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(DaoX509AuthoritiesPopulator.class);

    //~ Instance fields ================================================================================================

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private Pattern subjectDNPattern;
    private String subjectDNRegex = "CN=(.*?),";
    private UserDetailsService userDetailsService;

    //~ Methods ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userDetailsService, "An authenticationDao must be set");
        Assert.notNull(this.messages, "A message source must be set");

        subjectDNPattern = Pattern.compile(subjectDNRegex, Pattern.CASE_INSENSITIVE);
    }

    public UserDetails getUserDetails(X509Certificate clientCert) throws AuthenticationException {
        String subjectDN = clientCert.getSubjectDN().getName();

        Matcher matcher = subjectDNPattern.matcher(subjectDN);

        if (!matcher.find()) {
            throw new BadCredentialsException(messages.getMessage("DaoX509AuthoritiesPopulator.noMatching",
                    new Object[] {subjectDN}, "No matching pattern was found in subjectDN: {0}"));
        }

        if (matcher.groupCount() != 1) {
            throw new IllegalArgumentException("Regular expression must contain a single group ");
        }

        String userName = matcher.group(1);

        UserDetails user = this.userDetailsService.loadUserByUsername(userName);

        if (user == null) {
            throw new AuthenticationServiceException(
                "UserDetailsService returned null, which is an interface contract violation");
        }

        return user;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * Sets the regular expression which will by used to extract the user name from the certificate's Subject
     * DN.
     * <p>It should contain a single group; for example the default expression "CN=(.?)," matches the common
     * name field. So "CN=Jimi Hendrix, OU=..." will give a user name of "Jimi Hendrix".</p>
     * <p>The matches are case insensitive. So "emailAddress=(.?)," will match "EMAILADDRESS=jimi@hendrix.org,
     * CN=..." giving a user name "jimi@hendrix.org"</p>
     *
     * @param subjectDNRegex the regular expression to find in the subject
     */
    public void setSubjectDNRegex(String subjectDNRegex) {
        this.subjectDNRegex = subjectDNRegex;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
}
