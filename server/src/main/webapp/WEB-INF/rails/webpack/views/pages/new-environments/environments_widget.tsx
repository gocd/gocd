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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Agents} from "models/agents/agents";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {EnvironmentBody} from "views/pages/new-environments/environment_body_widget";
import {EnvironmentHeader} from "views/pages/new-environments/environment_header_widget";
import {FlashMessage, MessageType} from "../../components/flash_message";
import {Delete, IconGroup} from "../../components/icons";
import {Link} from "../../components/link";
import {DeleteOperation} from "../page_operations";

interface Attrs extends DeleteOperation<EnvironmentWithOrigin> {
  environments: Stream<Environments>;
  onSuccessfulSave: (msg: m.Children) => void;
  agents: Stream<Agents>;
}

export class EnvironmentsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const environmentsHtml = vnode.attrs.environments().map((environment: EnvironmentWithOrigin) => {
      const isEnvEmpty = _.isEmpty(environment.pipelines()) && _.isEmpty(environment.agents());

      return <CollapsiblePanel header={<EnvironmentHeader environment={environment}/>}
                               warning={isEnvEmpty}
                               actions={[
                                 <IconGroup>
                                   <Delete
                                     title={environment.canAdminister() ? undefined : `You are not authorized to delete '${environment.name()}' environment.`}
                                     disabled={!environment.canAdminister()}
                                     onclick={vnode.attrs.onDelete.bind(vnode.attrs, environment)}/>
                                 </IconGroup>
                               ]}
                               dataTestId={`collapsible-panel-for-env-${environment.name()}`}>
        <EnvironmentBody environment={environment}
                         environments={vnode.attrs.environments()}
                         agents={vnode.attrs.agents}
                         onSuccessfulSave={vnode.attrs.onSuccessfulSave}/>
      </CollapsiblePanel>;
    });

    const environmentUrl = "/configuration/managing_environments.html";
    const docLink        = <span data-test-id="doc-link">&nbsp;<Link href={docsUrl(environmentUrl)}
                                                                     target="_blank"
                                                                     externalLinkIcon={true}>Learn More</Link></span>;

    const noEnvironmentPresentMsg = <span>No environments are displayed because either no environments have been set up or you are not authorized to view the pipelines within any of the environments.{docLink}</span>;

    const noEnvironmentPresentMsgHtml = <FlashMessage type={MessageType.info}
                                                      message={noEnvironmentPresentMsg}
                                                      dataTestId="no-environment-present-msg"/>;

    return _.isEmpty(vnode.attrs.environments()) ? noEnvironmentPresentMsgHtml : environmentsHtml;
  }
}
