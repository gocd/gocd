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

package com.thoughtworks.go.plugin.access.elastic.v1;

import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;

public class CapabilitiesConverterV1Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void fromDTO_shouldAlwaysReturnCapabilitiesWithSupportStatusReportFalse() {
        final Capabilities capabilities = new CapabilitiesConverterV1().fromDTO(new CapabilitiesDTO());
        assertFalse(capabilities.supportsStatusReport());
    }

    @Test
    public void toDTO_shouldErrorOutWithUnSupportedException() {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Does not support conversion of com.thoughtworks.go.plugin.domain.elastic.Capabilities to com.thoughtworks.go.plugin.access.elastic.v1.CapabilitiesDTO.");

        new CapabilitiesConverterV1().toDTO(new Capabilities(false));
    }
}