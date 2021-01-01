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
package com.thoughtworks.go.util;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

public class CommaSeparatedString {
    public static String append(String origCommaSeparatedStr, List<String> entriesToAdd) {
        if (isEmpty(entriesToAdd)) {
            return origCommaSeparatedStr;
        }

        LinkedHashSet<String> distinctEntrySet = createDistinctEntrySetWithExistingAndNewEntriesToAdd(origCommaSeparatedStr, entriesToAdd);

        List<String> entryList = distinctEntrySet.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(toList());

        if (!entryList.isEmpty()) {
            return listToCommaSeparatedStr(entryList);
        }

        return null;
    }

    public static String remove(String commaSeparatedStr, List<String> entriesToRemove) {
        if (isEmpty(entriesToRemove)) {
            return commaSeparatedStr;
        }

        List<String> finalEntriesToRemove = filterEmptyEntriesAndTrimTheirValues(entriesToRemove);

        if (isNotBlank(commaSeparatedStr)) {
            List<String> entryListAfterRemoval = Arrays.stream(commaSeparatedStrToArr(commaSeparatedStr))
                    .map(String::trim)
                    .filter(entry -> !finalEntriesToRemove.contains(entry))
                    .collect(toList());

            if (!entryListAfterRemoval.isEmpty()) {
                return listToCommaSeparatedStr(entryListAfterRemoval);
            }

            return null;
        }

        return commaSeparatedStr;
    }

    private static List<String> filterEmptyEntriesAndTrimTheirValues(List<String> entriesToRemove) {
        return entriesToRemove.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(toList());
    }

    private static LinkedHashSet<String> createDistinctEntrySetWithExistingAndNewEntriesToAdd(String commaSeparatedStr, List<String> entriesToAdd) {
        LinkedHashSet<String> uniqEntrySet = new LinkedHashSet<>();

        if (isNotBlank(commaSeparatedStr)) {
            uniqEntrySet.addAll(commaSeparatedStrToList(commaSeparatedStr));
        }

        uniqEntrySet.addAll(entriesToAdd);
        return uniqEntrySet;
    }

    private static String listToCommaSeparatedStr(List<String> list) {
        return String.join(",", list);
    }

    public static List<String> commaSeparatedStrToList(String commaSeparatedStr){
        return isBlank(commaSeparatedStr) ? emptyList() : asList(commaSeparatedStrToArr(commaSeparatedStr));
    }

    private static String[] commaSeparatedStrToArr(String commaSeparatedStr) {
        return commaSeparatedStr.trim().split("\\s*,[,\\s]*");
    }
}
