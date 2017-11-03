///*
// * Copyright 2017 ThoughtWorks, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.thoughtworks.go.server.websocket.browser.subscription;
//
//import com.thoughtworks.go.domain.JobIdentifier;
//import org.junit.Test;
//
//import java.util.List;
//
//import static org.hamcrest.Matchers.equalTo;
//import static org.hamcrest.Matchers.instanceOf;
//import static org.hamcrest.Matchers.is;
//import static org.junit.Assert.*;
//
//public class SubscriptionMessageTest {
//    public static final String JSON = "[\n" +
//            "  {\n" +
//            "    \"type\": \"JobStatusChange\",\n" +
//            "    \"job_identifier\": {\n" +
//            "      \"pipeline_name\": \"foo\",\n" +
//            "      \"pipeline_label\": 42,\n" +
//            "      \"stage_name\": \"test\",\n" +
//            "      \"stage_counter\": 1,\n" +
//            "      \"build_name\": \"unit\"\n" +
//            "    }\n" +
//            "  },\n" +
//            "  {\n" +
//            "    \"type\": \"ServerHealthMessageCount\"\n" +
//            "  }\n" +
//            "]";
//
//    @Test
//    public void shouldDeserializeAListOfObjects() throws Exception {
//        List<SubscriptionMessage> messages = SubscriptionMessage.fromJSON(JSON);
//
//        assertThat(messages.size(), is(2));
//        assertThat(messages.get(0), is(instanceOf(JobStatusChange.class)));
//        assertThat(messages.get(1), is(instanceOf(ServerHealthMessageCount.class)));
//    }
//
//    @Test
//    public void shouldDeserializeJobStatusChange() throws Exception {
//        List<SubscriptionMessage> messages = SubscriptionMessage.fromJSON(JSON);
//        JobStatusChange subscriptionMessage = (JobStatusChange) messages.get(0);
//        JobIdentifier jobIdentifier = new JobIdentifier("foo", -1, "42", "test", "1", "unit");
//        jobIdentifier.setBuildId(null);
//        jobIdentifier.setPipelineCounter(null);
//        assertThat(subscriptionMessage, equalTo(new JobStatusChange(jobIdentifier)));
//    }
//
//    @Test
//    public void shouldDeserializeServerHealthMessageCount() throws Exception {
//        List<SubscriptionMessage> messages = SubscriptionMessage.fromJSON(JSON);
//        ServerHealthMessageCount subscriptionMessage = (ServerHealthMessageCount) messages.get(1);
//        assertThat(subscriptionMessage, is(new ServerHealthMessageCount()));
//    }
//}