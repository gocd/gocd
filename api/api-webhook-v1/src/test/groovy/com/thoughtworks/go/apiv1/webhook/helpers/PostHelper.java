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

package com.thoughtworks.go.apiv1.webhook.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.apiv1.webhook.controller.validation.GitHub;
import com.thoughtworks.go.apiv1.webhook.controller.validation.HostedBitbucket;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public interface PostHelper {
    String SECRET = "webhook-secret";
    Gson GSON = new Gson();

    Map<String, Object> payload();

    Map<String, Object> header(String event);

    Map<String, Object> header(String event, String auth);

    static Map<String, Object> load(String resource) {
        final String json;
        try {
            json = FileUtils.readFileToString(new File(PostHelper.class.getResource(resource).getFile()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return PostHelper.GSON.fromJson(json, new TypeToken<>() {
        }.getType());
    }

    interface Mixin {
        default GitHubPost withGitHub(String resource) {
            return withGitHub(load(resource));
        }

        default GitHubPost withGitHub(Map<String, Object> data) {
            return new GitHubPost(data);
        }

        default GitLabPost withGitLab(String resource) {
            return withGitLab(load(resource));
        }

        default GitLabPost withGitLab(Map<String, Object> data) {
            return new GitLabPost(data);
        }

        default BitbucketPost withBitbucket(String resource) {
            return withBitbucket(load(resource));
        }

        default BitbucketPost withBitbucket(Map<String, Object> data) {
            return new BitbucketPost(data);
        }

        default HostedBitbucketPost withHostedBitbucket(String resource) {
            return withHostedBitbucket(load(resource));
        }

        default HostedBitbucketPost withHostedBitbucket(Map<String, Object> data) {
            return new HostedBitbucketPost(data);
        }
    }

    class GitHubPost implements PostHelper {
        private final Map<String, Object> payload;

        GitHubPost(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public Map<String, Object> payload() {
            return payload;
        }

        @Override
        public Map<String, Object> header(String event) {
            return header(event, GitHub.calculateSignature(SECRET, GSON.toJson(payload)));
        }

        @Override
        public Map<String, Object> header(String event, String auth) {
            final Map<String, Object> map = new HashMap<>();
            map.put("X-Hub-Signature", auth);
            map.put("X-GitHub-Event", event);
            return map;
        }
    }

    class GitLabPost implements PostHelper {
        private final Map<String, Object> payload;

        GitLabPost(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public Map<String, Object> payload() {
            return payload;
        }

        @Override
        public Map<String, Object> header(String event) {
            return header(event, SECRET);
        }

        @Override
        public Map<String, Object> header(String event, String auth) {
            final Map<String, Object> map = new HashMap<>();
            map.put("X-Gitlab-Token", auth);
            map.put("X-Gitlab-Event", event);
            return map;
        }
    }

    class BitbucketPost implements PostHelper {
        private final Map<String, Object> payload;

        BitbucketPost(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public Map<String, Object> payload() {
            return payload;
        }

        @Override
        public Map<String, Object> header(String event) {
            return header(event, SECRET);
        }

        @Override
        public Map<String, Object> header(String event, String auth) {
            final Map<String, Object> map = new HashMap<>();
            map.put("Authorization", format("Basic %s", Base64.getEncoder().encodeToString(auth.getBytes())));
            map.put("X-Event-Key", event);
            return map;
        }
    }

    class HostedBitbucketPost implements PostHelper {
        private final Map<String, Object> payload;

        HostedBitbucketPost(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public Map<String, Object> payload() {
            return payload;
        }

        @Override
        public Map<String, Object> header(String event) {
            return header(event, HostedBitbucket.calculateSignature(SECRET, GSON.toJson(payload)));
        }

        @Override
        public Map<String, Object> header(String event, String auth) {
            final Map<String, Object> map = new HashMap<>();
            map.put("X-Hub-Signature", auth);
            map.put("X-Event-Key", event);
            return map;
        }
    }
}

