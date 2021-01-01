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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigMaterialUpdateListenerFactoryTest {
    @Mock
    private SystemEnvironment systemEnvironment;

    @Mock
    ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldCreateCompetingConsumersForSuppliedDependencyMaterialQueue() {
        int numberOfConfigMaterialPostUpdateListeners = 3;

        when(systemEnvironment.getNumberOfConfigMaterialPostUpdateListeners()).thenReturn(numberOfConfigMaterialPostUpdateListeners);

        ConfigMaterialUpdateListenerFactory factory = new ConfigMaterialUpdateListenerFactory(systemEnvironment, configMaterialPostUpdateQueue,
                null, null, null, null, null);
        factory.init();

        verify(configMaterialPostUpdateQueue, new Times(numberOfConfigMaterialPostUpdateListeners)).addListener(any(ConfigMaterialUpdateListener.class));
    }
}
