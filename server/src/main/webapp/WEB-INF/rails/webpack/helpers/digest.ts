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

import "mithril"; // ensure that Promise has been polyfilled

/**
 * Provides a consistent interface to compute hash digests. Implements just enough to make this work
 * in legacy browsers (e.g., older/iOS Safari, IE11, etc) with minimal effort. If we need more than
 * just this function, it might be better to just use something like https://github.com/vibornoff/webcrypto-shim
 *
 * @param algo: desired hash algorithm, e.g., "SHA-256"
 * @param message: the content to digest
 *
 * @returns a Promise<string> which resolves to the hex digest of the given message
 */
export function digest(algo: string, message: string): Promise<string> {
  const crypto = window.crypto || ((window as unknown) as IEWindow).msCrypto;
  const subtle = crypto.subtle || (crypto as WebkitCrypto).webkitSubtle;

  const maybePromise = subtle.digest(algo, byteArray(message));

  return new Promise<string>((res, rej) => {
    if (isCryptoOp(maybePromise)) { // CryptoOperation
      const op = maybePromise as MSCryptoOperation;
      op.addEventListener("complete", (e) => {
        res(hexString(e.target.result));
      }, false);
      op.addEventListener("error", rej, false);
      op.addEventListener("abort", rej, false);
    } else {
      maybePromise.then((buf) => {
        res(hexString(buf));
      }, rej);
    }
  });
}

/** Convenience function to compute a SHA-256 digest. Merely delegates/wraps digest(). */
export function sha256(message: string) {
  return digest("SHA-256", message);
}

interface IEWindow extends Window {
  msCrypto: Crypto;
}

interface WebkitCrypto extends Crypto {
  webkitSubtle: SubtleCrypto;
}

interface MSCryptoEvent extends Event {
  target: MSCryptoEventTarget;
}

interface MSCryptoEventTarget extends EventTarget {
  result: any;
}

interface MSCryptoOperation {
  addEventListener(type: string, fn: (e: MSCryptoEvent) => void, capture?: boolean): void;
}

function isCryptoOp(op: any): op is MSCryptoOperation {
  return "function" === typeof (op as MSCryptoOperation).addEventListener;
}

/** @returns a hexadecimal string representing the given ArrayBuffer */
function hexString(buf: ArrayBuffer): string {
  const ba = new Uint8Array(buf);
  const hexes = new Array(ba.length);

  for (let i = ba.length - 1; i >= 0; i--) {
    hexes[i] = ba[i].toString(16).padStart(2, "0");
  }

  return hexes.join("");
}

/** @returns a Uint8Array representing the given string */
function byteArray(subj: string) {
  if ("function" === typeof TextEncoder) {
    return new TextEncoder().encode(subj);
  }

  // Pre-Chromium MS Edge browsers do not support TextEncoder
  return Uint8Array.from(subj.split("").map((s) => s.charCodeAt(0)));
}
