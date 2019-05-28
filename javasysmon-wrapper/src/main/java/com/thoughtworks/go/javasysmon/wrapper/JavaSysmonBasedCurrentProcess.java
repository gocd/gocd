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

import com.jezhumble.javasysmon.JavaSysMon;

import java.util.ArrayList;
import java.util.List;

class JavaSysmonBasedCurrentProcess implements CurrentProcess {

    @Override
    public void infanticide() {
        new JavaSysMon().infanticide();
    }

    @Override
    public long currentPid() {
        return new JavaSysMon().currentPid();
    }

    @Override
    public List<Object> immediateChildren() {
        ArrayList<Object> result = new ArrayList<>();
        new JavaSysMon().visitProcessTree((int) currentPid(), (osProcess, level) -> {
            if (level == 1) {
                result.add(osProcess.processInfo());
            }

            // false to prevent the process from being killed!
            return false;
        });

        return result;
    }
}
