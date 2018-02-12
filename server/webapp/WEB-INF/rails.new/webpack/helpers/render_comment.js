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

const _ = require('lodash');

function renderComment(text, trackingTool) {
  const escapedText = _.escape(text);
  if (!trackingTool || !trackingTool.regex) {
    return escapedText;
  }
  let updatedComment = escapedText;
  try {
    updatedComment = escapedText.replace(new RegExp(trackingTool.regex, "g"), toLink);
  } catch (e) {
    return escapedText;
  }

  return updatedComment;

  function firstMatchingGroup(matchResult) {
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

  function hasGroups(regex) {
    return (new RegExp(`${regex}|`)).exec('').length - 1;
  }

  function toLink(matchedWord) {
    const trackingId = firstMatchingGroup(matchedWord.match(trackingTool.regex));
    if (trackingId) {
      const href = trackingTool.link.replace("${ID}", trackingId);
      return `<a target="story_tracker" href="${href}">${_.escape(matchedWord)}</a>`;
    } else if (hasGroups(trackingTool.regex)) {
      return matchedWord;
    } else {
      const href = trackingTool.link.replace("${ID}", matchedWord);
      return `<a target="story_tracker" href="${href}">${matchedWord}</a>`;
    }
  }
}

module.exports = renderComment;
