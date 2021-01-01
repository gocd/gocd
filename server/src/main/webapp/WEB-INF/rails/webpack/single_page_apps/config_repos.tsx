/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {RoutedSinglePageApp} from "helpers/spa_base";
import {ConfigReposPage} from "views/pages/config_repos";

export class ConfigReposSPA extends RoutedSinglePageApp {
  constructor() {
    super({
      "/": ConfigReposPage,
      "/:id": ConfigReposPage
    });
  }
}

// tslint:disable-next-line
new ConfigReposSPA();
