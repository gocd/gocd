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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayUtil {
    private ArrayUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] addToArray(T[] array, T nextOne) {
        return (T[]) ArrayUtils.add(array, nextOne);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] pushToArray(T firstOne, T[] array) {
        return (T[]) ArrayUtils.add(array, 0, firstOne);
    }

    public static <T> String join(T[] array) {
        return join(array, ",");
    }

    public static <T> String join(T[] array, String separator) {
        if(array == null) return null;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            T t = array[i];
            buffer.append(t.toString());
            if (i < array.length - 1) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }

    /*
     * Much like Arrays.asList, except the list is not fixed size.
     */
    public static <T> List<T> asList(T... a) {
        if (a == null) { return new ArrayList<>(); }

        ArrayList<T> list = new ArrayList<>(a.length);
        list.addAll(Arrays.asList(a));
        return list;
    }

    public static <T> Object[] capitalizeContents(T[] array) {
        ArrayList<String> list = new ArrayList<>();
        for (T t : array) {
            list.add(StringUtils.capitalize(t.toString()));
        }
        return list.toArray();
    }

    public static <T> boolean isEmpty(T[] array) {
        if (array == null || array.length == 0) {
            return true;
        }
        return false;
    }
}
