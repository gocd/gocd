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

package com.thoughtworks.go.util;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.helper.PartialConfigMother;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
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

    @Test
    public void shouldComputeMd5SumOfObjects() throws Exception {
        PartialConfig serializableObject = PartialConfigMother.withFullBlownPipeline("pipeline");
        byte[] byteArray;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(serializableObject);
                byteArray = byteArrayOutputStream.toByteArray();
            }
        }
        assertThat(DigestUtils.md5Hex(byteArray), is(CachedDigestUtils.md5Hex(serializableObject)));
    }
}
