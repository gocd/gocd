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
package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import org.hamcrest.Matcher;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.hamcrest.Matchers.hasItem;

public class TestGoMessageListener implements GoMessageListener {
    private static final int TIMEOUT = 10000;

    public List<GoMessage> received = new ArrayList<>();

    @Override
    public void onMessage(GoMessage message) {
        received.add(message);
    }

    public GoMessage getMessage() {
        return received.get(0);
    }

    public void waitForMessage(Matcher matcher) {
        Matcher iterableMatcher = hasItem(matcher);
        long start = System.currentTimeMillis();
        while (!iterableMatcher.matches(received)) {
            if (System.currentTimeMillis() - start > TIMEOUT) {
                bomb("Timeout waiting for message : " + received);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

}
