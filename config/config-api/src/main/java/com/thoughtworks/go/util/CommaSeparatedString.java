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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

public class CommaSeparatedString {
    public static String append(String origCommaSeparatedStr, List<String> entriesToAdd){
        if(isEmpty(entriesToAdd)){
            return origCommaSeparatedStr;
        }

        LinkedHashSet<String> uniqEntries = new LinkedHashSet<>();
        if (isNotBlank(origCommaSeparatedStr)) {
            uniqEntries.addAll(asList(origCommaSeparatedStr.split(",")));
        }
        uniqEntries.addAll(entriesToAdd);

        List<String> entryList = uniqEntries.stream().filter(entry -> !entry.isEmpty()).collect(Collectors.toList());

        if(entryList.isEmpty()){
            return null;
        }
        return String.join(",", entryList);
    }

    public static String remove(String origCommaSeparatedStr, List<String> entriesToRemove){
        if(isEmpty(entriesToRemove)){
            return origCommaSeparatedStr;
        }

        if (isNotBlank(origCommaSeparatedStr)) {
            List<String> finalEntryList = Arrays.stream(origCommaSeparatedStr.split(","))
                                                .filter(entry -> !entriesToRemove.contains(entry))
                                                .collect(Collectors.toList());
            if (finalEntryList.isEmpty()) {
                return null;
            } else {
                return String.join(",", finalEntryList);
            }
        }

        return origCommaSeparatedStr;
    }
}
