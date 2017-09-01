/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.logging;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;

// copy of class from log4j
class PropertyWatchdog extends StoppableFileWatchdogBecauseFileWatchDogIsUnStoppable {

    PropertyWatchdog(String filename) {
        super(filename);
        setName("WatchDog-" + new File(filename).getName());
    }

    public void doOnChange() {
        new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    }
}
