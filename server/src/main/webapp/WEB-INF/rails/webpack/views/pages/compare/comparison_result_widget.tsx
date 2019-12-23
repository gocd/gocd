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
import {Comparison, DependencyRevisions, MaterialRevisions} from "models/compare/compare";
import {DependencyMaterialAttributes} from "models/materials/types";
import {DependencyRevisionsWidget} from "./dependency_revisions_widget";
import {MaterialRevisionsWidget} from "./material_revisions_widget";

interface Attrs {
  comparisonResult: Comparison;
}

export class ComparisonResultWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    if (_.isEmpty(vnode.attrs.comparisonResult) || _.isEmpty(vnode.attrs.comparisonResult.changes)) {
      return <div/>;
    }

    return <div data-test-id="comparison-result-widget">
      {
        vnode.attrs.comparisonResult.changes.map((change) => {
          let viewBody: m.Child;
          switch (change.material.type()) {
            case "dependency":
              const pipelineAttrs = change.material.attributes()! as DependencyMaterialAttributes;
              viewBody            = <div>
                <DependencyRevisionsWidget pipelineName={pipelineAttrs.pipeline()}
                                           result={change.revision as DependencyRevisions}/>
              </div>;
              break;
            default:
              viewBody = <div>
                <MaterialRevisionsWidget result={change.revision as MaterialRevisions}/>
              </div>;
          }
          return <div data-test-id="material-changes">
            <span
              data-test-id="material-header">{change.material.typeForDisplay()} - {change.material.attributes()!.getLongDescription()}</span>
            {viewBody}
          </div>;
        })
      }
    </div>;
  }
}
