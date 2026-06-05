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

package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

class CaseInsensitiveStringTest {
    @Test
    public void shouldIgnoreCaseInEquals() {
        CaseInsensitiveString name = cis("someName");
        assertThat(name).isEqualTo(cis("someName"));
        assertThat(name).isEqualTo(cis("SOMENAME"));
        assertThat(name).isNotEqualTo(cis("SOMECRAP"));
    }

    @Test
    public void shouldUnderstandBlankString() {
        assertThat(cis("someName").isEmpty()).isFalse();
        assertThat(cis(null).isEmpty()).isTrue();
        assertThat(cis("").isEmpty()).isTrue();
        assertThat(cis(" ").isEmpty()).isFalse();
    }

    @Test
    public void shouldClone() {
        CaseInsensitiveString foo = cis("foo");
        Object fooClone = foo.clone();
        assertThat(foo).isEqualTo(fooClone);
        assertThat(foo).isNotSameAs(fooClone);
    }

    @Test
    public void shouldCompare() {
        CaseInsensitiveString foo = cis("foo");
        CaseInsensitiveString fOO = cis("fOO");
        CaseInsensitiveString bar = cis("bar");
        assertThat(foo.compareTo(fOO)).isEqualTo(0);
        assertThat(fOO.compareTo(foo)).isEqualTo(0);
        assertThat(bar.compareTo(foo)).isLessThan(0);
        assertThat(bar.compareTo(fOO)).isLessThan(0);
        assertThat(foo.compareTo(bar)).isGreaterThan(0);
        assertThat(fOO.compareTo(bar)).isGreaterThan(0);
    }

    @Test
    public void shouldUnderstandCase() {
        assertThat(cis("foo").toUpper()).isEqualTo("FOO");
        assertThat(cis("FOO").toLower()).isEqualTo("foo");
    }

    @Test
    public void shouldReturnNullSafeStringRepresentation() {
        assertThat(CaseInsensitiveString.str(cis("foo"))).isEqualTo("foo");
        assertThat(CaseInsensitiveString.str(cis(""))).isEmpty();
        assertThat(CaseInsensitiveString.str(cis(null))).isNull();
        assertThat(CaseInsensitiveString.str(null)).isNull();
    }
}