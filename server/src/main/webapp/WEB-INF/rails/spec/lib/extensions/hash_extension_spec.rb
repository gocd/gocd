#
# Copyright 2020 ThoughtWorks, Inc.
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

require 'rails_helper'

describe Hash do

  it "should understand selecting only given keys" do
    expect({ :foo => "bar", :baz => "bang", :quux => "quux"}.only(:foo, :quux)).to eq({ :foo => "bar", :quux => "quux"})
  end

  describe "for indifferent access" do
    before do
      @hash = HashWithIndifferentAccess.new("foo" => "bar", :baz => "bang", :quux => "quux").only(:foo, :quux)
    end

    it "should fetch entries indifferently" do
      expect(@hash[:foo]).to eq("bar")
      expect(@hash[:quux]).to eq("quux")
    end

    it "should be hash with indifferent access" do
      expect(@hash.class).to eq(HashWithIndifferentAccess)
    end
  end
end


