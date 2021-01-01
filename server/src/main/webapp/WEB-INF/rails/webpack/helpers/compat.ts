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

// OK, so we don't support IE, but some SPAs won't even render in IE to display the
// unsupported browser message in the first place because of JS errors. Implementing
// working equivalents isn't so bad, so far, so may as well make it work in at least
// IE 11 so GoCD isn't _totally_ broken.
interface IEElement extends HTMLElement {
  msMatchesSelector(sel: string): boolean;
}

interface IEWindow extends Window {
  clipboardData: {
    getData: (key: string) => string;
  };
}

export function matches(el: Element, selector: string): boolean {
  if ("function" === typeof el.matches) {
    return el.matches(selector);
  }

  return (el as IEElement).msMatchesSelector(selector);
}

export function closest(el: Element, selector: string): Element | null {
  if ("function" === typeof el.closest) {
    return el.closest(selector);
  }

  let result: Element | null = el;

  while (result && !matches(result, selector)) {
    result = result.parentElement;
  }

  return result;
}

export function getClipboardAsPlaintext(e: ClipboardEvent): string {
  if (e.clipboardData) {
    return e.clipboardData.getData("text/plain");
  }
  return ((window as unknown) as IEWindow).clipboardData.getData("Text");
}

export function insertTextFromClipboard(text: string) {
  if (document.queryCommandSupported("insertText")) {
    document.execCommand("insertText", false, text);
  } else {
    const range = getSelection()!.getRangeAt(0);
    range.deleteContents();
    range.insertNode(document.createTextNode(text));
  }
}

export function makeEvent(type: string, bubbles: boolean = true, cancelable: boolean = true): Event {
  if ("function" === typeof Event) {
      return new Event(type, { bubbles, cancelable });
  }

  const event = document.createEvent("Event");
  event.initEvent(type, bubbles, cancelable);

  return event;
}

export function getSelection() {
  return window.getSelection() || document.getSelection();
}
