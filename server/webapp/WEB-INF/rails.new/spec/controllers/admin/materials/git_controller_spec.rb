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
load File.join(File.dirname(__FILE__), 'material_controller_examples.rb')

describe Admin::Materials::GitController do
  before do
    @material = MaterialConfigsMother.gitMaterialConfig("git://foo.com")
    @short_material_type = 'git'
  end

  it_should_behave_like :material_controller

  def new_material
    GitMaterialConfig.new("")
  end

  def assert_successful_create
    git_material_config = GitMaterialConfig.new("new-url", "some-branch")
    git_material_config.setName(CaseInsensitiveString.new('new-some-kinda-material'))
    git_material_config.setConfigAttributes({GitMaterialConfig::FOLDER => "folder"})
    @pipeline.materialConfigs().get(1).should == git_material_config
  end

  def update_payload
    {:materialName =>  "new-some-kinda-material", :url => "new-url", :folder => "folder", :branch => "some-branch"}
  end

  def assert_successful_update
    @pipeline.materialConfigs().get(0).getUrl().should == "new-url"
    @pipeline.materialConfigs().get(0).getName().should == CaseInsensitiveString.new("new-some-kinda-material")
  end

  def setup_other_form_objects
    #nothing to load
  end

  def setup_for_new_material
    #nothing to load
  end
end
