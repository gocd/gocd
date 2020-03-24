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

import {ErrorsJSON} from "models/mixins/errors";
import {PropertyJSON} from "models/shared/configuration";

export interface PackageRepositorySummaryJSON {
  id: string;
  name: string;
}

export interface PackageJSON {
  id: string;
  name: string;
  auto_update: boolean;
  configuration: PropertyJSON[];
  package_repo: PackageRepositorySummaryJSON;
  errors?: ErrorsJSON;
}

export interface PackagesJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  packages: PackageJSON[];
}
