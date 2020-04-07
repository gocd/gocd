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

import {ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message/index";

describe("Flash Message", () => {

  let flashMessage: FlashMessageModelWithTimeout;

  beforeEach(() => {
    flashMessage = new FlashMessageModelWithTimeout();
  });

  it("should set message and type", () => {
    const message = "Success!";
    flashMessage.setMessage(MessageType.success, message);

    expect(flashMessage.type).toBe(MessageType.success);
    expect(flashMessage.message).toBe(message);
  });

  describe("Consume Error Response", () => {
    it("should show error message", () => {
      const message = "top level message";

      const errorResponse: ErrorResponse = {message};
      flashMessage.consumeErrorResponse(errorResponse);

      expect(flashMessage.type).toBe(MessageType.alert);
      expect(flashMessage.message).toBe(errorResponse.message);
    });

    it("should show error message from body when one exists", () => {
      const message = "message from body";

      const errorResponse: ErrorResponse = {
        message: "",
        body: JSON.stringify({message})
      };

      flashMessage.consumeErrorResponse(errorResponse);

      expect(flashMessage.type).toBe(MessageType.alert);
      expect(flashMessage.message).toBe(message);
    });

    it("should show error message from body when both error exists", () => {
      const topLevelMessage = "message from body";
      const bodyMessage     = "message from body";

      const errorResponse: ErrorResponse = {
        message: topLevelMessage,
        body: JSON.stringify({message: bodyMessage})
      };

      flashMessage.consumeErrorResponse(errorResponse);

      expect(flashMessage.type).toBe(MessageType.alert);
      expect(flashMessage.message).toBe(bodyMessage);
    });

    it("should show any errors that exists at the top level", () => {
      const data = {
        errors: {
          stage: "pipeline save failed because stage 'stage1' does not exists",
          base: "save failed"
        }
      };

      const errorResponse: ErrorResponse = {
        message: "Something failed",
        body: JSON.stringify({data})
      };

      const expectedMessageForDisplay = <div>
        {errorResponse.message}
        <ul>
          <li>{data.errors.stage}</li>
          <li>{data.errors.base}</li>
        </ul>
      </div>;

      flashMessage.consumeErrorResponse(errorResponse);
      expect(flashMessage.type).toBe(MessageType.alert);
      expect(flashMessage.message).toEqual(expectedMessageForDisplay);
    });
  });
});
