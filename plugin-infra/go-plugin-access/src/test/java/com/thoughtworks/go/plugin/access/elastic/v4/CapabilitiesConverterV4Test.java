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
package com.thoughtworks.go.plugin.access.elastic.v4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CapabilitiesConverterV4Test {
    @Mock
    private CapabilitiesDTO capabilitiesDTO;
    private CapabilitiesConverterV4 capabilitiesConverter;

    @BeforeEach
    public void setUp() throws Exception {
        capabilitiesConverter = new CapabilitiesConverterV4();
    }

    @Test
    public void fromDTO_shouldConvertToCapabilitiesFromCapabilitiesDTO() {
        when(capabilitiesDTO.supportsStatusReport()).thenReturn(false);
        when(capabilitiesDTO.supportsAgentStatusReport()).thenReturn(false);
        assertFalse(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsPluginStatusReport());
        assertFalse(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsAgentStatusReport());

        when(capabilitiesDTO.supportsStatusReport()).thenReturn(true);
        when(capabilitiesDTO.supportsAgentStatusReport()).thenReturn(true);
        assertTrue(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsPluginStatusReport());
        assertTrue(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsAgentStatusReport());

        when(capabilitiesDTO.supportsStatusReport()).thenReturn(false);
        when(capabilitiesDTO.supportsAgentStatusReport()).thenReturn(true);
        assertFalse(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsPluginStatusReport());
        assertTrue(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsAgentStatusReport());

        when(capabilitiesDTO.supportsStatusReport()).thenReturn(true);
        when(capabilitiesDTO.supportsAgentStatusReport()).thenReturn(false);
        assertTrue(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsPluginStatusReport());
        assertFalse(capabilitiesConverter.fromDTO(capabilitiesDTO).supportsAgentStatusReport());
    }
}
