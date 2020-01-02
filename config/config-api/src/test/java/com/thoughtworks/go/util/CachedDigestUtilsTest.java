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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class CachedDigestUtilsTest {

    @Test
    public void shouldComputeForAGiveStringUsing256SHA() {
        String fingerprint = "Some String";
        String computeMD5 = CachedDigestUtils.sha256Hex(fingerprint);
        assertThat(computeMD5, is(DigestUtils.sha256Hex(fingerprint)));
    }

    @Test
    public void shouldComputeForAnEmptyStringUsing256SHA() {
        String fingerprint = "";
        String computeMD5 = CachedDigestUtils.sha256Hex(fingerprint);
        assertThat(computeMD5, is(DigestUtils.sha256Hex(fingerprint)));
    }

    @Test
    public void shouldComputeForAGiveStringUsingMD5() {
        String fingerprint = "Some String";
        String computeMD5 = CachedDigestUtils.md5Hex(fingerprint);
        assertThat(computeMD5, is(DigestUtils.md5Hex(fingerprint)));
    }

    @Test
    public void shouldComputeForAnEmptyStringUsingMD5() {
        String fingerprint = "";
        String computeMD5 = CachedDigestUtils.md5Hex(fingerprint);
        assertThat(computeMD5, is(DigestUtils.md5Hex(fingerprint)));
    }

    @Test
    public void shouldComputeMD5ForAGiveString() throws IOException {
        byte[] testData = new byte[1024 * 1024];
        new Random().nextBytes(testData);
        assertThat(DigestUtils.md5Hex(testData),
                is(CachedDigestUtils.md5Hex(new ByteArrayInputStream(testData))));

    }
}
