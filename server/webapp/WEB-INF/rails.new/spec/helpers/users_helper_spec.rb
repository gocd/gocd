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

require 'spec_helper'

describe UsersHelper do
  include UsersHelper

  it "should create an array from user search model attributes" do
    user_search_model = UserSearchModel.new(User.new("foo", "Mr. Foo", "foo@bar.com"))

    expect(user_search_model_to_array(user_search_model)).to eq("['foo', 'Mr. Foo', 'foo@bar.com']")
  end

  #support ticket 7044
  it "should escape apostrophe in display name" do
    user_search_model = UserSearchModel.new(User.new("foo", "Mr. O' Brien", "foo@bar.com"))

    expect(user_search_model_to_array(user_search_model)).to eq("['foo', 'Mr. O\\' Brien', 'foo@bar.com']")
  end
end
