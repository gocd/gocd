/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {Origin} from "models/shared/origin";
export namespace PipelineStructureJSON {
  export interface Nameable {
    name: string;
  }

  export interface PipelineStructure {
    groups: PipelineGroup[];
    templates: Template[];
  }

  export interface Template extends Nameable {
    stages: Stage[];
  }

  export interface PipelineGroup extends Nameable {
    pipelines: Pipeline[];
  }

  export interface Pipeline extends Nameable {
    template_name?: string;
    origin: Origin;
    stages: Stage[];
  }

  export interface Stage extends Nameable {
    jobs: Job[];
  }

  export interface Job extends Nameable {
    is_elastic?: boolean;
  }
}
