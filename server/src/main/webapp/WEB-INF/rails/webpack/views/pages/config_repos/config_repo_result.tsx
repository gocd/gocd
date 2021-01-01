/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {ObjectCache} from "models/base/cache";
import {treeMap} from "models/base/traversable";
import {DefinedEnvironment, DefinedGroup, DefinedPipeline, DefinedStructures, NamedTree} from "models/config_repos/defined_structures";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Tree} from "views/components/hierarchy/tree";
import css from "./defined_structs.scss";

type Styles = typeof css;

interface CacheProvider {
  results: ObjectCache<DefinedStructures>;
}

interface Attrs {
  vm: CacheProvider;
}

export class CRResult extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
    const vm = vnode.attrs.vm;

    if (vm.results.failed()) {
      return <FlashMessage type={MessageType.alert}>
        Failed to load pipelines defined in this repository: {vm.results.failureReason()}
      </FlashMessage>;
    }

    // Prefer to keep showing stale contents between invalidation and refresh to avoid flickering
    // between loading message and updated structure. This implies that the user only sees the
    // loading message briefly (on reasonably fast latencies, just a flicker) upon first load.
    // Maybe we'll add a spinner off to the side if we feel it's necessary, but flickering between
    // this and the loading message feels wrong from a usability standpoint, IMO.
    if (vm.results.ready() || vm.results.contents()) {
      const root = vm.results.contents();

      if (!root.children.length) {
        return <FlashMessage type={MessageType.alert} message="This repository does not define any pipelines or environments."/>;
      }

      return treeMap<NamedTree, m.Vnode>(root, tree);
    }

    return <div class={css.loading}><i class={css.spin}/>Loading pipelines defined by repository&hellip;</div>;
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

  static readonly envs: Styles = override<Styles>(css, {
    ["tree"]:      classnames(css.environment, css.tree),
    ["treeDatum"]: classnames(css.environmentDatum, css.treeDatum),
  });

  static for(node: NamedTree): Styles | undefined {
    if (node instanceof DefinedGroup) {
      return Css.groups;
    }

    if (node instanceof DefinedPipeline) {
      return Css.pipelines;
    }

    if (node instanceof DefinedEnvironment) {
      return Css.envs;
    }

    return css;
  }
}

class Link {
  static for(node: NamedTree): m.Child {
    if (node instanceof DefinedStructures) {
      return "Groups, pipelines, and environments defined by this repository:";
    }

    if (node instanceof DefinedGroup) {
      return <a data-test-id={_.snakeCase("group " + node.name())} href={SparkRoutes.pipelineGroupsSPAPath(node.name())}>{node.name()}</a>;
    }

    if (node instanceof DefinedPipeline) {
      return <a data-test-id={_.snakeCase("pipeline " + node.name())} href={SparkRoutes.pipelineEditPath('pipelines', node.name(), 'general')}>{node.name()}</a>;
    }

    if (node instanceof DefinedEnvironment) {
      return <a data-test-id={_.snakeCase("environment " + node.name())} href={SparkRoutes.getEnvironmentPathOnSPA(node.name())}>{node.name()}</a>;
    }

    return node.name();
  }
}

function tree(n: NamedTree): m.Vnode {
  return <Tree css={Css.for(n)} datum={Link.for(n)}/>;
}
