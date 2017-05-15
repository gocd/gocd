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

package com.thoughtworks.go.server.security.x509;


import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.cert.X509Certificate;


/**
 * Populates the <code>UserDetails</code> associated with the X.509
 * certificate presented by a client.
 * <p>
 * Although the certificate will already have been validated by the web container,
 * implementations may choose to perform additional application-specific checks on
 * the certificate content here. If an implementation chooses to reject the certificate,
 * it should throw a {@link BadCredentialsException}.
 * </p>
 *
 * @author Luke Taylor
 * @version $Id: X509AuthoritiesPopulator.java 2544 2008-01-29 11:50:33Z luke_t $
 * @deprecated
 */
public interface X509AuthoritiesPopulator {
    //~ Methods ========================================================================================================

    /**
     * Obtains the granted authorities for the specified user.<p>May throw any
     * <code>AuthenticationException</code> or return <code>null</code> if the authorities are unavailable.</p>
     *
     * @param userCertificate the X.509 certificate supplied
     * @return the details of the indicated user (at minimum the granted authorities and the username)
     * @throws AuthenticationException if the user details are not available or the certificate isn't valid for the
     *                                 application's purpose.
     */
    UserDetails getUserDetails(X509Certificate userCertificate)
            throws AuthenticationException;
}
