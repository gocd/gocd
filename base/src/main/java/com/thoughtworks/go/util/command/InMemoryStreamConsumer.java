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
package com.thoughtworks.go.util.command;

import java.util.List;

public class InMemoryStreamConsumer extends ProcessOutputStreamConsumer<InMemoryConsumer, InMemoryConsumer> {

    public InMemoryStreamConsumer() {
        super(new InMemoryConsumer(), new InMemoryConsumer());
    }

    public String getStdError() {
        return getErrorConsumer().toString();
    }

    public String getStdOut() {
        return getStdConsumer().toString();
    }

    public String getAllOutput() {
        return getStdOut() + "\n"
                + getStdError();

    }

    public List<String> getStdLines() {
        return getStdConsumer().asList();
    }
    public List<String> getErrLines() {
        return getErrorConsumer().asList();
    }
}
