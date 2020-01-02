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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * CompositeConsumer multicasts strings to any set of {@link StreamConsumer} or {@link TaggedStreamConsumer} instances
 */
public class CompositeConsumer implements TaggedStreamConsumer {
    private List<StreamConsumer> consumers = new LinkedList<>();
    private String defaultTag;

    public CompositeConsumer(String defaultTag, StreamConsumer... consumers) {
        this.defaultTag = defaultTag;
        this.consumers.addAll(Arrays.asList(consumers));
    }

    public CompositeConsumer(StreamConsumer... consumers) {
        this(null, consumers);
    }

    @Override
    public void taggedConsumeLine(String tag, String line) {
        Iterator i = consumers.iterator();
        while (i.hasNext()) {
            StreamConsumer consumer = (StreamConsumer) i.next();
            if (null != tag && consumer instanceof TaggedStreamConsumer) {
                ((TaggedStreamConsumer) consumer).taggedConsumeLine(tag, line);
            } else {
                consumer.consumeLine(line);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consumeLine(String line) {
        taggedConsumeLine(defaultTag, line);
    }
}
