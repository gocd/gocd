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
import {MaterialConfigFiles} from "models/materials/material_config_files";
import {ConfigFileListEditor} from "./config_file_list_editor";
import styles from "./material_check.scss";

interface Attrs {
  materialConfigFiles: MaterialConfigFiles;
}

export class MaterialConfigFilesEditor extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return (
      <div class={styles.materialConfigFiles}>
        <span class={styles.resultsTitle}>
          We found the following Configuration files.
        </span>
        <div class={styles.pluginConfigFiles}>
          {_.map(vnode.attrs.materialConfigFiles.pluginConfigFiles(), (cfgList) => {
            return (<ConfigFileListEditor configFileList={cfgList}/>);
          })}
        </div>
      </div>
    );
  }
}
