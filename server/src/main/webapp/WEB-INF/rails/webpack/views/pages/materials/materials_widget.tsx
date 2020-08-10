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
import m from "mithril";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "views/components/link";
import {MaterialsAttrs} from "views/pages/materials";
import styles from "./index.scss";
import {MaterialWidget} from "./material_widget";

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {

  static helpText() {
    return <span>
      A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered".
      <Link href={docsUrl("introduction/concepts_in_go.html#materials")} target={"_blank"} externalLinkIcon={true}> Learn More</Link>
    </span>;
  }

  view(vnode: m.Vnode<MaterialsAttrs>) {
    if (vnode.attrs.materialVMs().length === 0) {
      return <div>
        <FlashMessage type={MessageType.info}>
          Either no pipelines have been set up or you are not authorized to view the same.&nbsp;
          <Link href={docsUrl("configuration/dev_authorization.html#specifying-permissions-for-pipeline-groups")} target="_blank"
                externalLinkIcon={true}>Learn More</Link>
        </FlashMessage>
        <div data-test-id="materials-help" class={styles.help}>
          {MaterialsWidget.helpText()}
        </div>
      </div>;
    }
    return <div data-test-id="materials-widget">
      {vnode.attrs.materialVMs().map((materialVM) => <MaterialWidget materialVM={materialVM}
                                                                     shouldShowPackageOrScmLink={vnode.attrs.shouldShowPackageOrScmLink}/>)}
    </div>;
  }
}
