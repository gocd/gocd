##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::Config::CaseInsensitiveStringRepresenter do
  it 'should serialize' do
    presenter = ApiV1::Config::CaseInsensitiveStringRepresenter.prepare(CaseInsensitiveString.new("foo"))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq("foo")
  end

  it 'should return nil when name is empty during serialization' do
    presenter = ApiV1::Config::CaseInsensitiveStringRepresenter.prepare(CaseInsensitiveString.new(""))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(nil)

    presenter = ApiV1::Config::CaseInsensitiveStringRepresenter.prepare(nil)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(nil)
  end

  it "should deserialize" do
    case_insensitive_str = ApiV1::Config::CaseInsensitiveStringRepresenter.new(nil).from_hash("foo", {})
    expect(case_insensitive_str).to eq(CaseInsensitiveString.new("foo"))
  end

  it "should skip empty value for name during deserialization" do
    expect(ApiV1::Config::CaseInsensitiveStringRepresenter.new(nil).from_hash(" ", {})).to eq(nil)
    expect(ApiV1::Config::CaseInsensitiveStringRepresenter.new(nil).from_hash("", {})).to eq(nil)
    expect(ApiV1::Config::CaseInsensitiveStringRepresenter.new(nil).from_hash(nil, {})).to eq(nil)
  end
end