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
package com.thoughtworks.go.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.thoughtworks.go.util.pool.DigestObjectPools;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;

import java.io.InputStream;

/**
 * A replacement for org.apache.commons.codec.digest.DigestUtils , with the MessageDigest Instance cached.
 * <p/>
 * This is to avoid the block on the call below which causes performance bottle necks.
 * java.security.Provider.getService(String, String)
 * <p/>
 * http://groups.google.com/group/spymemcached/browse_thread/thread/146bd39fa95cce6a
 */
public class CachedDigestUtils {

    private static final int STREAM_BUFFER_LENGTH = 1024;
    private static final DigestObjectPools objectPools = new DigestObjectPools();
    private static final Cache<String, String> sha256DigestCache = CacheBuilder.
            newBuilder().
            maximumSize(1024).
            build();
    private static final Cache<String, String> sha512DigestCache = CacheBuilder.
            newBuilder().
            maximumSize(1024).
            build();

    @SneakyThrows // compute() doesn't throw checked exceptions
    public static String sha512_256Hex(String data) {
        return sha512DigestCache.get(data, () -> compute(data, DigestObjectPools.SHA_512_256));
    }

    @SneakyThrows // compute() doesn't throw checked exceptions
    public static String sha256Hex(String string) {
        return sha256DigestCache.get(string, () -> compute(string, DigestObjectPools.SHA_256));
    }

    public static String md5Hex(String string) {
        return compute(string, DigestObjectPools.MD5);
    }

    public static String md5Hex(final InputStream data) {
        return objectPools.computeDigest(DigestObjectPools.MD5, digest -> {
            byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
            int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

            while (read > -1) {
                digest.update(buffer, 0, read);
                read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
            }
            return Hex.encodeHexString(digest.digest());
        });
    }

    private static String compute(final String string, String algorithm) {
        return objectPools.computeDigest(algorithm, digest -> {
            digest.update(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(string));
            return Hex.encodeHexString(digest.digest());
        });
    }
}
