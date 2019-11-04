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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentVariableWithOrigin} from "models/new-environments/environment_environment_variables";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {Agents} from "models/new_agent/agents";
import s from "underscore.string";
import {HelpText} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import {EditAgentsModal} from "views/pages/new-environments/edit_agents_modal";
import {EditEnvironmentVariablesModal} from "views/pages/new-environments/edit_environment_variables_modal";
import {EditPipelinesModal} from "views/pages/new-environments/edit_pipelines_modal";
import styles from "./index.scss";

export interface ElementListWidgetAttrs {
  modalToRender: (env: EnvironmentWithOrigin) => any;
  environment: EnvironmentWithOrigin;
  name: string;
}

export class ElementListWidget extends MithrilViewComponent<ElementListWidgetAttrs> {
  view(vnode: m.Vnode<ElementListWidgetAttrs>) {
    return <div class={styles.envBodyElement}
                data-test-id={`${s.slugify(vnode.attrs.name)}-for-${vnode.attrs.environment.name()}`}>
      <div class={styles.envBodyElementHeader} data-test-id={`${s.slugify(vnode.attrs.name)}-header`}>
        <span>{vnode.attrs.name}</span>
        <Icons.Edit iconOnly={true}
                    onclick={vnode.attrs.modalToRender.bind(vnode.attrs.modalToRender, vnode.attrs.environment)}/>
      </div>
      {vnode.children}
    </div>;
  }
}

interface EnvironmentBodyAttrs {
  environment: EnvironmentWithOrigin;
  environments: Environments;
  agents: Stream<Agents>;
  onSuccessfulSave: (msg: m.Children) => void;
}

interface EnvironmentBodyState {
  renderPipelinesModal: (env: EnvironmentWithOrigin) => any;
  renderAgentsModal: (env: EnvironmentWithOrigin) => any;
  renderEnvironmentsVariablesModal: (env: EnvironmentWithOrigin) => any;
}

export class EnvironmentBody extends MithrilComponent<EnvironmentBodyAttrs, EnvironmentBodyState> {
  oninit(vnode: m.Vnode<EnvironmentBodyAttrs, EnvironmentBodyState>) {
    vnode.state.renderPipelinesModal = (env: EnvironmentWithOrigin) => {
      new EditPipelinesModal(env, vnode.attrs.environments, vnode.attrs.onSuccessfulSave).render();
    };

    vnode.state.renderAgentsModal = (env: EnvironmentWithOrigin) => {
      new EditAgentsModal(env, vnode.attrs.environments, vnode.attrs.onSuccessfulSave).render();
    };

    vnode.state.renderEnvironmentsVariablesModal = (env: EnvironmentWithOrigin) => {
      new EditEnvironmentVariablesModal(env, vnode.attrs.onSuccessfulSave).render();
    };
  }

  view(vnode: m.Vnode<EnvironmentBodyAttrs, EnvironmentBodyState>) {
    const environment = vnode.attrs.environment;

    const plainTextVariables: m.Child = environment.environmentVariables().plainTextVariables().length === 0
      ? <HelpText helpText="No Plain Text Environment Variables are defined." helpTextId="no-plain-text-env-var"/>
      : <ul>{environment.environmentVariables().plainTextVariables().map(this.representPlainEnvVar)}</ul>;

    const secureVariables: m.Child = environment.environmentVariables().secureVariables().length === 0
      ? <HelpText helpText="No Secure Environment Variables are defined." helpTextId="no-secure-env-var"/>
      : <ul>{environment.environmentVariables().secureVariables().map(this.representSecureEnvVar)}</ul>;

    return <div class={styles.envBody} data-test-id={`environment-body-for-${environment.name()}`}>
      <ElementListWidget name={"Pipelines"}
                         modalToRender={vnode.state.renderPipelinesModal}
                         environment={environment}>
        <ul data-test-id={`pipelines-content`}>
          {environment.pipelines().map((pipeline) => <li>{pipeline.name()}</li>)}
        </ul>
      </ElementListWidget>
      <ElementListWidget name={"Agents"}
                         modalToRender={vnode.state.renderAgentsModal}
                         environment={environment}>
        <ul data-test-id={`agents-content`}>
          {environment.agents().map((envAgent) => {
            const agent = vnode.attrs.agents ? vnode.attrs.agents().find((agent) => agent.uuid === envAgent.uuid()) : null;
            return <li>{agent == null ? '' : agent.hostname}</li>;
          })}
        </ul>
      </ElementListWidget>
      <ElementListWidget name={"Environment Variables"}
                         modalToRender={vnode.state.renderEnvironmentsVariablesModal}
                         environment={environment}>
        <div data-test-id={`environment-variables-content`}>
          <div className={styles.envVarHeading}> Plain Text Environment Variables:</div>
          {plainTextVariables}
          <div className={styles.envVarHeading}> Secure Environment Variables:</div>
          {secureVariables}
        </div>
      </ElementListWidget>
    </div>;
  }

  representPlainEnvVar(envVar: EnvironmentVariableWithOrigin): m.Child {
    return <li>{envVar.name()} = {envVar.value()}</li>;
  }

  representSecureEnvVar(envVar: EnvironmentVariableWithOrigin): m.Child {
    return <li>{envVar.name()} = ******</li>;
  }
}
