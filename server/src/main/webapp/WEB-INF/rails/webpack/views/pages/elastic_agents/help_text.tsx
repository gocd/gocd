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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import styles from "views/pages/elastic_agents/help_text.scss";
import {ConceptDiagram} from "views/pages/pipelines/concept_diagram";

const clusterProfileImg      = require("../../../../app/assets/images/elastic_agents/cluster_profile.svg");
const elasticAgentProfileImg = require("../../../../app/assets/images/elastic_agents/elastic_agent_profile.svg");
const finishImg              = require("../../../../app/assets/images/elastic_agents/finish.svg");

export class HelpText extends MithrilViewComponent {
  view(vnode: m.Vnode) {
    return (
      <div>
        <div class={styles.panelHeader}>
          <h3 class={styles.panelTitle}>Configure Elastic Agents</h3>
        </div>
        <div class={styles.concepts}>
          <ConceptDiagram image={clusterProfileImg}>
            <h3>Step 1: Create a Cluster Profile</h3>
            <div>A cluster profile is the connection configuration of the environment where elastic agents run.</div>
          </ConceptDiagram>

          <ConceptDiagram image={elasticAgentProfileImg}>
            <h3>Step 2: Create an Elastic Agent Profile</h3>
            <div>An elastic profile usually contains the configuration for your elastic agent</div>
          </ConceptDiagram>

          <ConceptDiagram image={finishImg}>
            <h3>Step 3: Finish!</h3>
          </ConceptDiagram>
        </div>
      </div>
    );
  }
}
