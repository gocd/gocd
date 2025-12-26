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

import static org.assertj.core.api.Assertions.assertThat;

class CaseInsensitiveStringTest {
    @Test
    public void shouldIgnoreCaseInEquals() {
        CaseInsensitiveString name = new CaseInsensitiveString("someName");
        assertThat(name).isEqualTo(new CaseInsensitiveString("someName"));
        assertThat(name).isEqualTo(new CaseInsensitiveString("SOMENAME"));
        assertThat(name).isNotEqualTo(new CaseInsensitiveString("SOMECRAP"));
    }

    @Test
    public void shouldUnderstandBlankString() {
        assertThat(new CaseInsensitiveString("someName").isEmpty()).isFalse();
        assertThat(new CaseInsensitiveString(null).isEmpty()).isTrue();
        assertThat(new CaseInsensitiveString("").isEmpty()).isTrue();
        assertThat(new CaseInsensitiveString(" ").isEmpty()).isFalse();
    }

    @Test
    public void shouldClone() {
        CaseInsensitiveString foo = new CaseInsensitiveString("foo");
        Object fooClone = foo.clone();
        assertThat(foo).isEqualTo(fooClone);
        assertThat(foo).isNotSameAs(fooClone);
    }

    @Test
    public void shouldCompare() {
        CaseInsensitiveString foo = new CaseInsensitiveString("foo");
        CaseInsensitiveString fOO = new CaseInsensitiveString("fOO");
        CaseInsensitiveString bar = new CaseInsensitiveString("bar");
        assertThat(foo.compareTo(fOO)).isEqualTo(0);
        assertThat(fOO.compareTo(foo)).isEqualTo(0);
        assertThat(bar.compareTo(foo)).isLessThan(0);
        assertThat(bar.compareTo(fOO)).isLessThan(0);
        assertThat(foo.compareTo(bar)).isGreaterThan(0);
        assertThat(fOO.compareTo(bar)).isGreaterThan(0);
    }

    @Test
    public void shouldUnderstandCase() {
        assertThat(new CaseInsensitiveString("foo").toUpper()).isEqualTo("FOO");
        assertThat(new CaseInsensitiveString("FOO").toLower()).isEqualTo("foo");
    }

    @Test
    public void shouldReturnNullSafeStringRepresentation() {
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString("foo"))).isEqualTo("foo");
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString(""))).isEmpty();
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString(null))).isNull();
        assertThat(CaseInsensitiveString.str(null)).isNull();
    }
}