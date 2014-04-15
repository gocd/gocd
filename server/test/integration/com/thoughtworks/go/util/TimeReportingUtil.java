/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

public class TimeReportingUtil {
    private Date begin;
    private final String key;

    public enum Key {
        CONFIG_READ_2000, CONFIG_WRITE_2000, NOT_APPLICABLE
    }

    private TimeReportingUtil(String key) {
        this.key = key;
    }

    public static void report(Key key, TestAction testAction) throws Exception {
        TimeReportingUtil t = new TimeReportingUtil(key.toString());
        t.begin();
        try {
            testAction.perform();
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                t.report();
            } catch (IOException e) {
                //IGNORE
            }
        }
    }

    public static void print(TestAction testAction) throws Exception {
        Date begin;
        Date end;
        begin = new Date();
        testAction.perform();
        end = new Date();
        System.out.println("Time Taken: " + (end.getTime() - begin.getTime()) / 1000.0 + "s");
    }

    public void begin() {
        begin = new Date();
    }

    public void report() throws IOException {
        long difference = new Date().getTime() - begin.getTime();
        HttpClient httpClient = new HttpClient();
        HttpService.HttpClientFactory factory = new HttpService.HttpClientFactory(httpClient);
        PostMethod post = factory.createPost("http://host:3000/properties");
        post.addParameter("property[key]", key);
        post.addParameter("property[value]", String.valueOf(difference));
        try {
            httpClient.executeMethod(post);
        } finally {
            begin = null;
        }
        if (shouldThrowUp()) {
            if (post.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException(String.format("[Error] Posting [Key: %s] [Value: %s] failed.with status code %s", key, difference, post.getStatusCode()));
            }
        }
    }

    private boolean shouldThrowUp() {
        String envVariableValue = System.getenv("TIME_REPORTING_FAIL_ON_ERROR");
        return envVariableValue != null && "Y".equals(envVariableValue);
    }

    public static interface TestAction {
        public void perform() throws Exception;
    }
}
