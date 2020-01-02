/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.FileSizeUtils;

/**
 * @understands storage measurement
 */
public class DiskSpace implements Comparable<DiskSpace> {
    private final Long space;

    static final String UNKNOWN_DISK_SPACE = "Unknown";

    public DiskSpace(long space) {
        this.space = space;
    }

    public static DiskSpace unknownDiskSpace() {
        return new NullDiskSpace();
    }

    @Override
    public int compareTo(DiskSpace that) {
        return this.space.compareTo(that.space);
    }

    @Override
    public String toString() {
        return FileSizeUtils.byteCountToDisplaySize(space);
    }

    public Long space(){
        return space;
    }

    public boolean isNullDiskspace() {
        return false;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (this.getClass() != that.getClass()) { return false; }

        return this.space.equals(((DiskSpace) that).space);
    }

    @Override
    public int hashCode() {
        return (int) (space ^ (space >>> 32));
    }

    private static class NullDiskSpace extends DiskSpace {

        private NullDiskSpace() {
            super(-99999);
        }

        @Override
        public String toString() {
            return UNKNOWN_DISK_SPACE;
        }

        @Override
        public boolean isNullDiskspace() {
            return true;
        }
    }
}
