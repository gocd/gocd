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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {EnvironmentVariable, EnvironmentVariables, EnvironmentVariableWithOrigin} from "models/environment_variables/types";
import {EnvironmentVariablesWidget, GroupedEnvironmentVariables, GroupedEnvironmentVariablesAttrs} from "views/components/environment_variables";
import {EnvironmentVariableWidget, EnvironmentVariableWidgetAttrs} from "views/components/environment_variables/environment_variable_widget";
import {Link} from "views/components/link";

class EnvironmentVariableWithOriginWidget extends EnvironmentVariableWidget {
  reasonForNonEditableEnvVar(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>) {
    const environmentVariable = vnode.attrs.environmentVariable as EnvironmentVariableWithOrigin;
    return <div>
      Cannot edit this environment variable as it is defined in config repository:&nbsp;
      <Link target="_blank" href={SparkRoutes.ConfigRepoViewPath(environmentVariable.origin().id())}>{environmentVariable.origin().id()}</Link>
    </div>;
  }
}

class GroupedEnvironmentVariablesWithOrigin extends GroupedEnvironmentVariables {
  renderEnvironmentVariableWidget(vnode: m.Vnode<GroupedEnvironmentVariablesAttrs>, environmentVariable: EnvironmentVariableWithOrigin) {
    return <EnvironmentVariableWithOriginWidget environmentVariable={environmentVariable} onRemove={vnode.attrs.onRemove}/>;
  }
}

interface EnvironmentVariablesWidgetAttrs {
  environmentVariables: EnvironmentVariables;
}

export class EnvironmentVariablesWithOriginWidget extends EnvironmentVariablesWidget {
  view(vnode: m.Vnode<EnvironmentVariablesWidgetAttrs, {}>): m.Children {
    return <div>
      <GroupedEnvironmentVariablesWithOrigin environmentVariables={vnode.attrs.environmentVariables.plainTextVariables()}
                                             title="Plain Text Variables"
                                             onAdd={EnvironmentVariablesWidget.onAdd.bind(this, false, vnode)}
                                             onRemove={(envVar: EnvironmentVariable) => EnvironmentVariablesWidget.onRemove(envVar, vnode)}/>
      <GroupedEnvironmentVariablesWithOrigin environmentVariables={vnode.attrs.environmentVariables.secureVariables()}
                                             title="Secure Variables"
                                             onAdd={EnvironmentVariablesWidget.onAdd.bind(this, true, vnode)}
                                             onRemove={(envVar: EnvironmentVariable) => EnvironmentVariablesWidget.onRemove(envVar, vnode)}/>
    </div>;
  }
}
