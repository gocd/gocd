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

package com.thoughtworks.go.server.newsecurity.x509;

import net.sf.ehcache.*;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

import java.security.cert.X509Certificate;

public class CachingSubjectDnX509PrincipalExtractor implements X509PrincipalExtractor {

    private static final String CACHE_NAME = "agentCertificateCache";

    static {
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    }

    private final SubjectDnX509PrincipalExtractor delegate = new SubjectDnX509PrincipalExtractor();

    private final Ehcache cache;

    public CachingSubjectDnX509PrincipalExtractor() {
        this(createCacheIfRequired());
    }

    private CachingSubjectDnX509PrincipalExtractor(Ehcache cache) {
        this.cache = cache;
    }

    private static Ehcache createCacheIfRequired() {
        final CacheManager instance = CacheManager.getInstance();
        synchronized (instance) {
            if (!instance.cacheExists(CACHE_NAME)) {
                instance.addCache(new Cache(cacheConfiguration()));
            }
            return instance.getCache(CACHE_NAME);
        }
    }

    private static CacheConfiguration cacheConfiguration() {
        return new CacheConfiguration(CACHE_NAME, 2048)
                .timeToIdleSeconds(120)
                .timeToLiveSeconds(0)
                .eternal(false)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                .overflowToDisk(false)
                .diskPersistent(false);
    }

    public Ehcache getCache() {
        return cache;
    }

    @Override
    public Object extractPrincipal(X509Certificate cert) {
        try {
            Element element = cache.get(cert);
            if (element != null) {
                return element.getValue();
            }
        } catch (CacheException cacheException) {
            throw new DataRetrievalFailureException("Cache failure: " + cacheException.getMessage());
        }

        final Object principal = delegate.extractPrincipal(cert);
        cache.put(new Element(cert, principal));

        return principal;
    }
}
