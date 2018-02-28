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

package com.thoughtworks.go.plugin.access.elastic.v2;

import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CapabilitiesConverterV2Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private CapabilitiesDTO capabilitiesDTO;
    private CapabilitiesConverterV2 capabilitiesConverter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        capabilitiesConverter = new CapabilitiesConverterV2();
    }

    @Test
    public void fromDTO_shouldConvertToCapabilitiesFromCapabilitiesDTO() {
        when(capabilitiesDTO.supportsStatusReport()).thenReturn(false);
        assertFalse(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsStatusReport());

        when(capabilitiesDTO.supportsStatusReport()).thenReturn(true);
        assertTrue(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsStatusReport());
    }

    @Test
    public void toDTO_shouldErrorOutWithUnSupportedException() {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Does not support conversion of com.thoughtworks.go.plugin.domain.elastic.Capabilities to com.thoughtworks.go.plugin.access.elastic.v2.CapabilitiesDTO.");

        new CapabilitiesConverterV2().toDTO(new Capabilities(false));
    }

}