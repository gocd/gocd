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

package com.thoughtworks.go.server.security.x509.cache;

import com.thoughtworks.go.server.security.x509.X509UserCache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.security.cert.X509Certificate;


/**
 * Caches <code>User</code> objects using a Spring IoC defined <a
 * href="http://ehcache.sourceforge.net">EHCACHE</a>.
 *
 * @author Luke Taylor
 * @author Ben Alex
 * @deprecated use the X509 preauthenticated  
 * @version $Id: EhCacheBasedX509UserCache.java 2544 2008-01-29 11:50:33Z luke_t $
 */
public class EhCacheBasedX509UserCache implements X509UserCache, InitializingBean {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(EhCacheBasedX509UserCache.class);

    //~ Instance fields ================================================================================================

    private Ehcache cache;

    //~ Methods ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(cache, "cache is mandatory");
    }

    public UserDetails getUserFromCache(X509Certificate userCert) {
        Element element = null;

        try {
            element = cache.get(userCert);
        } catch (CacheException cacheException) {
            throw new DataRetrievalFailureException("Cache failure: " + cacheException.getMessage());
        }

        if (logger.isDebugEnabled()) {
            String subjectDN = "unknown";

            if ((userCert != null) && (userCert.getSubjectDN() != null)) {
                subjectDN = userCert.getSubjectDN().toString();
            }

            logger.debug("X.509 Cache hit. SubjectDN: " + subjectDN);
        }

        if (element == null) {
            return null;
        } else {
            return (UserDetails) element.getValue();
        }
    }

    public void putUserInCache(X509Certificate userCert, UserDetails user) {
        Element element = new Element(userCert, user);

        if (logger.isDebugEnabled()) {
            logger.debug("Cache put: " + userCert.getSubjectDN());
        }

        cache.put(element);
    }

    public void removeUserFromCache(X509Certificate userCert) {
        if (logger.isDebugEnabled()) {
            logger.debug("Cache remove: " + userCert.getSubjectDN());
        }

        cache.remove(userCert);
    }

    public void setCache(Ehcache cache) {
        this.cache = cache;
    }
}
