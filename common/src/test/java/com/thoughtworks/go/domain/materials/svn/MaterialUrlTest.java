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
package com.thoughtworks.go.domain.materials.svn;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

public class MaterialUrlTest {

    @Test
    public void shouldIgnoreTrailingSlash() {
        assertThat(new MaterialUrl("http://somehost/"), is((new MaterialUrl("http://somehost"))));
        assertThat(new MaterialUrl("http://somehost"), is((new MaterialUrl("http://somehost/"))));
    }

    @Test
    public void shouldDecodeSpecialCharacters() {
        assertThat(new MaterialUrl("http://somehost/program files"), is((new MaterialUrl("http://somehost/program%20files"))));
        assertThat(new MaterialUrl("http://somehost/program%20files"), is((new MaterialUrl("http://somehost/program files"))));
        assertThat(new MaterialUrl("http://somehost/sv@n/"), is(new MaterialUrl("http://somehost/sv%40n")));
    }

    @Test
    public void shouldDecodeAndRemoveTrailingSlash() {
        assertThat(new MaterialUrl("http://somehost/program files/"), is((new MaterialUrl("http://somehost/program%20files"))));
    }

    @Test
    public void shouldIgnoreTheFileProtocol() {
        assertThat(new MaterialUrl("file:///somefile/Program files/"), is((new MaterialUrl("/somefile/Program files/"))));
        assertThat(new MaterialUrl("FilE:///somefile/Program files/"), is((new MaterialUrl("/somefile/Program files/"))));
    }
}
