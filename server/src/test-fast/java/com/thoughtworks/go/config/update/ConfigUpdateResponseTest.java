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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ConfigAwareUpdate;
import com.thoughtworks.go.config.ConfigSaveState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ConfigUpdateResponseTest {
    @Test
    public void shouldReturnWasMergedIfConfigSaveStateIsMerged() throws Exception {
        ConfigUpdateResponse configUpdateResponse = new ConfigUpdateResponse(null, null, null, mock(ConfigAwareUpdate.class), ConfigSaveState.MERGED);
        assertTrue(configUpdateResponse.wasMerged());
    }
    
    @Test
    public void shouldNOTReturnWasMergedIfConfigSaveStateIsUpdated() throws Exception {
        ConfigUpdateResponse configUpdateResponse = new ConfigUpdateResponse(null, null, null, mock(ConfigAwareUpdate.class), ConfigSaveState.UPDATED);
        assertFalse(configUpdateResponse.wasMerged());
    }

    @Test
    public void shouldNOTReturnWasMergedIfConfigSaveStateIsNull() throws Exception {
        ConfigUpdateResponse configUpdateResponse = new ConfigUpdateResponse(null, null, null, mock(ConfigAwareUpdate.class), null);
        assertFalse(configUpdateResponse.wasMerged());
    }
}
