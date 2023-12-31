/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BoundedOutputStreamConsumerTest {

    @Test
    public void shouldCropLongLines() {
        InMemoryStreamConsumer actualConsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        BoundedOutputStreamConsumer streamConsumer = new BoundedOutputStreamConsumer(actualConsumer, 30);

        streamConsumer.stdOutput("This is a fairly ridiculously long line.");

        assertThat(actualConsumer.getAllOutput(), is("This is ...[ cropped by GoCD ]\n"));
    }

    @Test
    public void shouldNotCropShortLines() {
        InMemoryStreamConsumer actualConsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        BoundedOutputStreamConsumer streamConsumer = new BoundedOutputStreamConsumer(actualConsumer, 30);

        streamConsumer.stdOutput("A short line");

        assertThat(actualConsumer.getAllOutput(), is("A short line\n"));
    }

    @Test
    public void shouldNotCropLongLinesIfUnbounded() {
        InMemoryStreamConsumer actualConsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        BoundedOutputStreamConsumer streamConsumer = new BoundedOutputStreamConsumer(actualConsumer, 0);

        streamConsumer.stdOutput("This is a fairly ridiculously long line.");

        assertThat(actualConsumer.getAllOutput(), is("This is a fairly ridiculously long line.\n"));
    }

    @Test
    public void shouldKeepNoticeForSmallMaxLength() {
        InMemoryStreamConsumer actualConsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        BoundedOutputStreamConsumer streamConsumer = new BoundedOutputStreamConsumer(actualConsumer, 10);

        streamConsumer.stdOutput("This is a fairly ridiculously long line.");

        assertThat(actualConsumer.getAllOutput(), is("...[ cropped by GoCD ]\n"));
    }
}
