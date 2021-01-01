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
import {RestyleAttrs, RestyleComponent} from "jsx/mithril-component";
import m from "mithril";
import css from "./user_input_pane.scss";

type Styles = typeof css;

interface Attrs extends RestyleAttrs<Styles> {
  heading?: m.Children;
  onchange?: (e: Event) => void;
  constrainedWidth?: boolean;
}

export class UserInputPane extends RestyleComponent<Styles, Attrs> {
  css: Styles = css;

  view(vnode: m.Vnode<Attrs>) {
    const {heading, onchange} = vnode.attrs;

    return <section class={classnames(this.css.userInput, {[this.css.constrained]: !!vnode.attrs.constrainedWidth})} onchange={onchange}>
      {heading ? <SectionHeading css={this.css}>{heading}</SectionHeading> : void 0}
      {vnode.children}
    </section>;
  }
}

export class SectionHeading extends RestyleComponent<Styles> {
  css: Styles = css;

  view(vnode: m.Vnode<RestyleAttrs<Styles>>) {
    return [
      <h3 class={this.css.sectionHeading}>{vnode.children}</h3>,
      <p class={this.css.sectionNote}><span class={this.css.attention}>*</span> denotes a required field</p>
    ];
  }
}
