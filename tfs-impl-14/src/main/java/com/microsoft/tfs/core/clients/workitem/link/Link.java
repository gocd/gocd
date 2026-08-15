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
package com.microsoft.tfs.core.clients.workitem.link;

/**
 * An empty stand-in for a TFS SDK interface removed by trimTfsSdkJar (see build.gradle). The bytecode
 * verifier loads the target type of an assignability check even when that target is an interface (the check
 * then passes trivially, but the type must load to prove it IS an interface — JVMS 4.10.1.2), and the
 * retained {@code Workspace} class has such a check in its (unused) check-in path.
 */
public interface Link {
}
