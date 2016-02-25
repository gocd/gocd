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

/**
 * Used to specify the result of an execution of a part of a plugin.
 */
@Deprecated
//Will be moved to internal scope
public class ExecutionResult extends Result {

    /**
     * Mark the result as 'Failure'.
     *
     * @param message More details about the failure.
     * @param exception (currently not used)
     * @return A new ExecutionResult instance, which is marked as 'Failure'.
     */
    public static ExecutionResult failure(String message, Exception exception) {
        return failure(message);
    }

    /**
     * Mark the result as 'Failure'.
     *
     * @param message More details about the failure.
     * @return A new ExecutionResult instance, which is marked as 'Failure'.
     */
    public static ExecutionResult failure(String message) {
        ExecutionResult executionResult = new ExecutionResult();
        executionResult.withErrorMessages(message);
        return executionResult;
    }

    /**
     * Mark the result as 'Success'.
     *
     * @param message More details about the run.
     * @return A new ExecutionResult instance, which is marked as 'Success'.
     */
    public static ExecutionResult success(String message) {
        ExecutionResult executionResult = new ExecutionResult();
        executionResult.withSuccessMessages(message);
        return executionResult;
    }
}
