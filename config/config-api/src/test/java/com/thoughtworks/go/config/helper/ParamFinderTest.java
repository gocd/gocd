/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.helper;


import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ParamConfig;
import com.thoughtworks.go.config.ParamsConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParamFinderTest {

    private ParamFinder paramFinder;

    @Before
    public void setup() {
        paramFinder = new ParamFinder();
    }

    @Test
    public void shouldReturnTrueIfAttributeIsAParam() {
        assertTrue(paramFinder.isAttributeAParam("#{param}"));
        assertTrue(paramFinder.isAttributeAParam("#{#{param}}"));
        assertTrue(paramFinder.isAttributeAParam("#{}"));
    }

    @Test
    public void shouldReturnFalseIfAttributeIsNotAParam() {
        assertFalse(paramFinder.isAttributeAParam("abc"));
        assertFalse(paramFinder.isAttributeAParam("pop#{param}"));
    }

    @Test
    public void shouldReturnFalseIfAttributeIsNull() {
        assertFalse(paramFinder.isAttributeAParam((String) null));
    }

    @Test
    public void shouldReturnNullIfParamConfigsIsNull() {
        assertNull(paramFinder.getParamValue(null, "#{param}"));
    }

    @Test
    public void shouldReturnNullIfParamConfigsIsEmpty() {
        assertNull(paramFinder.getParamValue(new ParamsConfig(), "#{param}"));
    }

    @Test
    public void shouldReturnNullIfAttributeDoesNotConformToParamPattern() {
        ParamsConfig paramConfigs = new ParamsConfig(new ParamConfig("foo", "bar"));
        assertNull(paramFinder.getParamValue(paramConfigs, "#{}"));
        assertNull(paramFinder.getParamValue(paramConfigs, "#{#{param}}"));
        assertNull(paramFinder.getParamValue(paramConfigs, "pa#{#p}}"));
        assertNull(paramFinder.getParamValue(paramConfigs, "##{param}"));
        assertNull(paramFinder.getParamValue(paramConfigs, (String) null));
        assertNull(paramFinder.getParamValue(paramConfigs, ""));
    }

    @Test
    public void shouldReturnTheParamValueForAValidAttribute() {
        assertEquals(paramFinder.getParamValue(new ParamsConfig(new ParamConfig("foo", "bar")), "#{foo}"), "bar");
    }
}