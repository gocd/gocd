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
import {override} from "helpers/css_proxies";
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {treeMap} from "models/base/traversable";
import {DefinedGroup, DefinedPipeline, NamedTree} from "models/config_repos/defined_structures";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Tree} from "views/components/hierarchy/tree";
import css from "views/pages/config_repos/defined_structs.scss";
import {MaterialUsagesVM} from "views/pages/materials/models/material_usages_view_model";
import {MaterialAttrs} from "./material_widget";

type Styles = typeof css;

export class MaterialUsageWidget extends MithrilViewComponent<MaterialAttrs> {
  view(vnode: m.Vnode<MaterialAttrs, this>): m.Children | void | null {
    const vm = vnode.attrs.materialVM;

    if (vm.results.failed()) {
      return <FlashMessage type={MessageType.alert}>
        Failed to load pipelines: {vm.results.failureReason()}
      </FlashMessage>;
    }

    if (vm.results.ready() || vm.results.contents()) {
      const root = vm.results.contents();

      if (!root.children.length) {
        return <FlashMessage type={MessageType.alert} message="This material is not used in any pipeline"/>;
      }

      return treeMap<NamedTree, m.Vnode>(root, tree);
    }
    return <div className={css.loading}><i className={css.spin}/>Loading pipelines &hellip;</div>;
  }
}

class Css {
  static readonly groups: Styles = override<Styles>(css, {
    ["tree"]:      classnames(css.group, css.tree),
    ["treeDatum"]: classnames(css.groupDatum, css.treeDatum),
  });

  static readonly pipelines: Styles = override<Styles>(css, {
    ["tree"]:      classnames(css.pipeline, css.tree),
    ["treeDatum"]: classnames(css.pipelineDatum, css.treeDatum),
  });

  static for(node: NamedTree): Styles | undefined {
    if (node instanceof DefinedGroup) {
      return Css.groups;
    }

    if (node instanceof DefinedPipeline) {
      return Css.pipelines;
    }

    return css;
  }
}

class Link {
  static for(node: NamedTree): m.Child {
    if (node instanceof MaterialUsagesVM) {
      return "Pipelines using this material:";
    }

    if (node instanceof DefinedGroup) {
      return <a data-test-id={_.snakeCase("group " + node.name())} href={SparkRoutes.pipelineGroupsSPAPath(node.name())}>{node.name()}</a>;
    }

    if (node instanceof DefinedPipeline) {
      return <a data-test-id={_.snakeCase("pipeline " + node.name())}
                href={SparkRoutes.pipelineEditPath('pipelines', node.name(), 'materials')}>{node.name()}</a>;
    }
    return node.name();
  }
}

function tree(n: NamedTree): m.Vnode {
  return <Tree css={Css.for(n)} datum={Link.for(n)}/>;
}
