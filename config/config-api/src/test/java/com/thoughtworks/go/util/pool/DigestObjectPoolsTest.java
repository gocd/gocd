/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util.pool;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DigestObjectPoolsTest {

    private DigestObjectPools pools;

    @BeforeEach
    public void setUp() throws Exception {
        pools = new DigestObjectPools();
        pools.clearThreadLocals();

    }

    @AfterEach
    public void tearDown() throws Exception {
        pools.clearThreadLocals();
    }

    @Test
    public void shouldCallPerformActionWithADigest() throws IOException {
        DigestObjectPools.DigestOperation operation = mock(DigestObjectPools.DigestOperation.class);
        pools.computeDigest(DigestObjectPools.SHA_256, operation);
        verify(operation).perform(any(MessageDigest.class));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenTheAlgorithmIsUnknown() throws IOException {
        DigestObjectPools.DigestOperation operation = mock(DigestObjectPools.DigestOperation.class);
        try {
            pools.computeDigest("upside_down_fred_rubble_bubble_cake", operation);
            fail("Expected to get an exception as the algorithm is Flintstones proprietary!");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), is("Algorithm not supported"));
        }
    }

    @Test
    public void shouldResetDigestForFutureUsage() {
        DigestObjectPools.DigestOperation operation = digest -> {
            digest.update(org.apache.commons.codec.binary.StringUtils.getBytesUtf8("foo"));
            return Hex.encodeHexString(digest.digest());
        };
        String shaFirst = pools.computeDigest(DigestObjectPools.SHA_256, operation);
        String shaSecond = pools.computeDigest(DigestObjectPools.SHA_256, operation);
        assertThat(shaFirst, is(shaSecond));
    }

    @Test
    public void shouldCreateDigestOnlyIfItIsNotAlreadyInitializedOnThisThreadsThreadLocal() throws NoSuchAlgorithmException {
        DigestObjectPools.CreateDigest creator = mock(DigestObjectPools.CreateDigest.class);
        when(creator.create(DigestObjectPools.SHA_256)).thenReturn(MessageDigest.getInstance(DigestObjectPools.SHA_256));
        DigestObjectPools pools = new DigestObjectPools(creator);
        try {
            DigestObjectPools.DigestOperation operation = mock(DigestObjectPools.DigestOperation.class);

            pools.computeDigest(DigestObjectPools.SHA_256, operation);
            pools.computeDigest(DigestObjectPools.SHA_256, operation);

            verify(creator).create(DigestObjectPools.SHA_256);
            verifyNoMoreInteractions(creator);
        } finally {
            pools.clearThreadLocals();
        }
    }

    @Test
    public void shouldCreateDigestOnlyIfItIsNotAlreadyInitializedOnThreads() throws NoSuchAlgorithmException, InterruptedException {
        DigestObjectPools.CreateDigest creator = mock(DigestObjectPools.CreateDigest.class);
        when(creator.create(DigestObjectPools.SHA_256)).thenReturn(MessageDigest.getInstance(DigestObjectPools.SHA_256));
        final DigestObjectPools pools = new DigestObjectPools(creator);
        try {
            final DigestObjectPools.DigestOperation operation = mock(DigestObjectPools.DigestOperation.class);

            pools.computeDigest(DigestObjectPools.SHA_256, operation);
            pools.computeDigest(DigestObjectPools.SHA_256, operation);

            Thread thread = new Thread(() -> {
                pools.computeDigest(DigestObjectPools.SHA_256, operation);
                pools.computeDigest(DigestObjectPools.SHA_256, operation);
            });
            thread.start();
            thread.join();
            verify(creator, times(2)).create(DigestObjectPools.SHA_256);
            verifyNoMoreInteractions(creator);
        } finally {
            pools.clearThreadLocals();
        }
    }
}
