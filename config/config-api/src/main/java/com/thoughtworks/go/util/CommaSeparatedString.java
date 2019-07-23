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

package com.thoughtworks.go.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

public class CommaSeparatedString {
    public static String append(String origCommaSeparatedStr, List<String> entriesToAdd){
        if(isEmpty(entriesToAdd)){
            return origCommaSeparatedStr;
        }

        LinkedHashSet<String> distinctEntrySet = createDistinctEntrySetWithExistingAndNewEntriesToAdd(origCommaSeparatedStr, entriesToAdd);

        List<String> entryList = distinctEntrySet.stream()
                                                 .filter(StringUtils::isNotBlank)
                                                 .collect(Collectors.toList());
        if (!entryList.isEmpty()) {
            return convertListToCommaSeparatedStr(entryList);
        }

        return null;
    }

    public static String remove(String commaSeparatedStr, List<String> entriesToRemove){
        if(isEmpty(entriesToRemove)){
            return commaSeparatedStr;
        }

        if (isNotBlank(commaSeparatedStr)) {
            List<String> entryListAfterRemoval = Arrays.stream(convertCommaSeparatedStrToArray(commaSeparatedStr))
                                                       .filter(entry -> !entriesToRemove.contains(entry))
                                                       .collect(Collectors.toList());
            if (!entryListAfterRemoval.isEmpty()) {
                return convertListToCommaSeparatedStr(entryListAfterRemoval);
            }

            return null;
        }

        return commaSeparatedStr;
    }

    private static LinkedHashSet<String> createDistinctEntrySetWithExistingAndNewEntriesToAdd(String commaSeparatedStr, List<String> entriesToAdd) {
        LinkedHashSet<String> uniqEntrySet = new LinkedHashSet<>();

        if (isNotBlank(commaSeparatedStr)) {
            uniqEntrySet.addAll(convertCommaSeparatedStrToList(commaSeparatedStr));
        }

        uniqEntrySet.addAll(entriesToAdd);
        return uniqEntrySet;
    }

    private static String convertListToCommaSeparatedStr(List<String> list){
        return String.join(",", list);
    }

    private static List<String> convertCommaSeparatedStrToList(String commaSeparatedStr){
        return Arrays.asList(convertCommaSeparatedStrToArray(commaSeparatedStr));
    }

    private static String[] convertCommaSeparatedStrToArray(String commaSeparatedStr){
        return commaSeparatedStr.split(",");
    }
}
