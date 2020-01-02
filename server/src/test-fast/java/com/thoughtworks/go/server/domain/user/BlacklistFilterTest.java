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
package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistFilterTest {
    @Test
    void isPipelineVisible() {
        final BlacklistFilter f = new BlacklistFilter(null, CaseInsensitiveString.list("p1"), null);
        assertFalse(f.isPipelineVisible(new CaseInsensitiveString("p1")));
        assertTrue(f.isPipelineVisible(new CaseInsensitiveString("p0")));
    }

    @Test
    void allowPipeline() {
        final BlacklistFilter f = new BlacklistFilter(null, CaseInsensitiveString.list("p1"), null);
        assertFalse(f.isPipelineVisible(new CaseInsensitiveString("p1")));
        assertTrue(f.isPipelineVisible(new CaseInsensitiveString("p0")));

        f.allowPipeline(new CaseInsensitiveString("p1"));
        assertTrue(f.isPipelineVisible(new CaseInsensitiveString("p1")));
    }

    @Test
    void equals() {
        final BlacklistFilter a = new BlacklistFilter(null, CaseInsensitiveString.list("p1"), null);
        final BlacklistFilter b = new BlacklistFilter(null, CaseInsensitiveString.list("p1"), null);
        final BlacklistFilter c = new BlacklistFilter(null, CaseInsensitiveString.list("p0"), null);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
