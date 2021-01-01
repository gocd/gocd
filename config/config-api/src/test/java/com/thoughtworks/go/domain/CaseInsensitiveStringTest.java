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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CaseInsensitiveStringTest {

    @Test
    public void shouldIgnoreCaseInEquals() {
        CaseInsensitiveString name = new CaseInsensitiveString("someName");
        assertThat(name, is(new CaseInsensitiveString("someName")));
        assertThat(name, is(new CaseInsensitiveString("SOMENAME")));
        assertThat(name, not(new CaseInsensitiveString("SOMECRAP")));
    }

    @Test
    public void shouldUnderstandBlankString() {
        assertThat(new CaseInsensitiveString("someName").isBlank(), is(false));
        assertThat(new CaseInsensitiveString(null).isBlank(), is(true));
        assertThat(new CaseInsensitiveString("").isBlank(), is(true));
        assertThat(new CaseInsensitiveString(" ").isBlank(), is(false));
    }

    @Test
    public void shouldClone() throws Exception {
        CaseInsensitiveString foo = new CaseInsensitiveString("foo");
        CaseInsensitiveString fooClone = (CaseInsensitiveString) ReflectionUtil.invoke(foo, "clone");
        assertThat(foo, is(fooClone));
        assertThat(foo, not(sameInstance(fooClone)));
    }

    @Test
    public void shouldCompare() {
        CaseInsensitiveString foo = new CaseInsensitiveString("foo");
        CaseInsensitiveString fOO = new CaseInsensitiveString("fOO");
        CaseInsensitiveString bar = new CaseInsensitiveString("bar");
        assertThat(foo.compareTo(fOO), is(0));
        assertThat(fOO.compareTo(foo), is(0));
        assertThat(bar.compareTo(foo), lessThan(0));
        assertThat(bar.compareTo(fOO), lessThan(0));
        assertThat(foo.compareTo(bar), greaterThan(0));
        assertThat(fOO.compareTo(bar), greaterThan(0));
    }

    @Test
    public void shouldUnderstandCase() {
        assertThat(new CaseInsensitiveString("foo").toUpper(), is("FOO"));
        assertThat(new CaseInsensitiveString("FOO").toLower(), is("foo"));
    }

    @Test
    public void shouldReturnNullSafeStringRepresentation() {
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString("foo")), is("foo"));
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString("")), is(""));
        assertThat(CaseInsensitiveString.str(new CaseInsensitiveString(null)), nullValue());
        assertThat(CaseInsensitiveString.str(null), nullValue());
    }
}
