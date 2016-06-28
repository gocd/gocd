##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

require 'spec_helper'
describe ApiV2::Config::ErrorRepresenter do

  it "should represent an error" do
    errors = ConfigErrors.new
    errors.add("key1", "Key1Msg1")
    errors.add("key1", "Key1Msg2")
    errors.add("key2", "Key2Msg1")
    presenter   = ApiV2::Config::ErrorRepresenter.new(errors)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(error_hash)
  end

  it "should serialize empty errors object" do
    errors = ConfigErrors.new
    presenter   = ApiV2::Config::ErrorRepresenter.new(errors)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to be_empty
  end

  def error_hash
    {
      "key1" => ["Key1Msg1", "Key1Msg2"],
      "key2" => ["Key2Msg1"]
    }
  end
end
