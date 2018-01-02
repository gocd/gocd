/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util.pool;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestObjectPools {

    public static final String SHA_256 = "SHA-256";
    public static final String MD_5 = "MD5";
    private static ThreadLocal<MessageDigest> sha256DigestLocal = new ThreadLocal<>();
    private static ThreadLocal<MessageDigest> md5DigestLocal = new ThreadLocal<>();
    private final CreateDigest createDigest;

    public DigestObjectPools() {
        this(new SimpleCreateDigest());
    }

    public DigestObjectPools(CreateDigest createDigest) {
        this.createDigest = createDigest;
    }

    public String computeDigest(String algorithm, DigestOperation operation) {
        if (!SHA_256.equals(algorithm) && !MD_5.equals(algorithm)) {
            throw new IllegalArgumentException("Algorithm not supported");
        }
        try {
            MessageDigest digest = getDigest(algorithm);
            String result = operation.perform(digest);
            digest.reset();//test passes even without this, but can't see sun impl's source, so playing safe
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute the digest.", e);
        }
    }

    private MessageDigest getDigest(String algorithm) throws Exception {
        ThreadLocal<MessageDigest> messageDigestThreadLocal = get(algorithm);
        MessageDigest digest = messageDigestThreadLocal.get();
        if (digest == null) {
            digest = createDigest.create(algorithm);
            messageDigestThreadLocal.set(digest);
        }
        return digest;
    }

    private ThreadLocal<MessageDigest> get(String algorithm) {
        if (SHA_256.equals(algorithm)) {
            return sha256DigestLocal;
        }
        if (MD_5.equals(algorithm)) {
            return md5DigestLocal;
        }
        throw new IllegalArgumentException("Algorithm not supported");
    }

    public static interface DigestOperation {
        public String perform(MessageDigest digest) throws IOException;
    }

    public static interface CreateDigest {
        public MessageDigest create(String algorithm) throws NoSuchAlgorithmException;
    }

    private static class SimpleCreateDigest implements CreateDigest {

        public MessageDigest create(String algorithm) throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(algorithm);
        }
    }

    /**
     * @deprecated Used only in tests
     */
    void clearThreadLocals() {
        sha256DigestLocal.set(null);
        md5DigestLocal.set(null);
    }
}
