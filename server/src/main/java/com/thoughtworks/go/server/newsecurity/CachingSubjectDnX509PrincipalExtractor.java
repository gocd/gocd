/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;

@Component
public class CachingSubjectDnX509PrincipalExtractor implements X509PrincipalExtractor {
    private final SubjectDnX509PrincipalExtractor delegate = new SubjectDnX509PrincipalExtractor();
    private static final Logger LOGGER = LoggerFactory.getLogger(CachingSubjectDnX509PrincipalExtractor.class);

    private final Cache cache;

    @Autowired
    public CachingSubjectDnX509PrincipalExtractor(@Qualifier("userCache") Cache cache) {
        this.cache = cache;
    }

    @Override
    public Object extractPrincipal(X509Certificate cert) {
        Element element = null;

        try {
            element = cache.get(cert);
        } catch (CacheException cacheException) {
            throw new DataRetrievalFailureException("Cache failure: " + cacheException.getMessage());
        }

        if (LOGGER.isDebugEnabled()) {
            String subjectDN = "unknown";

            if ((cert != null) && (cert.getSubjectDN() != null)) {
                subjectDN = cert.getSubjectDN().toString();
            }

            LOGGER.debug("X.509 Cache hit. SubjectDN: {}", subjectDN);
        }

        if (element == null) {
            element = new Element(cert, delegate.extractPrincipal(cert));
            cache.put(element);
        }

        return element.getValue();
    }


}
