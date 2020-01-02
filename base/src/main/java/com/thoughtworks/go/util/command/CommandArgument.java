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
package com.thoughtworks.go.util.command;

import java.io.Serializable;

public abstract class CommandArgument implements Serializable {
    public abstract String originalArgument();

    public abstract String forDisplay();

    public abstract String forCommandLine();

    @Override
    public final String toString() {
        return forDisplay();
    }

    @Override
    public int hashCode() {
        String originalArgument = this.originalArgument();
        return originalArgument == null ? 0 : originalArgument.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (that.getClass() != this.getClass()) return false;
        return equal((CommandArgument) that);
    }

    protected boolean equal(CommandArgument that) {
        String originalArgument = this.originalArgument();
        String othersOriginalArgument = that.originalArgument();
        return originalArgument != null ? originalArgument.equals(othersOriginalArgument) : othersOriginalArgument == null;
    }

    public String replaceSecretInfo(String line) {
        if (originalArgument().length() > 0) {
            line = line.replace(originalArgument(), forDisplay());
        }
        return line;
    }
}
