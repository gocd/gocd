/*
* Copyright 2018 ThoughtWorks, Inc.
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

import Page from "helpers/spa_base";
import * as  m from "mithril";
import {ArtifactStoresPage} from "views/pages/artifact_stores";

const $                    = require("jquery");
const ArtifactStoresWidget = require("views/artifact_stores/artifact_stores_widget");

export class ArtifactStoresSPA extends Page {
  constructor() {
    super(ArtifactStoresPage);
  }
}

$(() => {
  const artifactStoresContainer = $("#artifact-stores");

  if (artifactStoresContainer.get().length === 0) {
    return new ArtifactStoresSPA();
  }

  m.mount(artifactStoresContainer.get(0), ArtifactStoresWidget);
});
