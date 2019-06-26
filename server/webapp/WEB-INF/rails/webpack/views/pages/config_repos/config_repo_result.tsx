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

import classnames from "classnames";
import {override} from "helpers/css_proxies";
import VMRoutes from "helpers/vm_routes";
import {MithrilComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";
import {treeMap} from "models/base/traversable";
import {DefinedEnvironment, DefinedGroup, DefinedPipeline, DefinedStructures, NamedTree} from "models/config_repos/defined_pipelines";
import {EventAware} from "models/mixins/event_aware";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Tree} from "views/components/hierarchy/tree";
import {Spinner} from "views/components/icons/index";
import * as css from "./defined_structs.scss";

// @ts-ignore
import * as Routes from "gen/js-routes";

type Styles = typeof css;

interface Attrs {
  repo: string;
  vm: EventAware;
  cache?: ObjectCache<DefinedStructures>;
}

interface State {
  cache: ObjectCache<DefinedStructures>;
}

export class CRResultCache extends AbstractObjCache<DefinedStructures> {
  private repoId: string;

  constructor(repoId: string) {
    super();
    this.repoId = repoId;
  }

  doFetch(resolve: (data: DefinedStructures) => void, reject: (reason: string) => void) {
    DefinedStructures.fetch(this.repoId).then(resolve).catch(rejectAsString(reject));
  }
}

export class CRResult extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    const cache = vnode.attrs.cache || new CRResultCache(vnode.attrs.repo);
    vnode.attrs.vm.on("expand", () => {
      cache.prime(m.redraw);
    });
    vnode.attrs.vm.on("refresh", () => { cache.invalidate(); cache.prime(m.redraw); });
    vnode.state.cache = cache;
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children {
    if (vnode.state.cache.failed()) {
      return <FlashMessage type={MessageType.alert}>
        Failed to load pipelines defined in this repository: {vnode.state.cache.failureReason()}
      </FlashMessage>;
    }

    if (vnode.state.cache.ready()) {
      const root = vnode.state.cache.contents();

      if (!root.children.length) {
        return <FlashMessage type={MessageType.alert} message="This repository does not define any pipelines or environments."/>;
      }

      return treeMap<NamedTree, m.Vnode>(root, tree);
    }

    return <div class={css.loading}><Spinner iconOnly={true}/> Loading pipelines defined by repository&hellip;</div>;
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
      return <a href={Routes.pipelineGroupShowPath(node.name())}>{node.name()}</a>;
    }

    if (node instanceof DefinedPipeline) {
      return <a href={VMRoutes.pipelineHistoryPath(node.name())}>{node.name()}</a>;
    }

    if (node instanceof DefinedEnvironment) {
      return <a href={Routes.environmentShowPath(node.name())}>{node.name()}</a>;
    }

    return node.name();
  }
}

function tree(n: NamedTree): m.Vnode {
  return <Tree css={Css.for(n)} datum={Link.for(n)}/>;
}
