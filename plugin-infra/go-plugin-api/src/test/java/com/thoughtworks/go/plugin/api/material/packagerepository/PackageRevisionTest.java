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
package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.material.packagerepository.exceptions.InvalidPackageRevisionDataException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class PackageRevisionTest {

    @Test
    public void shouldAcceptDataKeyMadeUpOfAlpahNumericAndUnderScoreCharacters() throws Exception {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");
        packageRevision.addData("HELLO_WORLD123", "value");
        assertThat(packageRevision.getDataFor("HELLO_WORLD123"), is("value"));
    }

    @Test
    public void shouldThrowExceptionWhenDataKeyIsNullOrEmpty() throws Exception {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");
        try {
            packageRevision.addData(null, "value");
        } catch (InvalidPackageRevisionDataException e) {
            assertThat(e.getMessage(), is("Key names cannot be null or empty."));
        }
        try {
            packageRevision.addData("", "value");
        } catch (InvalidPackageRevisionDataException e) {
            assertThat(e.getMessage(), is("Key names cannot be null or empty."));
        }
    }

    @Test
    public void shouldThrowExceptionIfDataKeyContainsCharactersOtherThanAlphaNumericAndUnderScoreCharacters() throws Exception {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");
        try {
            packageRevision.addData("HEL-LO-WORLD", "value");
            fail("should have thrown exception");
        } catch (InvalidPackageRevisionDataException e) {
            assertThat(e.getMessage(), is("Key 'HEL-LO-WORLD' is invalid. Key names should consists of only alphanumeric characters and/or underscores."));
        }
    }

    @Test
    public void shouldNotAllowDataWhenKeyIsInvalid() throws Exception {
        assertForInvalidKey("", "Key names cannot be null or empty.");
        assertForInvalidKey("!key", "Key '!key' is invalid. Key names should consists of only alphanumeric characters and/or underscores.");
    }

    private void assertForInvalidKey(String key, String expectedMessage) {
        HashMap<String, String> data = new HashMap<>();
        data.put(key, "value");
        try {
            new PackageRevision(null, null, null, data);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(expectedMessage));
        }
    }
}
