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
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {MaterialModification} from "models/config_repos/types";
import {MaterialWithModification} from "models/materials/materials";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import styles from "./index.scss";
import {MaterialAttrs} from "./materials_widget";

export class MaterialHeaderWidget extends MithrilViewComponent<MaterialAttrs> {
  private static readonly MAX_COMMIT_MSG_LENGTH: number            = 84;
  private static readonly MAX_USERNAME_AND_REVISION_LENGTH: number = 40;

  view(vnode: m.Vnode<MaterialAttrs, this>): m.Children | void | null {
    const material = vnode.attrs.materialVM.material;
    return [
      MaterialHeaderWidget.getIcon(material),
      <div className={headerStyles.headerTitle}>
        <h4 data-test-id="material-type" className={headerStyles.headerTitleText}>{material.config.typeForDisplay()}</h4>
        <span data-test-id="material-display-name" className={headerStyles.headerTitleUrl}>{material.config.displayName()}</span>
      </div>,
      <div data-test-id="latest-mod-in-header">{this.showLatestModificationDetails(material.config.fingerprint(), material.modification)}</div>
    ];
  }

  private static getIcon(material: MaterialWithModification) {
    let style = styles.unknown;
    switch (material.config.type()) {
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

  private showLatestModificationDetails(fingerprint: string, modification: MaterialModification | null) {
    if (modification === null) {
      return "This material was never parsed";
    }

    const comment         = _.truncate(modification.comment, {length: MaterialHeaderWidget.MAX_COMMIT_MSG_LENGTH});
    const username        = _.truncate(modification.username, {length: MaterialHeaderWidget.MAX_USERNAME_AND_REVISION_LENGTH});
    const revision        = _.truncate(modification.revision, {length: MaterialHeaderWidget.MAX_USERNAME_AND_REVISION_LENGTH});
    const usernameElement = _.isEmpty(username)
      ? undefined
      : <span>
      <span className={headerStyles.committer}>{username}</span> | </span>;

    const revisionLink = <Link dataTestId={"vsm-link"} href={SparkRoutes.materialsVsmLink(fingerprint, modification.revision)}
                               title={"VSM"} onclick={e => e.stopPropagation()}>{revision}</Link>;
    return <div
      className={headerStyles.commitInfo}>
      <span className={headerStyles.comment}>
        {comment}
      </span>
      <div className={headerStyles.committerInfo}>
        {usernameElement}{revisionLink}
      </div>
    </div>;
  }
}
