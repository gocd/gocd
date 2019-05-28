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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface CurrentProcess {

    Logger LOG = LoggerFactory.getLogger(CurrentProcess.class);

    void infanticide();

    long currentPid();

    // this is only used for logging via `toString` so we make it return an object
    List<Object> immediateChildren();
}
