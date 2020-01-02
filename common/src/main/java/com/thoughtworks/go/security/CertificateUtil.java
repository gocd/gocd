/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.security;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class CertificateUtil {
    public static String md5Fingerprint(X509Certificate certificate) {
        try {
            return DigestUtils.sha256Hex(certificate.getEncoded());
        } catch (GeneralSecurityException gse) {
            throw bomb(gse);
        }
    }
}
