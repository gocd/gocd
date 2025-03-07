/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.scm.revision;

import com.thoughtworks.go.plugin.access.scm.exceptions.InvalidSCMRevisionDataException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SCMRevisionTest {
    @Test
    public void shouldAcceptDataKeyMadeUpOfAlphaNumericAndUnderScoreCharacters() {
        SCMRevision scmRevision = new SCMRevision("rev123", new Date(), "loser", null, new HashMap<>(), null);
        scmRevision.addData("HELLO_WORLD123", "value");
        assertThat(scmRevision.getDataFor("HELLO_WORLD123")).isEqualTo("value");
    }

    @Test
    public void shouldThrowExceptionWhenDataKeyIsNullOrEmpty() {
        SCMRevision scmRevision = new SCMRevision("rev123", new Date(), "loser", null, null, null);
        try {
            scmRevision.addData(null, "value");
        } catch (InvalidSCMRevisionDataException e) {
            assertThat(e.getMessage()).isEqualTo("Key names cannot be null or empty.");
        }
        try {
            scmRevision.addData("", "value");
        } catch (InvalidSCMRevisionDataException e) {
            assertThat(e.getMessage()).isEqualTo("Key names cannot be null or empty.");
        }
    }

    @Test
    public void shouldThrowExceptionIfDataKeyContainsCharactersOtherThanAlphaNumericAndUnderScoreCharacters() {
        SCMRevision scmRevision = new SCMRevision("rev123", new Date(), "loser", null, null, null);
        try {
            scmRevision.addData("HEL-LO-WORLD", "value");
            fail("should have thrown exception");
        } catch (InvalidSCMRevisionDataException e) {
            assertThat(e.getMessage()).isEqualTo("Key 'HEL-LO-WORLD' is invalid. Key names should consists of only alphanumeric characters and/or underscores.");
        }
    }

    @Test
    public void shouldNotAllowDataWhenKeyIsInvalid() {
        assertForInvalidKey(null, "Key names cannot be null or empty.");
        assertForInvalidKey("", "Key names cannot be null or empty.");
        assertForInvalidKey("HEL-LO-WORLD", "Key 'HEL-LO-WORLD' is invalid. Key names should consists of only alphanumeric characters and/or underscores.");
    }

    private void assertForInvalidKey(String key, String expectedMessage) {
        Map<String, String> data = new HashMap<>();
        data.put(key, "value");
        try {
            new SCMRevision(null, null, null, null, data, null);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(expectedMessage);
        }
    }
}
