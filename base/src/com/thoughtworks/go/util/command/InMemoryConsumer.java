/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util.command;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.thoughtworks.go.util.ListUtil.join;

public class InMemoryConsumer implements StreamConsumer {
    private Queue<String> lines = new ConcurrentLinkedQueue<>();
    private static final Logger LOG = Logger.getLogger(InMemoryConsumer.class);

    public void consumeLine(String line) {
        try {
            lines.add(line);
        } catch (RuntimeException e) {
            LOG.error("Problem consuming line [" + line + "]", e);
        }
    }

    public List<String> asList() {
        return new ArrayList<>(lines);
    }

    public boolean contains(String message) {
        return toString().contains(message);
    }

    public String toString() {
        return join(asList(), "\n");
    }
}
