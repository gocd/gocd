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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import * as Icons from "views/components/icons/index";
import {EnvironmentBody} from "views/pages/new-environments/environment_body_widget";
import {EnvironmentHeader} from "views/pages/new-environments/environment_header_widget";

interface Attrs {
  environments: Stream<Environments>;
  deleteEnvironment: (env: EnvironmentWithOrigin) => Promise<any>;
  onSuccessfulSave: (msg: m.Children) => void;
}

export class EnvironmentsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return vnode.attrs.environments().map((environment: EnvironmentWithOrigin) => {
      const isEnvEmpty = _.isEmpty(environment.pipelines()) && _.isEmpty(environment.agents());
      return <CollapsiblePanel header={<EnvironmentHeader environment={environment}/>}
                               warning={isEnvEmpty}
                               actions={[
                                 <Icons.Delete iconOnly={true}
                                               onclick={(e: MouseEvent) => {
                                                 e.stopPropagation();
                                                 return vnode.attrs.deleteEnvironment(environment);
                                               }}/>
                               ]}
                               dataTestId={`collapsible-panel-for-env-${environment.name()}`}>
        <EnvironmentBody environment={environment} environments={vnode.attrs.environments()}
                         onSuccessfulSave={vnode.attrs.onSuccessfulSave}/>
      </CollapsiblePanel>;
    });
  }
}
