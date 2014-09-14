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

describe ActionView::Helpers::InstanceTag do

  it "should delete the id key when 'omit_id_generation' is true" do
    options = {:omit_id_generation => true}
    new_tag = ActionView::Helpers::InstanceTag.new("object_name", "put", self)
    input_tag = new_tag.to_input_field_tag("text", options)
    input_tag.should == "<input name=\"object_name[put]\" size=\"30\" type=\"text\" />"
  end

  it "should not delete the id key when 'omit_id_generation' is false" do
    options = {:omit_id_generation => false}
    new_tag = ActionView::Helpers::InstanceTag.new("object_name", "put", self)
    input_tag = new_tag.to_input_field_tag("text", options)
    input_tag.should == "<input id=\"object_name_put\" name=\"object_name[put]\" size=\"30\" type=\"text\" />"
  end

  it "should remove the first input tag (with type hidden) when drop_hidden_field is true" do
    options = {:drop_hidden_field => true}
    new_tag = ActionView::Helpers::InstanceTag.new("object_name", "put", self)
    checkbox_tag = new_tag.to_check_box_tag(options)

    checkbox_tag.should == "<input id=\"object_name_put\" name=\"object_name[put]\" type=\"checkbox\" value=\"1\" />"
  end

  it "should not remove the first input tag (with type hidden) when drop_hidden_field is true" do
    options = {:drop_hidden_field => false}
    new_tag = ActionView::Helpers::InstanceTag.new("object_name", "put", self)
    checkbox_tag = new_tag.to_check_box_tag(options)

    checkbox_tag.should == "<input name=\"object_name[put]\" type=\"hidden\" value=\"0\" /><input id=\"object_name_put\" name=\"object_name[put]\" type=\"checkbox\" value=\"1\" />"

  end

end
