/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;

public final class ListUtil {
    private ListUtil() {
    }

    public static String join(Collection c) {
        return join(c, ", ");
    }

    public static String join(Collection c, String join) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Object> iter = c.iterator(); iter.hasNext(); ) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(join);
            }
        }
        return sb.toString();
    }

    public static <T> T find(Collection<T> c, Condition condition) {
        for (T item : c) {
            if (condition.isMet(item)) {
                return item;
            }
        }
        return null;
    }

    public interface Condition {
        <T> boolean isMet(T item);
    }


    public static <T> boolean addAllTo(List<T> list, T... args) {
        return list.addAll(asList(args));
    }

    public static List<String> split(String list, String splitOn) {
        return Arrays.asList(list.split(splitOn));
    }

    public static List<List<String>> split(List<String> list, String splitOn) {
        ArrayList<List<String>> splittedStrings = new ArrayList<>();
        if (list.isEmpty()) {
            return splittedStrings;
        }
        splittedStrings.add(new ArrayList<String>());
        for (String line : list) {
            if (line.equals(splitOn)) {
                splittedStrings.add(new ArrayList<String>());
            } else {
                last(splittedStrings).add(line);
            }
        }
        return splittedStrings;

    }

    public static <T> T last(List<T> list) {
        ExceptionUtils.bombIf(list.isEmpty(), "Unable to get last of empty list");
        return list.get(list.size() - 1);
    }

    public static <T> T removeLast(List<T> list) {
        ExceptionUtils.bombIf(list.isEmpty(), "Unable to remove last of empty list");
        return list.remove(list.size() - 1);
    }

    public static <T, S extends Collection<T>> S filterInto(S results, Collection<T> source, Filter<T> filter) {
        for (T model : source) {
            if (filter.matches(model)) {
                results.add(model);
            }
        }
        return results;
    }

    public interface Transformer<T, V> {
        V transform(T obj);
    }

    public static <T, V> ArrayList<V> map(Collection<T> list, Transformer<T, V> transformer) {
        ArrayList<V> transformedList = new ArrayList<>();
        for (T obj : list) {
            transformedList.add(transformer.transform(obj));
        }
        return transformedList;
    }
}
