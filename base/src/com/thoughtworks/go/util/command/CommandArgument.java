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

package com.thoughtworks.go.util.command;

import java.io.Serializable;

public abstract class CommandArgument implements Serializable {
    public abstract String forCommandline();
    public abstract String forDisplay();

    public final String toString() {
        return forDisplay();
    }

    public int hashCode() {
        String commandLine = this.forCommandline();
        return commandLine == null ? 0 : commandLine.hashCode();
    }

    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (that.getClass()!=this.getClass()) return false;
        return equal((CommandArgument)that);
    }

    protected boolean equal(CommandArgument that) {
        String myCommandLine = this.forCommandline();
        String othersCommandLine = that.forCommandline();
        return myCommandLine != null ? myCommandLine.equals(othersCommandLine) : othersCommandLine == null;
    }

    public String replaceSecretInfo(String line) {
        if(forCommandline().length() > 0){
            line = line.replace(forCommandline(), forDisplay());
        }
        return line;
    }
}
