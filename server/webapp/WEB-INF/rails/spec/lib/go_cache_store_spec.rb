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

require File.join(File.dirname(__FILE__), "..", "spec_helper")

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
      ActionController::Base.cache_store.should be_a GoCacheStore
  end

  it "should write to underlying cruise cache" do
    @store.write("name", "value")
    @go_cache.get("name").should == "value"
  end

  it "should honor writing under subkey to underlying cruise cache" do
    @store.write("name", "value", :subkey => "sub")
    @go_cache.get("name", "sub").should == "value"
  end

  it "should read from underlying cruise cache" do
    @go_cache.put("read_key", "read_value")
    @store.read("read_key").should == "read_value"
  end

  it "should honor subkey reading from underlying cruise cache" do
    @go_cache.put("read_key", "sub", "read_value")
    @store.read("read_key", :subkey => "sub").should == "read_value"
  end

  it "should delete from underlying cruise cache" do
    @go_cache.put("delete_key", "value")
    @store.delete("delete_key").should == "value"
    @store.read("delete_key").should be_nil
    @go_cache.get("delete_key").should be_nil
  end

  it "should delete subkey from underlying cruise cache" do
    @go_cache.put("delete_key", "sub", "value")
    @store.delete("delete_key", :subkey => "sub").should == "value"
    @store.read("delete_key", :subkey => "sub").should be_nil
    @go_cache.get("delete_key", "sub").should be_nil
  end

  it "should understand if key exists in cruise cache" do
    @go_cache.put("key", "value")
    @store.exist?("key").should be_true 
    @go_cache.remove("key")
    @store.exist?("key").should be_false

    @store.exist?("nokey").should be_false
  end

  it "should understand if key, subkey combination exists in cruise cache" do
    @go_cache.put("key", "sub", "value")
    @store.exist?("key", :subkey => "sub").should be_true
    @go_cache.remove("key", "sub")
    @store.exist?("key", :subkey => "sub").should be_false

    @store.exist?("nokey", :subkey => "sub").should be_false
  end

  it "should clear cruise cache" do
    @go_cache.put("key", "value")
    @store.clear
    @go_cache.get("key").should be_nil
  end
end

