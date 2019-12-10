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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Link} from "views/components/link";
import styles from "./edit_pipelines.scss";

interface Attrs {
  dataTestId?: string;
  configRepoId: string;
}

export class ConfigRepoLink extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <span data-test-id={vnode.attrs.dataTestId} className={styles.configRepoLink}>
                                      (Config Repository:
                                      <Link target="_blank" href={SparkRoutes.ConfigRepoViewPath(vnode.attrs.configRepoId)}>
                                        {vnode.attrs.configRepoId}
                                      </Link>
                                      )
                                    </span>;
  }
}
