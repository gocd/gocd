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

import org.apache.commons.lang.StringUtils;

public class ProcessFailedException extends RuntimeException {
    private final String sysout;
    private final String syserr;

    public String getSysout() {
        return sysout;
    }

    public String getSyserr() {
        return syserr;
    }

    public ProcessFailedException(String message, String sysout, String syserr, Throwable t) {
        super(StringUtils.isEmpty(syserr) ? message : String.format("%s\n Error: %s", message, syserr), t);
        this.sysout = sysout;
        this.syserr = syserr;
    }
}
