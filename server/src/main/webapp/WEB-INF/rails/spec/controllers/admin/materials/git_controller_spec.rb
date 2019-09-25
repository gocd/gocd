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

require 'rails_helper'
require_relative 'material_controller_examples'

describe Admin::Materials::GitController do
  before do
    @material = MaterialConfigsMother.gitMaterialConfig("git://foo.com")
    @short_material_type = 'git'
  end

  it_should_behave_like :material_controller

  def new_material
    com.thoughtworks.go.helper.MaterialConfigsMother.git("")
  end

  def assert_successful_create
    git_material_config = com.thoughtworks.go.helper.MaterialConfigsMother.git("new-url", "some-branch")
    git_material_config.setName(CaseInsensitiveString.new('new-some-kinda-material'))
    git_material_config.setConfigAttributes({GitMaterialConfig::FOLDER => "folder"})
    expect(@pipeline.materialConfigs().get(1)).to eq(git_material_config)
  end

  def update_payload
    {:materialName =>  "new-some-kinda-material", :url => "new-url", :folder => "folder", :branch => "some-branch"}
  end

  def assert_successful_update
    expect(@pipeline.materialConfigs().get(0).getUrl()).to eq("new-url")
    expect(@pipeline.materialConfigs().get(0).getName()).to eq(CaseInsensitiveString.new("new-some-kinda-material"))
  end

  def setup_other_form_objects
    #nothing to load
  end

  def setup_for_new_material
    #nothing to load
  end
end
