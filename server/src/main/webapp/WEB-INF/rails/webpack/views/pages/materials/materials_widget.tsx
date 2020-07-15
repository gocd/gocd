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

import classnames from "classnames";
import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaterialWithFingerprint} from "models/materials/materials";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {MaterialsAttrs} from "views/pages/materials";
import styles from "./index.scss";

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {

  static helpText() {
    return <span>
      A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered".
      <Link href={docsUrl("introduction/concepts_in_go.html#materials")} target={"_blank"} externalLinkIcon={true}> Learn More</Link>
    </span>;
  }

  view(vnode: m.Vnode<MaterialsAttrs>) {
    if (vnode.attrs.materials().length === 0) {
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
      {vnode.attrs.materials().map((material) => <MaterialWidget material={material}/>)}
    </div>;
  }
}

interface MaterialAttrs {
  material: MaterialWithFingerprint;
}

export class MaterialWidget extends MithrilViewComponent<MaterialAttrs> {
  view(vnode: m.Vnode<MaterialAttrs, this>): m.Children | void | null {
    const test = vnode.attrs.material.attributesAsMap();
    return <CollapsiblePanel header={<MaterialHeader {...vnode.attrs} />}>
      <KeyValuePair data={test}/>
    </CollapsiblePanel>;
  }

}

class MaterialHeader extends MithrilViewComponent<MaterialAttrs> {
  view(vnode: m.Vnode<MaterialAttrs, this>): m.Children | void | null {
    return [
      this.getIcon(vnode),
      <div className={headerStyles.headerTitle}>
        <h4 data-test-id="material-type" className={headerStyles.headerTitleText}>{vnode.attrs.material.typeForDisplay()}</h4>
        <span data-test-id="material-display-name" className={headerStyles.headerTitleUrl}>{vnode.attrs.material.displayName()}</span>
      </div>
    ];
  }

  private getIcon(vnode: m.Vnode<MaterialAttrs, this>) {
    let style      = styles.unknown;
    const material = vnode.attrs.material;
    switch (material.type()) {
      case "git":
        style = styles.git;
        break;
      case "hg":
        style = styles.mercurial;
        break;
      case "svn":
        style = styles.subversion;
        break;
      case "p4":
        style = styles.perforce;
        break;
      case "tfs":
        style = styles.tfs;
        break;
      case "package":
        style = styles.package;
        break;
    }
    return <div data-test-id="material-icon" className={classnames(styles.material, style)}/>;
  }
}
