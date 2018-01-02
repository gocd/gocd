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

public class Pair<A,B> {

    private final A a;
    private final B b;

    public static <U, V> Pair<U, V> pair(U a, V b) {
        return new Pair<>(a, b);
    }

    public Pair(A a, B b){
        this.a = a;
        this.b = b;
    }

    public static<T> Pair<T,T> twins(T value){
       return new Pair<>(value, value);
    }

    public A first(){
        return a;
    }

    public B last(){
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair pair = (Pair) o;

        if (a != null ? !a.equals(pair.a) : pair.a != null) {
            return false;
        }
        if (b != null ? !b.equals(pair.b) : pair.b != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        return result;
    }
}
