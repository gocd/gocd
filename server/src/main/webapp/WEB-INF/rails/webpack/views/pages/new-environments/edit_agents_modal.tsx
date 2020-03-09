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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Agent, AgentConfigState, Agents} from "models/agents/agents";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import s from "underscore.string";
import {Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {CheckboxField, SearchField} from "views/components/forms/input_fields";
import {Modal, ModalState, Size} from "views/components/modal";
import styles from "views/pages/new-environments/edit_pipelines.scss";
import {AgentsViewModel} from "views/pages/new-environments/models/agents_view_model";

interface AgentCheckboxListWidgetAttrs {
  readonly: boolean;
  title: string;
  agents: Agents;
  agentSelectedFn: (a: any) => (a: boolean | undefined) => boolean | undefined;
}

export class AgentCheckboxListWidget extends MithrilViewComponent<AgentCheckboxListWidgetAttrs> {
  view(vnode: m.Vnode<AgentCheckboxListWidgetAttrs>) {
    const agents = vnode.attrs.agents;

    if (agents.length === 0) {
      return;
    }

    return <div class={styles.pipelinesContainer} data-test-id={s.slugify(vnode.attrs.title)}>
      <div class={styles.header}>{vnode.attrs.title}</div>
      {
        agents
          .filter((agent: Agent) => agent.agentConfigState !== AgentConfigState.Pending)
          .map((agent: Agent) => {
            return <div class={styles.pipelineCheckbox} data-test-id={`agent-checkbox-for-${agent.uuid}`}>
              <CheckboxField label={agent.hostname}
                             dataTestId={`form-field-input-${agent.uuid}`}
                             readonly={vnode.attrs.readonly}
                             property={vnode.attrs.agentSelectedFn(agent)}/>
            </div>;
          })
      }
    </div>;
  }
}

interface UnavailableElasticAgentsWidgetAttrs {
  agents: Agents;
}

export class UnavailableElasticAgentsWidget extends MithrilViewComponent<UnavailableElasticAgentsWidgetAttrs> {
  view(vnode: m.Vnode<UnavailableElasticAgentsWidgetAttrs>) {
    const agents = vnode.attrs.agents;

    if (agents.length === 0) {
      return;
    }

    const title = "Unavailable Agents (Elastic Agents):";
    return <div class={styles.pipelinesContainer} data-test-id={s.slugify(title)}>
      <div class={styles.header}> {title} </div>
      <ul>
        {
          agents.map((agent) => {
            return <li data-test-id={`agent-list-item-for-${agent.uuid}`}>
              <span>{agent.hostname}</span>
            </li>;
          })
        }
      </ul>
    </div>;
  }
}

interface AgentFilterWidgetAttrs {
  agentsVM: AgentsViewModel;
}

export class AgentFilterWidget extends MithrilViewComponent<AgentFilterWidgetAttrs> {
  view(vnode: m.Vnode<AgentFilterWidgetAttrs>) {
    return <div class={styles.pipelineFilterWrapper}>
      <span>Agents</span>
      <div class={styles.searchFieldWrapper}>
        <SearchField label="agent-search" placeholder="agent hostname"
                     property={vnode.attrs.agentsVM.searchText}/>
      </div>
    </div>;
  }
}

export class EditAgentsModal extends Modal {
  readonly agentsVM: AgentsViewModel;
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  private errorMessage: Stream<string> = Stream();

  constructor(env: EnvironmentWithOrigin,
              environments: Environments,
              agents: Agents,
              onSuccessfulSave: (msg: m.Children) => void) {
    super(Size.medium);
    this.onSuccessfulSave = onSuccessfulSave;
    this.fixedHeight      = true;
    this.agentsVM         = new AgentsViewModel(env.clone(), agents);
  }

  title(): string {
    return "Edit Agents Association";
  }

  body(): m.Children {
    if (!this.agentsVM.agents()) {
      return;
    }

    let noAgentsMsg: m.Child;
    if (this.agentsVM.agents().length === 0) {
      noAgentsMsg = <FlashMessage type={MessageType.info}
                                  message={'There are no agents available!'}/>;
    } else if (this.agentsVM.filteredAgents().length === 0) {
      noAgentsMsg = <FlashMessage type={MessageType.info}
                                  message={`No agents matching search text '${this.agentsVM.searchText()}' found!`}/>;
    }

    return <div>
      <AgentFilterWidget agentsVM={this.agentsVM}/>
      <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>
      {noAgentsMsg ? noAgentsMsg : this.agentsHtml()}
    </div>;
  }

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="save-button" onclick={this.performSave.bind(this)}
               disabled={this.isLoading()}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)} disabled={this.isLoading()}>Cancel</Cancel>
    ];
  }

  performSave() {
    const environment = this.agentsVM.environment;
    if (environment.isValid()) {
      this.modalState = ModalState.LOADING;
      EnvironmentsAPIs.updateAgentAssociation(environment.name(), this.agentsVM.selectedAgentUuids, this.agentsVM.removedAgentUuids)
                      .then((result) => {
                        this.modalState = ModalState.OK;
                        result.do((successResponse) => {
                          this.onSuccessfulSave(<span>Environment <em>{environment.name()}</em> was updated successfully!</span>);
                          this.close();
                        }, (errorResponse) => {
                          this.errorMessage(JSON.parse(errorResponse.body!).message);
                        });
                      });
    } else {
      return;
    }
  }

  private agentsHtml() {
    return <div className={styles.allPipelinesWrapper}>
      <AgentCheckboxListWidget agents={this.agentsVM.availableAgents()}
                               title={"Available Agents:"}
                               readonly={false}
                               agentSelectedFn={this.agentsVM.agentSelectedFn.bind(this.agentsVM)}/>
      <AgentCheckboxListWidget agents={this.agentsVM.configRepoEnvironmentAgents()}
                               title={"Agents associated with this environment in configuration repository:"}
                               readonly={true}
                               agentSelectedFn={this.agentsVM.agentSelectedFn.bind(this.agentsVM)}/>
      <AgentCheckboxListWidget agents={this.agentsVM.environmentElasticAgents()}
                               title={"Elastic Agents associated with this environment:"}
                               readonly={true}
                               agentSelectedFn={this.agentsVM.agentSelectedFn.bind(this.agentsVM)}/>
      <UnavailableElasticAgentsWidget agents={this.agentsVM.elasticAgentsNotBelongingToCurrentEnv()}/>
    </div>;
  }
}
