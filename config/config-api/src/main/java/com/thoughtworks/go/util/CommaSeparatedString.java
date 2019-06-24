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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CommaSeparatedString {
    public static String append(String origCommaSeparatedStr, List<String> entriesToAdd){
        if(CollectionUtils.isEmpty(entriesToAdd)){
            return origCommaSeparatedStr;
        }

        LinkedHashSet<String> entrySet = new LinkedHashSet<>();

        if (!StringUtils.isBlank(origCommaSeparatedStr)) {
            entrySet.addAll(Arrays.asList(origCommaSeparatedStr.split(",")));
        }
        entrySet.addAll(entriesToAdd);

        List<String> entryList = entrySet.stream().filter(entry -> !entry.isEmpty()).collect(Collectors.toList());

        if(entryList.isEmpty()){
            return null;
        }
        return String.join(",", entryList);
    }

    public static String remove(String origCommaSeparatedStr, List<String> entriesToRemove){
        if (!StringUtils.isBlank(origCommaSeparatedStr)) {
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
