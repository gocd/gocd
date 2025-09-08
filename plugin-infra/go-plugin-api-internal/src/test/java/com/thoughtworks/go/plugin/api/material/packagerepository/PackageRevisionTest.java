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
package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.material.packagerepository.exceptions.InvalidPackageRevisionDataException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PackageRevisionTest {

    @Test
    public void shouldAcceptDataKeyMadeUpOfAlphaNumericAndUnderScoreCharacters() {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");
        packageRevision.addData("HELLO_WORLD123", "value");
        assertThat(packageRevision.getDataFor("HELLO_WORLD123")).isEqualTo("value");
    }

    @Test
    public void shouldThrowExceptionWhenDataKeyIsNullOrEmpty() {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");

        assertThatThrownBy(() -> packageRevision.addData(null, "value"))
                .isInstanceOf(InvalidPackageRevisionDataException.class)
                .hasMessage("Key names cannot be null or empty.");

        assertThatThrownBy(() -> packageRevision.addData("", "value"))
                .isInstanceOf(InvalidPackageRevisionDataException.class)
                .hasMessage("Key names cannot be null or empty.");
    }

    @Test
    public void shouldThrowExceptionIfDataKeyContainsCharactersOtherThanAlphaNumericAndUnderScoreCharacters() {
        PackageRevision packageRevision = new PackageRevision("rev123", new Date(), "loser");

        assertThatThrownBy(() -> packageRevision.addData("HEL-LO-WORLD", "value"))
                .isInstanceOf(InvalidPackageRevisionDataException.class)
                .hasMessage("Key 'HEL-LO-WORLD' is invalid. Key names should consists of only alphanumeric characters and/or underscores.");
    }

    @Test
    public void shouldNotAllowDataWhenKeyIsInvalid() {
        assertForInvalidKey("", "Key names cannot be null or empty.");
        assertForInvalidKey("!key", "Key '!key' is invalid. Key names should consists of only alphanumeric characters and/or underscores.");
    }

    private void assertForInvalidKey(String key, String expectedMessage) {
        Map<String, String> data = new HashMap<>();
        data.put(key, "value");

        assertThatThrownBy(() -> new PackageRevision(null, null, null, data))
                .isInstanceOf(InvalidPackageRevisionDataException.class)
                .hasMessage(expectedMessage);
    }
}
