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

import {MaterialJSON} from "./material_json";

export interface ComparisonJSON {
  pipeline_name: string;
  from_counter: number;
  to_counter: number;
  is_bisect: boolean;
  changes: ChangeJSON[];
}

export interface ChangeJSON {
  material: MaterialJSON;
  revision: RevisionJSON[];
}

export interface MaterialRevisionJSON {
  revision_sha: string;
  modified_by: string;
  modified_at: string;
  commit_message: string;
}

export interface DependencyRevisionJSON {
  revision: string;
  pipeline_counter: string;
  completed_at: string;
}

export type RevisionJSON = MaterialRevisionJSON | DependencyRevisionJSON;
