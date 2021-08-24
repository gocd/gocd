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

package com.thoughtworks.go.agent.testhelper;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

public class FakeGoServerExtension implements BeforeEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FakeGoServerExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        context.getRequiredTestInstances().getAllInstances() //
                .forEach(instance -> injectInstanceFields(context, instance));
    }

    private void injectInstanceFields(ExtensionContext context, Object instance) {
        findAnnotatedFields(instance.getClass(), GoTestResource.class, ReflectionUtils::isNotStatic).forEach(field -> {
            assertSupportedType("field", FakeGoServer.class);
            try {
                makeAccessible(field).set(instance, newFakeGoServer(context));
            }
            catch (Throwable t) {
                ExceptionUtils.throwAsUncheckedException(t);
            }
        });
    }

    private FakeGoServer newFakeGoServer(ExtensionContext context) {
        return context.getStore(NAMESPACE).getOrComputeIfAbsent("server", key -> startFakeGoServer(), FakeGoServer.class);
    }

    private FakeGoServer startFakeGoServer() {
        FakeGoServer server = new FakeGoServer();
        try {
            server.start();
        } catch (Exception e) {
            throw new ExtensionConfigurationException("Unable to start FakeGoServer", e);
        }
        return server;
    }

    private void assertSupportedType(String target, Class<?> type) {
        if (type != FakeGoServer.class) {
            throw new ExtensionConfigurationException("Can only resolve @GoTestResource " + target + " of type "
                    + FakeGoServer.class.getName() + " but was: " + type.getName());
        }
    }
}
