/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {bind} from "classnames/bind";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as defaultStyles from "./index.scss";

const defaultClassnames = bind(defaultStyles);

type Styles = typeof defaultStyles;

export enum ButtonIcon {
  ADD,
  DOC,
  FILTER,
}

export interface OnClickHandler {
  onclick?: (e: MouseEvent) => void;
}

interface Restyleable<T> {
  css?: T;
}

export interface Attrs extends OnClickHandler, Restyleable<Styles> {
  icon?: ButtonIcon;
  small?: boolean;
  disabled?: boolean;
  dropdown?: boolean;
}

abstract class Button extends MithrilViewComponent<Attrs> {
  protected cls = defaultClassnames;
  private stylesheet: Styles = defaultStyles;

  abstract type(): string;

  css(): Styles {
    return this.stylesheet;
  }

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.css) {
      this.stylesheet = vnode.attrs.css;
      this.cls = bind(vnode.attrs.css);
    }
  }

  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const isDropdown = vnode.attrs.dropdown;

    return (
      <button {...vnode.attrs}
              onclick={vnode.attrs.onclick}
              class={this.cls(
                this.css().button,
                {[this.css().btnSmall]: isSmall, [this.css().btnDropdown]: isDropdown},
                Button.iconClass(vnode.attrs.icon, this.css()),
                this.type()
              )}>
        {vnode.children}
      </button>
    );
  }

  private static iconClass(icon?: ButtonIcon, css?: Styles) {
    css = css || defaultStyles;

    switch (icon) {
      case ButtonIcon.ADD:
        return css.iconAdd;
      case ButtonIcon.DOC:
        return css.iconDoc;
      case ButtonIcon.FILTER:
        return css.iconFilter;
    }
    return "";
  }
}

export class Danger extends Button {
  type(): string {
    return this.css().btnDanger;
  }
}

export class Primary extends Button {
  type(): string {
    return this.css().btnPrimary;
  }
}

export class Secondary extends Button {
  type(): string {
    return this.css().btnSecondary;
  }
}

export class Reset extends Button {
  type(): string {
    return this.css().btnReset;
  }
}

export class Cancel extends Button {
  type(): string {
    return this.css().btnCancel;
  }
}

export class Link extends Button {
  type(): string {
    return this.css().btnLink;
  }
}

export class Default extends Button {
  type(): string {
    return this.css().btnDefault;
  }
}

export class ButtonGroup extends MithrilViewComponent<Restyleable<Styles>> {
  view(vnode: m.Vnode<Restyleable<Styles>>) {
    const css = vnode.attrs.css || defaultStyles;

    return (
      <div class={css.buttonGroup} aria-label="actions">
        {vnode.children}
      </div>
    );
  }
}

export interface DropdownAttrs extends Restyleable<Styles> {
  show: Stream<boolean>;
}

export abstract class Dropdown<V = {}> extends MithrilComponent<DropdownAttrs & V> {
  private cls = defaultClassnames;
  private stylesheet: Styles = defaultStyles;

  css(): Styles {
    return this.stylesheet;
  }

  oninit(vnode: m.Vnode<DropdownAttrs & V>) {
    if (vnode.attrs.css) {
      this.stylesheet = vnode.attrs.css;
      this.cls = bind(vnode.attrs.css);
    }
    document.body.addEventListener("click", this.closeDropdown.bind(this, vnode));
  }

  onremove(vnode: m.VnodeDOM<DropdownAttrs & V>) {
    document.body.removeEventListener("click", this.closeDropdown.bind(this, vnode));
  }

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & V>, e: MouseEvent) {
    vnode.attrs.show(!vnode.attrs.show());
  }

  stopPropogation(e: Event) {
    e.stopPropagation();

    // redraw specifically and intentioally set to false here.
    // The reason is that, after any event, mithril's auto-redraw system redraws the component. This resets the changes in any input fields that user might have done on view.
    // To avoid that, redraw needs to set as false.
    // refer: https://mithril.js.org/autoredraw.html
    //@ts-ignore
    e.redraw = false;
  }

  view(vnode: m.Vnode<DropdownAttrs & V>) {
    return (
      <div class={this.cls(this.classNames(), this.css().btnDropdownContainer)}
           onclick={this.stopPropogation}>
        {this.doRenderButton(vnode)}
        {this.doRenderDropdownContent(vnode)}
      </div>
    );
  }

  protected classNames(): string | undefined {
    return undefined;
  }

  protected closeDropdown(vnode: m.Vnode<DropdownAttrs & V>) {
    vnode.attrs.show(false);
    m.redraw();
  }

  protected abstract doRenderButton(vnode: m.Vnode<DropdownAttrs & V>): m.Children;

  protected abstract doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & V>): m.Children;
}
