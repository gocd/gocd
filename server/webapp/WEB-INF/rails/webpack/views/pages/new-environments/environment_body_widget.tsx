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
import {EnvironmentVariableWithOrigin} from "models/new-environments/environment_environment_variables";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import s from "underscore.string";
import {HelpText} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import styles from "./index.scss";

export interface ElementListWidgetAttrs {
  environmentName: string;
  name: string;
}

export class ElementListWidget extends MithrilViewComponent<ElementListWidgetAttrs> {
  view(vnode: m.Vnode<ElementListWidgetAttrs>) {
    return <div class={styles.envBodyElement}
                data-test-id={`${s.slugify(vnode.attrs.name)}-for-${vnode.attrs.environmentName}`}>
      <div class={styles.envBodyElementHeader} data-test-id={`${s.slugify(vnode.attrs.name)}-header`}>
        <span>{vnode.attrs.name}</span>
        <Icons.Edit iconOnly={true} onclick={() => alert("You pressed edit button!")}/>
      </div>
      {vnode.children}
    </div>;
  }
}

interface EnvironmentBodyAttrs {
  environment: EnvironmentWithOrigin;
}

export class EnvironmentBody extends MithrilViewComponent<EnvironmentBodyAttrs> {
  view(vnode: m.Vnode<EnvironmentBodyAttrs>) {
    const environment = vnode.attrs.environment;

    const plainTextVariables: m.Child = environment.environmentVariables().plainTextVariables().length === 0
      ? <HelpText helpText="No Plain Text Environment Variables are defined." helpTextId="no-plain-text-env-var"/>
      : <ul>{environment.environmentVariables().plainTextVariables().map(this.representPlainEnvVar)}</ul>;

    const secureVariables: m.Child = environment.environmentVariables().secureVariables().length === 0
      ? <HelpText helpText="No Secure Environment Variables are defined." helpTextId="no-secure-env-var"/>
      : <ul>{environment.environmentVariables().secureVariables().map(this.representSecureEnvVar)}</ul>;

    return <div class={styles.envBody} data-test-id={`environment-body-for-${environment.name()}`}>
      <ElementListWidget name={"Pipelines"} environmentName={environment.name()}>
        <ul data-test-id={`pipelines-content`}>
          {environment.pipelines().map((pipeline) => <li>{pipeline.name()}</li>)}
        </ul>
      </ElementListWidget>
      <ElementListWidget name={"Agents"} environmentName={environment.name()}>
        <ul data-test-id={`agents-content`}>
          {environment.agents().map((agent) => <li>{agent.uuid()}</li>)}
        </ul>
      </ElementListWidget>
      <ElementListWidget name={"Environment Variables"} environmentName={environment.name()}>
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
