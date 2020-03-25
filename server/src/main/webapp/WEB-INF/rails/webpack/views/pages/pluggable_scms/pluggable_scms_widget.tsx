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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {RequiresPluginInfos} from "views/pages/page_operations";
import {PluggableScmWidget} from "./pluggable_scm_widget";

interface Attrs extends RequiresPluginInfos {
  scms: Stream<Scms>;
}

export class PluggableScmsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id="scms">
      {vnode.attrs.scms().map((scm) => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: scm.pluginMetadata().id()});
        return <PluggableScmWidget scm={scm} disableActions={pluginInfo === undefined}/>
      })}
    </div>;
  }
}
