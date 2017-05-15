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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.cert.X509Certificate;
import java.util.Collection;


/**
 * <code>Authentication</code> implementation for X.509 client-certificate authentication.
 *
 * @author Luke Taylor
 * @version $Id: X509AuthenticationToken.java 2544 2008-01-29 11:50:33Z luke_t $
 * @deprecated superceded by the preauth provider. Use the X.509 authentication support in org.springframework.security.ui.preauth.x509 instead.
 */
public class X509AuthenticationToken extends AbstractAuthenticationToken {
    //~ Instance fields ================================================================================================

    private static final long serialVersionUID = 1L;
    private Object principal;
    private X509Certificate credentials;

    //~ Constructors ===================================================================================================

    /**
     * Used for an authentication request.  The {@link Authentication#isAuthenticated()} will return
     * <code>false</code>.
     *
     * @param credentials the certificate
     */
    public X509AuthenticationToken(X509Certificate credentials) {
        super(null);
        this.credentials = credentials;
    }

    /**
     * Used for an authentication response object. The {@link Authentication#isAuthenticated()}
     * will return <code>true</code>.
     *
     * @param principal   the principal, which is generally a
     *                    <code>UserDetails</code>
     * @param credentials the certificate
     * @param authorities the authorities
     */
    public X509AuthenticationToken(Object principal, X509Certificate credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    //~ Methods ========================================================================================================

    public Object getCredentials() {
        return credentials;
    }

    public Object getPrincipal() {
        return principal;
    }
}
