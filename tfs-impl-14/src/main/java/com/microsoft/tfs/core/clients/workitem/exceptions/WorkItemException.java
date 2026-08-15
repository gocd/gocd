/*
 * Copyright Thoughtworks, Inc.
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
package com.microsoft.tfs.core.clients.workitem.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * An empty stand-in for a TFS SDK class removed by trimTfsSdkJar (see build.gradle), mirroring the real
 * class's hierarchy. Only exists as the superclass of {@link UnableToSaveException}, which the bytecode
 * verifier must be able to load; the work item feature that throws it is trimmed away.
 */
public class WorkItemException extends TEClientException {
}
