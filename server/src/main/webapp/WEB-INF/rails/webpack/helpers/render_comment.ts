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
import _ from "lodash";

export interface TrackingTool {
  regex: string;
  link: string;
}

export function renderComment(text: string, trackingTool: TrackingTool) {
  if (!trackingTool || !trackingTool.regex) {
    return _.escape(text);
  }

  try {
    const regex              = new RegExp(trackingTool.regex);
    const commentStringParts = [];
    let matchResult          = text.match(regex);
    while (matchResult !== null) {
      commentStringParts.push(_.escape(text.substr(0, matchResult.index)));
      const linkifiedWord = toLink(matchResult[0]);
      commentStringParts.push(linkifiedWord);
      text        = text.substr(matchResult!.index! + matchResult[0].length);
      matchResult = text.match(regex);
    }
    commentStringParts.push(_.escape(text));
    return commentStringParts.join("");
  } catch (e) {
    return _.escape(text);
  }

  function firstMatchingGroup(matchResult: RegExpMatchArray) {
    if (matchResult === null || matchResult.length === 0) {
      return null;
    }
    for (let i = 1; i < matchResult.length; i++) {
      if (matchResult[i]) {
        return matchResult[i];
      }
    }
    return null;
  }

  function hasGroups(regex: string) {
    return (new RegExp(`${regex}|`)).exec("")!.length - 1;
  }

  function toLink(matchedWord: string) {
    const trackingId = firstMatchingGroup(matchedWord.match(trackingTool.regex)!);
    if (trackingId) {
      const href = trackingTool.link.replace("${ID}", trackingId);
      return `<a target="story_tracker" href="${href}">${_.escape(matchedWord)}</a>`;
    } else if (hasGroups(trackingTool.regex)) {
      return _.escape(matchedWord);
    } else {
      const href = trackingTool.link.replace("${ID}", _.escape(matchedWord));
      return `<a target="story_tracker" href="${href}">${_.escape(matchedWord)}</a>`;
    }
  }
}
