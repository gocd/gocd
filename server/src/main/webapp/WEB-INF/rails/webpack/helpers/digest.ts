/*
 * Copyright 2024 Thoughtworks, Inc.
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

/**
 * Provides a consistent interface to compute hash digests as hex strings.
 *
 * @param algo desired hash algorithm, e.g., "SHA-256"
 * @param message the content to digest
 *
 * @returns a Promise<string> which resolves to the hex digest of the given message
 */
export function digest(algo: string, message: string): Promise<string> {
  return window.crypto.subtle.digest(algo, byteArray(message))
    .then(buf => {
      return hexString(buf);
    });
}

/** Convenience function to compute a SHA-256 digest. Merely delegates/wraps digest(). */
export function sha256(message: string) {
  return digest("SHA-256", message);
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
  return new TextEncoder().encode(subj);
}
