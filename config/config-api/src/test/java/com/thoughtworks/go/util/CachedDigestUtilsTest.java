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

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Random;

import static com.thoughtworks.go.util.CachedDigestUtils.*;
import static org.junit.Assert.assertEquals;

public class CachedDigestUtilsTest {
    @Test
    public void shouldComputeForAGivenStringUsingSHA_512_256() {
        String fingerprint = "Some String";
        String digest = sha512_256Hex(fingerprint);
        assertEquals(DigestUtils.sha512_256Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAnEmptyStringUsingSHA_512_256() {
        String fingerprint = "";
        String digest = sha512_256Hex(fingerprint);
        assertEquals(DigestUtils.sha512_256Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAGivenStringUsingSHA_256() {
        String fingerprint = "Some String";
        String digest = sha256Hex(fingerprint);
        assertEquals(DigestUtils.sha256Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAnEmptyStringUsingSHA_256() {
        String fingerprint = "";
        String digest = sha256Hex(fingerprint);
        assertEquals(DigestUtils.sha256Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAGivenStringUsingMD5() {
        String fingerprint = "Some String";
        String digest = md5Hex(fingerprint);
        assertEquals(DigestUtils.md5Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAnEmptyStringUsingMD5() {
        String fingerprint = "";
        String digest = md5Hex(fingerprint);
        assertEquals(DigestUtils.md5Hex(fingerprint), digest);
    }

    @Test
    public void shouldComputeForAGivenStreamUsingMD5() {
        byte[] testData = new byte[1024 * 1024];
        new Random().nextBytes(testData);
        assertEquals(DigestUtils.md5Hex(testData), md5Hex(new ByteArrayInputStream(testData)));
    }
}
