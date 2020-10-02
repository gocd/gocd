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

import {el} from "helpers/dom";

/**
 * Utility method to download arbitrary content as a named file. Most params are self-explanatory.
 *
 * @param contents: The simplest `BlobPart[]` is any raw content (binary or text), but often just a
 *                  string array. For much of our usage, (e.g., from an AJAX response) this will
 *                  probably be a single string wrapped in an array.
 * @param name:     The desired filename for the download.
 * @param mimeType: An optional MIME type; defaults to plaintext.
 */
export function downloadAsFile(contents: BlobPart[], name: string, mimeType = "text/plain") {
  const data = new Blob(contents, { type: mimeType });
  const a = el("a", { href: URL.createObjectURL(data), download: name, style: "display:none" }, []);
  document.body.appendChild(a); // Firefox requires this to be added to the DOM before click()
  a.click();
  document.body.removeChild(a);
}
