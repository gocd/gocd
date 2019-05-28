/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.javasysmon.wrapper;

import com.thoughtworks.go.utils.JavaVersion;

import java.util.List;

public class DefaultCurrentProcess implements CurrentProcess {

    private final CurrentProcess delegate;

    public DefaultCurrentProcess() {
        this.delegate = JavaVersion.current().isJava9Compatible() ? new Java9CompatibleCurrentProcess() : new JavaSysmonBasedCurrentProcess();
    }

    @Override
    public void infanticide() {
        delegate.infanticide();
    }

    @Override
    public long currentPid() {
        return delegate.currentPid();
    }

    @Override
    public List<Object> immediateChildren() {
        return delegate.immediateChildren();
    }
}
