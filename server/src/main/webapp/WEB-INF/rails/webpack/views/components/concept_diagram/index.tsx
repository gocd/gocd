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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import css from "./index.scss";

interface Attrs {
  image: string;
  css?: typeof css;
  adaptiveWidth?: boolean;
}

export class ConceptDiagram extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    const styles = vnode.attrs.css || css;
    return <figure class={classnames(styles.conceptDiagram, {[styles.adaptive]: !!vnode.attrs.adaptiveWidth})}>
      <object type="image/svg+xml" data={vnode.attrs.image}/>
      <figcaption>
        {vnode.children}
      </figcaption>
    </figure>;
  }
}
