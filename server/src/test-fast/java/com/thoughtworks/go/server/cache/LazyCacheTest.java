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
package com.thoughtworks.go.server.cache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LazyCacheTest {

    @Test
    public void shouldGetValueFromCacheIfPresent() {
        Object valueInCache = new Object();

        Ehcache ehcache = mock(Ehcache.class);
        when(ehcache.get("foo")).thenReturn(new Element("foo", valueInCache));

        Supplier supplier = mock(Supplier.class);
        assertThat(new LazyCache(ehcache, null).get("foo", supplier)).isSameAs(valueInCache);
        verifyZeroInteractions(supplier);
    }

    @Test
    public void shouldComputeValueFromSupplierIfNotPresentInCache() {
        Ehcache ehcache = mock(Ehcache.class);
        when(ehcache.get("foo")).thenReturn(null);

        Object lazilyComputedValue = new Object();

        Supplier supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(lazilyComputedValue);
        assertThat(new LazyCache(ehcache, null).get("foo", supplier)).isSameAs(lazilyComputedValue);
        verify(supplier, times(1)).get();

        verify(ehcache, times(2)).get("foo");
        verify(ehcache, times(1)).put(new Element("foo", lazilyComputedValue));
    }
}
