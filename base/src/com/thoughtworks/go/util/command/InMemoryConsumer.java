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
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.util.ListUtil.join;

public class InMemoryConsumer implements StreamConsumer {
    private Queue<String> lines = new ConcurrentLinkedQueue<String>();
    private static final Logger LOG = Logger.getLogger(InMemoryConsumer.class);

    public void consumeLine(String line) {
        try {
            lines.add(line);
        } catch (RuntimeException e) {
            LOG.error("Problem consuming line [" + line + "]", e);
        }
    }

    public List<String> asList() {
        return new ArrayList<String>(lines);
    }

    public boolean contains(String message) {
        return toString().contains(message);
    }

    public String toString() {
        return output();
    }

    public String output() {
        return join(asList(), "\n");
    }

    public String lastLine() {
        List<String> lines = asList();
        return lines.get(lines.size() - 1);
    }

    public String firstLine() {
        return asList().get(0);
    }

    public int lineCount() {
        return lines.size();
    }

    public void waitForContain(String content, int timeoutInSeconds) throws InterruptedException {
        long start = System.nanoTime();
        while (true) {
            if (contains(content)) {
                break;
            }
            if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(timeoutInSeconds)) {
                throw new RuntimeException("waiting timeout!");
            }
            Thread.sleep(10);
        }
    }

    public void clear() {
        lines.clear();
    }
}
