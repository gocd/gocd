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
import {RestyleAttrs, RestyleComponent, RestyleViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import * as defaultStyles from "./index.scss";

type Styles = typeof defaultStyles;

export enum ButtonIcon {
  ADD,
  REMOVE,
  DOC,
  FILTER,
  GEAR,
  SPINNER
}

export type Alignment = "right" | "left";

export interface OnClickHandler {
  onclick?: (e: MouseEvent) => void;
}

export interface Attrs extends OnClickHandler, RestyleAttrs<Styles> {
  icon?: ButtonIcon;
  small?: boolean;
  disabled?: boolean;
  dropdown?: boolean;
  align?: Alignment;
}

abstract class Button extends RestyleViewComponent<Styles, Attrs> {
  css: Styles = defaultStyles;

  static isHtmlAttr(key: string): boolean {
    switch (key) {
      case "icon":
      case "small":
      case "dropdown":
      case "align":
      case "css":
        return false;
      default:
        return true;
    }
  }

  static onlyHtmlAttrs(attrs: Attrs): any {
    const result: any = {};
    for (const key in attrs) {
      if (Button.isHtmlAttr(key)) {
        result[key] = attrs[key];
      }
    }
    return result;
  }

  abstract type(): string;

  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const isDropdown = vnode.attrs.dropdown;

    return (
      <button {...Button.onlyHtmlAttrs(vnode.attrs)}
              class={classnames(
                this.css.button,
                {[this.css.btnDropdown]: isDropdown},
                Button.iconClass(vnode.attrs.icon, this.css),
                Button.alignClass(vnode.attrs.align, this.css),
                this.type(),
                {[this.css.btnSmall]: isSmall}
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
      case ButtonIcon.REMOVE:
        return css.iconRemove;
      case ButtonIcon.GEAR:
        return css.iconGear;
      case ButtonIcon.SPINNER:
        return css.iconSpinner;
    }
    return "";
  }

  private static alignClass(align?: Alignment, css?: Styles) {
    css = css || defaultStyles;

    switch (align) {
      case "left":
        return css.leftAlign;
      case "right":
        return css.rightAlign;
    }
    return "";
  }
}

export class Danger extends Button {
  type(): string {
    return this.css.btnDanger;
  }
}

export class Primary extends Button {
  type(): string {
    return this.css.btnPrimary;
  }
}

export class Secondary extends Button {
  type(): string {
    return this.css.btnSecondary;
  }
}

export class Reset extends Button {
  type(): string {
    return this.css.btnReset;
  }
}

export class Cancel extends Button {
  type(): string {
    return this.css.btnCancel;
  }
}

export class Link extends Button {
  type(): string {
    return this.css.btnLink;
  }
}

export class Default extends Button {
  type(): string {
    return this.css.btnDefault;
  }
}

export interface SimpleDDAttrs {
  text: m.Children;
  disabled?: boolean;
  title?: string;
  "aria-label"?: string;
  classes?: string[];
}

export class ButtonGroup extends RestyleViewComponent<Styles, RestyleAttrs<Styles>> {
  css: Styles = defaultStyles;

  view(vnode: m.Vnode<RestyleAttrs<Styles>>) {
    return (
      <div class={this.css.buttonGroup} aria-label="actions">
        {vnode.children}
      </div>
    );
  }
}

export interface DropdownAttrs extends RestyleAttrs<Styles> {
  show: Stream<boolean>;
}

export abstract class Dropdown<V = {}> extends RestyleComponent<Styles, DropdownAttrs & V> {
  css: Styles = defaultStyles;

  oninit(vnode: m.Vnode<DropdownAttrs & V>) {
    super.oninit(vnode);
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
      <div class={classnames(this.classNames(), this.css.btnDropdownContainer)}
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

export class SimpleDropdown extends Dropdown<SimpleDDAttrs> {
  additionalClasses = "";

  view(vnode: m.Vnode<DropdownAttrs & SimpleDDAttrs>) {
    this.additionalClasses = (vnode.attrs.classes || []).join(" ");
    return super.view(vnode);
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & SimpleDDAttrs>) {
    return <Primary dropdown={true} disabled={vnode.attrs.disabled} title={vnode.attrs.title} aria-label={vnode.attrs["aria-label"]} onclick={(e) => this.toggleDropdown(vnode, e)}>
      {vnode.attrs.text}
    </Primary>;
  }

  protected doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & SimpleDDAttrs>) {
    if (vnode.attrs.show()) {
      return vnode.children;
    }
  }

  protected classNames() {
    return this.additionalClasses;
  }
}
