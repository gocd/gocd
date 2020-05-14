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

import m from "mithril";
import * as simulateEvent from "simulate-event";
import {Page} from "views/pages/page";

let mounted = false;
type ElementWithInput = HTMLInputElement | HTMLTextAreaElement;
type ElementWithValue = ElementWithInput | HTMLSelectElement;

export function stubAllMethods<T>(keys: Array<keyof T>): T {
  return keys.reduce((result, k) => {
    result[k] = jasmine.createSpy(`${k}()`) as any;
    return result;
  }, {} as T);
}

export class TestHelper {
  root?: HTMLElement;

  constructor(root?: HTMLElement) {
    this.root = root;
  }

  mount(callback: () => m.Children) {
    this.initialize();

    m.mount(this.root!, {
      view() {
        return callback();
      }
    });
    this.redraw();
  }

  mountPage(callback: () => Page<any, any>) {
    this.initialize();

    m.mount(this.root!, callback());
    this.redraw();
  }

  route(defaultRoute: string, callback: () => m.RouteDefs) {
    this.initialize();

    m.route(this.root!, defaultRoute, callback());
    this.redraw();
  }

  unmount(done?: () => void) {
    if (!mounted) {
      throw new Error("Did you forget to mount?");
    }

    mounted = false;

    m.mount(this.root!, null);
    this.redraw();

    this.root!.remove();
    this.root = undefined;

    if (done) {
      done();
    }
  }

  // native implementations - prefer these
  byTestId(id: string, context?: Element) {
    return this.q(dataTestIdSelector(id), context);
  }

  allByTestId(id: string, context?: Element): NodeListOf<HTMLElement> {
    return this.qa(dataTestIdSelector(id), context);
  }

  q(selector: string, context?: Element) {
    return (context || this.root!).querySelector(selector) as HTMLElement;
  }

  qa(selector: string, context?: Element): NodeListOf<HTMLElement> {
    return (context || this.root!).querySelectorAll(selector);
  }

  fromModalByTestId(id: string): Element {
    const modalContainer = this.q(".component-modal-container div:first-child", document.body);
    if (!modalContainer) {
      throw new Error("Did you forget to open a modal?");
    }
    return this.q(dataTestIdSelector(id), modalContainer);
  }

  closeModal() {
    this.click(this.q("header button[class*=overlay-close]", this.modal()));
  }

  modal() {
    const modalContainer = this.q(".component-modal-container div:first-child", document.body);
    if (!modalContainer) {
      throw new Error("Did you forget to open a modal?");
    }
    return modalContainer;
  }

  forModal() {
    return new TestHelper(this.modal());
  }

  textByTestId(id: string, context?: Element): string {
    return this.text(this.byTestId(id, context));
  }

  textAllByTestId(id: string, context?: Element): string[] {
    return this.textAll(this.allByTestId(id, context));
  }

  text(selector: string | Element, context?: Element): string {
    return (this._el(selector, context).textContent || "").trim();
  }

  textAll(selector: string | NodeList | Element[], context?: Element): string[] {
    const result: string[] = [];
    const elements         = Array.from("string" === typeof selector ? this.qa(selector, context) : selector);

    for (const el of elements) {
      result.push((el.textContent || "").trim());
    }

    return result;
  }

  value(selector: string | Element, context?: Element): string {
    return (this._el(selector, context) as ElementWithValue).value;
  }

  valueAll(selector: string | NodeList | Element[], context?: Element): string[] {
    const result: string[] = [];
    const elements         = Array.from("string" === typeof selector ? this.qa(selector, context) : selector);

    for (const el of elements) {
      result.push(this.value(el as Element));
    }

    return result;
  }

  onchange(selector: string | Element, value: string, context?: Element) {
    const input: ElementWithValue = this._el(selector, context) as ElementWithValue;
    input.value                   = value;
    simulateEvent.simulate(input, "change");
    this.redraw();
  }

  oninput(selector: string | Element, value: string, context?: Element) {
    const input: ElementWithInput = this._el(selector, context) as ElementWithInput;
    input.value                   = value;
    simulateEvent.simulate(input, "input");
    this.redraw();
  }

  click(selector: string | Element, context?: Element) {
    const element = this._el(selector, context);
    if (!element) {
      throw new Error(`Unable to find element with selector ${selector}`);
    }
    simulateEvent.simulate(element, "click");
    this.redraw();
  }

  clickButtonOnActiveModal(buttonSelector: string) {
    // `.new-modal-container` is the selector used by the old modals
    const element = document.querySelector(`.new-modal-container ${buttonSelector}`) || document.querySelector(`.component-modal-container ${buttonSelector}`);
    if (!element) {
      throw new Error(`Unable to find button with selector ${buttonSelector}`);
    }
    simulateEvent.simulate(element, "click");
    this.redraw();
  }

  redraw() {
    m.redraw.sync();
  }

  dump(context?: Element) {
    // tslint:disable-next-line:no-console
    console.log((context || this.root!).innerHTML);
  }

  clickByTestId(id: string, context?: Element) {
    this.click(dataTestIdSelector(id), context);
  }

  byClass(className: string) {
    return this.root!.getElementsByClassName(className)[0];
  }

  allByClass(className: string) {
    return this.root!.getElementsByClassName(className);
  }

  private _el(selector: string | Element, context?: Element): Element {
    return "string" === typeof selector ? this.q(selector, context) : selector;
  }

  private initialize() {
    if (mounted) {
      throw new Error("Did you forget to unmount from a previous test?");
    }
    mounted   = true;
    this.root = document.createElement("root");
    document.body.appendChild(this.root);
  }
}

function dataTestIdSelector(id: string): string {
  return `[data-test-id="${id}"]`;
}
