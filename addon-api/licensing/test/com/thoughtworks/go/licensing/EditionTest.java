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

package com.thoughtworks.go.licensing;

import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EditionTest {
    @Test
    public void displayNameForFreeLicenseShouldBe_Community_() {
        assertThat(Edition.Free.getDisplayName(), is("Community"));
    }

    @Test
    public void displayNameForAllLicenseTypesShouldBeTheSameAsCorrespondingNames() {
        for (Edition edition : Edition.values()) {
            if (edition != Edition.Free && edition != Edition.OpenSource) {
                assertThat(edition.getDisplayName(), is(edition.toString()));
            }
            assertThat(Edition.valueOf(edition.toString()), sameInstance(edition));//config versioning uses the string representation and x-lates it back to Edition - ShilpaG & JJ
        }
    }

    @Test
    public void shouldUnderstandEnterpriziness() {
        for (Edition edition : Edition.values()) {
            assertThat(edition.isEnterprise(), is(a(Edition.Enterprise, Edition.Professional).contains(edition)));
        }
    }
}
