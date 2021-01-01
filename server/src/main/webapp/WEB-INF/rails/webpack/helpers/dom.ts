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

type Child = string | Node;

const EVENT_HANDLER_ATTR = /^on([a-z]+)$/; // may expand this regex if need to accommodate non-standard event names

export function el(tag: HTMLElement | string, options: any, children: Child | Child[]): HTMLElement {
  const n = isHtmlElement(tag) ? tag : document.createElement(tag);

  if (options) {
    for (const key of Object.keys(options)) {
      const maybeEvent = key.toLowerCase().match(EVENT_HANDLER_ATTR);
      const value = options[key];

      if (maybeEvent && isHandlerAttrValue(value)) {
        const [, evt] = maybeEvent!;
        const handler = "string" === typeof value ? new Function("event", value).bind(n) : value;

        n.addEventListener(evt, handler);
      } else {
        n.setAttribute(key, value);
      }
    }
  }

  if (children instanceof Array) {
    for (const child of children) {
      appendTo(n, asNode(child));
    }
  } else {
    appendTo(n, asNode(children));
  }
  return n;
}

export function replaceWith<T extends ChildNode>(src: T, dst: T): T {
  if ("function" === typeof src.replaceWith) {
    src.replaceWith(dst);
  } else {
    if (src.parentElement) {
      src.parentElement.replaceChild(dst, src);
    }
  }

  return dst;
}

export function empty<T extends Node>(el: T): T {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}

export function removeEl(element: Element) {
  if (element.parentElement) {
    element.parentElement.removeChild(element);
  }
}

export function isHtmlElement(el: any): el is HTMLElement {
  return !!(el as HTMLElement).classList;
}

export function isChildNode(el: any): el is ChildNode {
  return "function" === typeof (el as ChildNode).remove;
}

function isHandlerAttrValue(value: any): value is (Function | string) { // tslint:disable-line ban-types
  return "function" === typeof value || "string" === typeof value;
}

function appendTo(el: Node, child?: Node) {
  if (child) { el.appendChild(child); }
}

function asNode(subj: Child): Node | undefined {
  if ("string" === typeof subj) {
    if ("" === subj) { return undefined; }
    return document.createTextNode(subj);
  }

  if (subj instanceof Node) {
    return subj;
  }

  throw new TypeError(`Expected ${subj} to be either a string or Node`);
}
