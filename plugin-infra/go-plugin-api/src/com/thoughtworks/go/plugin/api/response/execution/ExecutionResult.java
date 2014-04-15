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

package com.thoughtworks.go.plugin.api.response.execution;

import com.thoughtworks.go.plugin.api.response.Result;

public class ExecutionResult extends Result {

    public static ExecutionResult failure(String message,Exception ex){
       return failure(message);
    }

    public static ExecutionResult failure(String message){
        ExecutionResult executionResult = new ExecutionResult();
        executionResult.withErrorMessages(message);
        return executionResult;
    }

    public static ExecutionResult success(String message){
        ExecutionResult executionResult = new ExecutionResult();
        executionResult.withSuccessMessages(message);
        return executionResult;
    }
}
