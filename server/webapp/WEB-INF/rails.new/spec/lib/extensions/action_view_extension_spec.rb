##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')
require 'action_view/helpers/form_helper'

describe ActionView::Helpers::Tags::Base do

  it "should delete the id key when 'omit_id_generation' is true" do
    options = { size: 30, omit_id_generation: true }
    input_tag = ActionView::Helpers::Tags::TextField.new("object_name", "put", self, options).render

    expect(input_tag).to eq("<input name=\"object_name[put]\" size=\"30\" type=\"text\" />")
  end

  it "should not delete the id key when 'omit_id_generation' is false" do
    options = { size: 30, omit_id_generation: false }
    input_tag = ActionView::Helpers::Tags::TextField.new("object_name", "put", self, options).render

    expect(input_tag).to eq("<input id=\"object_name_put\" name=\"object_name[put]\" size=\"30\" type=\"text\" />")
  end

  it "should remove the first input tag (with type hidden) when include_hidden field is false" do
    options = { include_hidden: false }
    checkbox_tag = ActionView::Helpers::Tags::CheckBox.new("object_name", "put", self, 1, 0, options).render

    expect(checkbox_tag).to eq("<input id=\"object_name_put\" name=\"object_name[put]\" type=\"checkbox\" value=\"1\" />")
  end

  it "should not remove the first input tag (with type hidden) when include_hidden field is true" do
    options = { include_hidden: true }
    checkbox_tag = ActionView::Helpers::Tags::CheckBox.new("object_name", "put", self, 1, 0, options).render

    expect(checkbox_tag).to eq(
        "<input name=\"object_name[put]\" type=\"hidden\" value=\"0\" /><input id=\"object_name_put\" name=\"object_name[put]\" type=\"checkbox\" value=\"1\" />")
  end
end
