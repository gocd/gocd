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
package com.thoughtworks.go.plugins;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @understands: PluginTestUtil
 */
public class PluginTestUtil {
    public static BundleContext bundleCtxWithHeaders(Map headerMap) {
        BundleContext bundleCtx = mock(BundleContext.class);
        Bundle bundle = bundleWithHeaders(headerMap);
        when(bundleCtx.getBundle()).thenReturn(bundle);
        return bundleCtx;
    }

    public static Bundle bundleWithHeaders(Map headerMap, String symbolicName) {
        Bundle bundle = mock(Bundle.class);
        Hashtable<String, String> headers = new Hashtable<>();
        headers.putAll(headerMap);
        when(bundle.getHeaders()).thenReturn(headers);
        when(bundle.getSymbolicName()).thenReturn(symbolicName);
        return bundle;
    }

    public static Bundle bundleWithHeaders(Map headerMap) {
        return bundleWithHeaders(headerMap, "dummy-bundle");
    }
}
