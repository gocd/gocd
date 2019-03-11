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

package com.thoughtworks.go.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class SecretParams extends ArrayList<SecretParam> implements Serializable {
    private static final Pattern pattern = Pattern.compile("(?:#\\{SECRET\\[(.*?)\\]\\[(.*?)\\]})+");

    public SecretParams() {
    }

    public SecretParams(SecretParam... secretParams) {
        if (secretParams.length > 0) {
            this.addAll(asList(secretParams));
        }
    }

    public SecretParams(int size) {
        super(size);
    }

    public List<String> keys() {
        return this.stream().map(SecretParam::getKey).collect(toList());
    }

    public Map<String, SecretParams> groupBySecretConfigId() {
        return this.stream().collect(groupingBy(SecretParam::getSecretConfigId, toSecretParams()));
    }

    public SecretParams merge(SecretParams secretParams) {
        this.addAll(secretParams);
        return this;
    }

    public static Collector<SecretParam, SecretParams, SecretParams> toSecretParams() {
        return Collector.of(SecretParams::new, SecretParams::add, SecretParams::merge);
    }

    public static SecretParams parse(String stringToParse) {
        final SecretParams secretParams = new SecretParams();

        if (isBlank(stringToParse)) {
            return secretParams;
        }

        final Matcher matcher = pattern.matcher(stringToParse);
        while (matcher.find()) {
            secretParams.add(new SecretParam(matcher.group(1), matcher.group(2)));
        }

        return secretParams;
    }

    public static SecretParams union(SecretParams list1, SecretParams list2) {
        final SecretParams newMergedList = new SecretParams(list1.size() + list2.size());
        newMergedList.addAll(list1);
        newMergedList.addAll(list2);
        return newMergedList;
    }
}