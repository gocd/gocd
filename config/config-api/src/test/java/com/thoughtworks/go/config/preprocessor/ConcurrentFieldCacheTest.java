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

package com.thoughtworks.go.config.preprocessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentFieldCacheTest {

    @Test
    public void shouldFindRegularFields() {
        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(FooBar.class)).hasSize(3);
    }

    @Test
    public void shouldFindSuperclassFields() {
        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(DerivedFooBar.class)).hasSize(4);
    }

    @Test
    public void shouldExcludeSynthetics() {
        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(DerivedFooBar.InnerClass.class)).hasSize(0);
    }

    @Test
    public void shouldExcludeStatics() {
        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(StaticFieldFoo.class)).hasSize(1);
    }

    @Test
    public void shouldAllowReset() {
        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(this.getClass())).isEmpty();
        assertThat(ConcurrentFieldCache.size()).isNotZero();

        ConcurrentFieldCache.reset();
        assertThat(ConcurrentFieldCache.size()).isZero();

        assertThat(ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(this.getClass())).isEmpty();
        assertThat(ConcurrentFieldCache.size()).isEqualTo(1);
    }
}

class FooBar {
    @SuppressWarnings("unused") private String value;
    @SuppressWarnings("unused") private String data;
    @SuppressWarnings("unused") protected String moreData;
}

class DerivedFooBar extends FooBar {
    @SuppressWarnings("unused") protected String derivedClassData;

    @SuppressWarnings("InnerClassMayBeStatic")
    class InnerClass {
    }
}

class StaticFieldFoo {
    @SuppressWarnings("unused") private static String value;
    @SuppressWarnings("unused") private static final String valueFinal = "";
    @SuppressWarnings("unused") public static final String valuePublicFinal = "";
    @SuppressWarnings("unused") private String data;
}

