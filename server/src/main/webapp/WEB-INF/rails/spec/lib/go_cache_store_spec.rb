#
# Copyright 2021 ThoughtWorks, Inc.
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

describe GoCacheStore do
  before :each do
    @go_cache = Spring.bean("goCache")
    @go_cache.clear
    @store = GoCacheStore.new()
  end

  after :each do
    @go_cache.clear
  end

  it "should be the default cache store" do
      expect(ActionController::Base.cache_store.class).to eq GoCacheStore
  end

  it "should write to underlying cruise cache" do
    @store.write("name", "value")
    expect(@store.fetch("name")).to eq "value"
    expect(@go_cache.get("name").value).to eq "value"
  end

  it "should honor writing under subkey to underlying cruise cache" do
    @store.write("name", "value", :subkey => "sub")
    expect(@go_cache.get("name", "sub").value).to eq "value"
  end

  it "should read from underlying cruise cache" do
    @go_cache.put("read_key", ActiveSupport::Cache::Entry.new("read_value"))
    expect(@store.read("read_key")).to eq "read_value"
  end

  it "should honor subkey reading from underlying cruise cache" do
    @go_cache.put("read_key", "sub", ActiveSupport::Cache::Entry.new("read_value"))
    expect(@store.read("read_key", :subkey => "sub")).to eq "read_value"
  end

  it "should delete from underlying cruise cache" do
    @go_cache.put("delete_key", ActiveSupport::Cache::Entry.new("value"))
    expect(@store.delete("delete_key").value).to eq "value"
    expect(@store.read("delete_key")).to eq nil
    expect(@go_cache.get("delete_key")).to eq nil
  end

  it "should delete subkey from underlying cruise cache" do
    @go_cache.put("delete_key", "sub", ActiveSupport::Cache::Entry.new("value"))
    expect(@store.delete("delete_key", :subkey => "sub").value).to eq "value"
    expect(@store.read("delete_key", :subkey => "sub")).to eq nil
    expect(@go_cache.get("delete_key", "sub")).to eq nil
  end

  it "should understand if key exists in cruise cache" do
    @go_cache.put("key", ActiveSupport::Cache::Entry.new("value"))
    expect(@store.exist?("key")).to be_truthy
    @go_cache.remove("key")
    expect(@store.exist?("key")).to be_falsey
    expect(@store.exist?("nokey")).to be_falsey
  end

  it "should understand if key, subkey combination exists in cruise cache" do
    @go_cache.put("key", "sub", ActiveSupport::Cache::Entry.new("value"))
    expect(@store.exist?("key", :subkey => "sub")).to be_truthy
    @go_cache.remove("key", "sub")
    expect(@store.exist?("key", :subkey => "sub")).to be_falsey
    expect(@store.exist?("nokey", :subkey => "sub")).to be_falsey
  end

  it "should clear cruise cache" do
    @go_cache.put("key", ActiveSupport::Cache::Entry.new("value"))
    @store.clear
    expect(@go_cache.get("key")).to eq nil
  end

  it "should convert Ruby string values into Java string values" do
    some_value = "some ruby string"
    key = SecureRandom.hex
    @store.write(key, some_value)

    expect(@store.fetch(key)).to be_an_instance_of(java.lang.String)
    expect(@store.fetch(key)).not_to be_an_instance_of(String)
    expect(@store.fetch(key)).to eq(java.lang.String.new("some ruby string"))

    expect(@go_cache.get(key).value).to be_an_instance_of(java.lang.String)
    expect(@go_cache.get(key).value).not_to be_an_instance_of(String)
    expect(@go_cache.get(key).value).to eq(java.lang.String.new("some ruby string"))
  end
end

