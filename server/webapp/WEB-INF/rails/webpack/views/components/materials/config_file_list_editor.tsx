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
import {ConfigFileList} from "models/materials/config_file_list";

interface Attrs {
  configFileList: ConfigFileList;
}

export class ConfigFileListEditor extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    const list = vnode.attrs.configFileList;
    let filesList;
    if (list.hasErrors()) {
      filesList = (<li>{list.errors()}</li>);
    } else if (list.isEmpty()) {
      return;
    } else {
      filesList = _.map(list.files(), (file) => {
        return <li>
          {file}
          </li>;
      });
    }
    return <div>
      <span>{list.pluginId()}</span>
      <ul>
        {filesList}
      </ul>
    </div>;
  }
}
