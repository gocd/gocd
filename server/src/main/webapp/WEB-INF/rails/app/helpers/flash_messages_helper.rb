#
# Copyright 2019 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

module FlashMessagesHelper
  def flash_message_pane_start id, no_body = false, options = {}
    ("<div class=\"flash\" id=\"#{id}\">" + (no_body ? flash_message_pane_end : "")).html_safe
  end

  def flash_message_pane_end
    "</div>"
  end
end