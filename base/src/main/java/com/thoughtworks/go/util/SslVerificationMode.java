/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public enum SslVerificationMode {
    FULL(new DefaultHostnameVerifier(), null),
    NONE(NoopHostnameVerifier.INSTANCE, new TrustEverythingStrategy()),
    NO_VERIFY_HOST(NoopHostnameVerifier.INSTANCE, null);

    private HostnameVerifier verifier;
    private final TrustStrategy trustStrategy;


    SslVerificationMode(HostnameVerifier verifier, TrustStrategy trustStrategy) {
        this.verifier = verifier;
        this.trustStrategy = trustStrategy;
    }

    public HostnameVerifier verifier() {
        return verifier;
    }

    public TrustStrategy trustStrategy() {
        return trustStrategy;
    }

    private static class TrustEverythingStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return true;
        }
    }

}
