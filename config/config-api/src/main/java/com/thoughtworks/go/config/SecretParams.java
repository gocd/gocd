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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.replaceOnce;

public class SecretParams extends ArrayList<SecretParam> implements Serializable {
    private static final Pattern PATTERN = Pattern.compile("(?:\\{\\{SECRET:\\[(.*?)\\]\\[(.*?)\\]}})+");
    public static final String MASKED_VALUE = "******";

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

    public Map<String, SecretParams> groupBySecretConfigId() {
        return this.stream().collect(groupingBy(SecretParam::getSecretConfigId, toSecretParams()));
    }

    public boolean hasSecretParams() {
        return !this.isEmpty();
    }

    public SecretParams merge(SecretParams secretParams) {
        this.addAll(secretParams);
        return this;
    }

    public static Collector<SecretParam, SecretParams, SecretParams> toSecretParams() {
        return Collector.of(SecretParams::new, SecretParams::add, SecretParams::merge);
    }

    public static Collector<SecretParams, SecretParams, SecretParams> toFlatSecretParams() {
        return Collector.of(SecretParams::new, SecretParams::addAll, SecretParams::merge);
    }

    public static SecretParams parse(String stringToParse) {
        final SecretParams secretParams = new SecretParams();

        if (isBlank(stringToParse)) {
            return secretParams;
        }

        final Matcher matcher = PATTERN.matcher(stringToParse);
        while (matcher.find()) {
            secretParams.add(new SecretParam(matcher.group(1), matcher.group(2)));
        }

        return secretParams;
    }

    public static SecretParams union(SecretParams... params) {
        final SecretParams newMergedList = new SecretParams();
        for (SecretParams param : params) {
            if (!CollectionUtils.isEmpty(param)) {
                newMergedList.addAll(param);
            }
        }
        return newMergedList;
    }

    public String substitute(String originalValue) {
        return this.stream()
                .map(this::textReplaceFunction)
                .reduce(Function.identity(), Function::andThen)
                .apply(originalValue);
    }

    private Function<String, String> textReplaceFunction(SecretParam secretParam) {
        return text -> {
            if (secretParam.isUnresolved()) {
                throw new UnresolvedSecretParamException(secretParam.getKey());
            }

            return replaceOnce(text, format("{{SECRET:[%s][%s]}}", secretParam.getSecretConfigId(), secretParam.getKey()), secretParam.getValue());
        };
    }

    public Optional<SecretParam> findFirst(String key) {
        return this.stream()
                .filter(secretParam -> secretParam.getKey().equals(key))
                .findFirst();
    }

    public Optional<SecretParam> findFirstByConfigId(String configId) {
        return this.stream()
                .filter(secretParam -> secretParam.getSecretConfigId().equals(configId))
                .findFirst();
    }

    public String mask(String originalValue) {
        return this.stream()
                .map(this::maskFunction)
                .reduce(Function.identity(), Function::andThen)
                .apply(originalValue);
    }

    private Function<String, String> maskFunction(SecretParam secretParam) {
        return text -> replaceOnce(text, secretParam.asString(), MASKED_VALUE);
    }
}
