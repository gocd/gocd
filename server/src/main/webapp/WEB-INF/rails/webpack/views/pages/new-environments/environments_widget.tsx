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
import {Anchor, ScrollManager} from "views/components/anchor/anchor";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Delete, IconGroup} from "views/components/icons";
import {Link} from "views/components/link";
import {EnvironmentBody} from "views/pages/new-environments/environment_body_widget";
import {EnvironmentHeader} from "views/pages/new-environments/environment_header_widget";
import {DeleteOperation} from "views/pages/page_operations";
import styles from "./index.scss";

interface Attrs extends DeleteOperation<EnvironmentWithOrigin> {
  environments: Stream<Environments>;
  onSuccessfulSave: (msg: m.Children) => void;
  agents: Stream<Agents>;
  sm: ScrollManager;
}

interface EnvAttrs extends Attrs {
  environment: EnvironmentWithOrigin;
}

export class EnvironmentWidget extends MithrilViewComponent<EnvAttrs> {
  expanded: Stream<boolean> = Stream();

  oninit(vnode: m.Vnode<EnvAttrs>) {
    const linked = vnode.attrs.sm.getTarget() === vnode.attrs.environment.name();

    // set the initial state of the collapsible panel; alternative to setting `expanded` attribute
    // and, perhaps, more obvious that this is only matters for first load
    this.expanded(linked);
  }

  view(vnode: m.Vnode<EnvAttrs>) {
    const environment = vnode.attrs.environment;
    return <Anchor id={environment.name()} sm={vnode.attrs.sm} onnavigate={() => this.expanded(true)}>
      <CollapsiblePanel header={<EnvironmentHeader environment={environment}/>}
                        warning={this.isEnvEmpty(environment)}
                        actions={this.getActionButtons(environment, vnode)}
                        dataTestId={`collapsible-panel-for-env-${environment.name()}`}
                        vm={this}>
        <EnvironmentBody environment={environment}
                         environments={vnode.attrs.environments}
                         agents={vnode.attrs.agents}
                         onSuccessfulSave={vnode.attrs.onSuccessfulSave}/>
      </CollapsiblePanel>
    </Anchor>;
  }

  getActionButtons(environment: EnvironmentWithOrigin, vnode: m.Vnode<EnvAttrs>) {
    let warningButton;
    if (this.isEnvEmpty(environment)) {
      warningButton = <div data-test-id="warning-tooltip-wrapper" className={styles.warningTooltipWrapper}>
        <i data-test-id={"warning-icon"} className={styles.warningIcon}/>
        <div data-test-id="warning-tooltip-content" className={styles.warningTooltipContent}>
          <p>Neither pipelines nor agents are associated with this environment.</p>
        </div>
      </div>;
    }
    let deleteTitle;
    if (!environment.canAdminister()) {
      deleteTitle = `You are not authorized to delete the '${environment.name()}' environment.`;
    } else if (!environment.isLocal()) {
      deleteTitle = `Cannot delete '${environment.name()}' environment as it is partially defined in config repository.`;
    }
    return [
      warningButton,
      <IconGroup>
        <Delete
          title={deleteTitle}
          disabled={!environment.canAdminister() || !environment.isLocal()}
          onclick={vnode.attrs.onDelete.bind(vnode.attrs, environment)}/>
      </IconGroup>
    ];
  }

  private isEnvEmpty(environment: EnvironmentWithOrigin) {
    return _.isEmpty(environment.pipelines()) && _.isEmpty(environment.agents());

  }
}

export class EnvironmentsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {

    if (_.isEmpty(vnode.attrs.environments())) {
      return this.noEnvironmentConfiguresMessage();
    }

    return <div>
      {vnode.attrs.environments().map((environment: any) => {
        return <EnvironmentWidget {...vnode.attrs} environment={environment}/>;
      })}
    </div>;

  }

  noEnvironmentConfiguresMessage() {
    const environmentUrl = "/configuration/managing_environments.html";
    const docLink        = <span data-test-id="doc-link">
       <Link href={docsUrl(environmentUrl)} target="_blank" externalLinkIcon={true}>
        Learn More
      </Link>
    </span>;

    const noEnvironmentPresentMsg = <span>
      Either no environments have been set up or you are not authorized to view the environments. {docLink}
    </span>;

    return <FlashMessage type={MessageType.info} message={noEnvironmentPresentMsg} dataTestId="no-environment-present-msg"/>;
  }
}
