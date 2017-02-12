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


import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.cert.X509Certificate;


/**
 * Provides a cache of {@link UserDetails} objects for the
 * {@link X509AuthenticationProvider}.
 * <p>
 * Similar in function to the {@link UserCache}
 * used by the Dao provider, but the cache is keyed with the user's certificate
 * rather than the user name.
 * </p>
 *
 * @author Luke Taylor
 * @deprecated
 * @version $Id: X509UserCache.java 2544 2008-01-29 11:50:33Z luke_t $
 */
public interface X509UserCache {
    //~ Methods ========================================================================================================

    UserDetails getUserFromCache(X509Certificate userCertificate);

    void putUserInCache(X509Certificate key, UserDetails user);

    void removeUserFromCache(X509Certificate key);
}
