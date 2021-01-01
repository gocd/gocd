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
package com.thoughtworks.go.junit5;

import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SystemExtension implements AfterEachCallback, BeforeEachCallback, BeforeAllCallback, AfterAllCallback {
    private CurrentState currentState;

    @Override
    public void beforeAll(ExtensionContext context) {
        currentState = new CurrentState();
        System.setProperties(copyOf(currentState.getProperties()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.setProperties(currentState.getProperties());
        updateEnvs(currentState.getEnvironmentVariables());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        currentState = new CurrentState();

        if (!context.getTestMethod().isPresent()) {
            return;
        }

        SystemProperty[] systemProperties = context.getTestMethod().get().getAnnotationsByType(SystemProperty.class);
        if (systemProperties != null) {
            System.setProperties(copyOf(currentState.getProperties()));
            Arrays.stream(systemProperties).forEach(prop -> System.setProperty(prop.key(), prop.value()));
        }

        final Map<String, String> copyOfEnvs = new HashMap<>(currentState.getEnvironmentVariables());
        EnvironmentVariable[] environmentVariables = context.getTestMethod().get().getAnnotationsByType(EnvironmentVariable.class);
        if (environmentVariables != null) {
            Arrays.stream(environmentVariables).forEach(env -> copyOfEnvs.put(env.key(), env.value()));
            updateEnvs(copyOfEnvs);
        }

    }

    @SuppressWarnings("unchecked")
    private void updateEnvs(Map<String, String> envs) {
        try {
            Class<?> cl = System.getenv().getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(System.getenv());
            writableEnv.clear();
            writableEnv.putAll(envs);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    private Properties copyOf(Properties originalProperties) {
        final Properties copyOfOriginalProperties = new Properties();
        if (originalProperties != null) {
            copyOfOriginalProperties.putAll(originalProperties);
        }
        return copyOfOriginalProperties;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.setProperties(currentState.getProperties());
        updateEnvs(currentState.getEnvironmentVariables());
    }

    class CurrentState {
        private final Properties properties;
        private final Map<String, String> environmentVariables;

        public CurrentState() {
            properties = System.getProperties();
            environmentVariables = System.getenv();
        }

        public Map<String, String> getEnvironmentVariables() {
            return environmentVariables;
        }

        public Properties getProperties() {
            return properties;
        }
    }
}
