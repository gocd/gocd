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
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialAPIs, MaterialWithFingerprints} from "models/materials/materials";
import {Link} from "views/components/link";
import * as styles from "views/pages/materials/index.scss";
import {MaterialsWidget} from "views/pages/materials/materials_widget";
import {Page, PageState} from "views/pages/page";

export interface MaterialsAttrs {
  materials: Stream<MaterialWithFingerprints>;
}

// tslint:disable-next-line:no-empty-interface
interface State extends MaterialsAttrs {
}

export class MaterialsPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.materials = Stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (vnode.state.materials().length === 0) {
      return <div class={styles.help}>
        {this.helpText()}
      </div>;
    }
    return <MaterialsWidget {...vnode.state}/>;
  }

  pageName(): string {
    return "Materials";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.resolve(MaterialAPIs.all()).then((result) => {
      result.do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.materials(successResponse.body);
      }, this.setErrorState);
    });
  }

  helpText(): m.Children {
    return <span>
      A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered".
      <Link href={docsUrl("introduction/concepts_in_go.html#materials")} target={"_blank"} externalLinkIcon={true}> Learn More</Link>
    </span>;
  }
}
