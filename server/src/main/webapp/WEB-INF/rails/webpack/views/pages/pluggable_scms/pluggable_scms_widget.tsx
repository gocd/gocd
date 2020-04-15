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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Scm, Scms} from "models/materials/pluggable_scm";
import {ScrollManager} from "views/components/anchor/anchor";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "views/components/link";
import styles from "views/pages/package_repositories/index.scss";
import {CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos} from "views/pages/page_operations";
import {PluggableScmWidget} from "./pluggable_scm_widget";

interface Attrs extends RequiresPluginInfos, EditOperation<Scm>, CloneOperation<Scm>, DeleteOperation<Scm> {
  scms: Stream<Scms>;
  showUsages: (scm: Scm, e: MouseEvent) => void;
  scrollOptions: PluggableSCMScrollOptions;
}

export interface PluggableSCMScrollOptions {
  sm: ScrollManager;
  shouldOpenEditView: boolean;
}

export class PluggableScmsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.scrollOptions.sm.hasTarget()) {
      const target           = vnode.attrs.scrollOptions.sm.getTarget();
      const hasAnchorElement = vnode.attrs.scms().some((temp) => temp.name() === target);
      if (!hasAnchorElement) {
        const msg = `'${target}' SCM has not been set up.`;
        return <FlashMessage dataTestId="anchor-scm-not-present" type={MessageType.alert} message={msg}/>;
      }
    }
    if (!vnode.attrs.scms || _.isEmpty(vnode.attrs.scms())) {
      return <div className={styles.tips} data-test-id="pluggable-scm-info">
        <ul>
          <li>Click on "Create Pluggable Scm" to add new SCM.</li>
          <li>An SCM can be set up and used as a material in the pipelines. You can read more
            from <Link target="_blank" href={docsUrl("extension_points/scm_extension.html")}>here</Link>.
          </li>
        </ul>
      </div>;
    }
    return <div data-test-id="scms-widget">
      {vnode.attrs.scms().map((scm) => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: scm.pluginMetadata().id()});
        return <PluggableScmWidget scm={scm}
                                   sm={vnode.attrs.scrollOptions}
                                   disableActions={pluginInfo === undefined}
                                   {...vnode.attrs}/>;
      })}
    </div>;
  }
}
