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

describe Admin::Materials::PackageController do
  include ConfigSaveStubbing

  before do
    @material = MaterialConfigsMother.packageMaterialConfig()
    @short_material_type = 'package'
  end

  it_should_behave_like :material_controller do
    def assert_material_is_initialized
      assigns[:material].should_not == nil
      assigns[:material].getType().should == 'PackageMaterial'
    end

    def controller_specific_assertion
      assigns[:package_configuration].should_not == be_nil
      assigns[:package_configuration].name.should == @material.getPackageDefinition().getName()
    end
  end

  describe :action do
    before :each do
      controller.stub(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      repository1 = PackageRepositoryMother.create("repo-id", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      @pkg = PackageDefinitionMother.create("pkg-id", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "p3v2")].to_java(ConfigurationProperty)), repository1)
      repository1.setPackages(Packages.new([@pkg].to_java(PackageDefinition)))
      repos = PackageRepositories.new
      repos.add(repository1)
      @cruise_config.setPackageRepositories(repos)

      @cruise_config_mother = GoConfigMother.new

      @pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", MaterialConfigs.new([@material].to_java(MaterialConfig)), ["build-name"].to_java(java.lang.String))

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      controller.stub(:current_user).and_return(@user)
      @result = stub_localized_result

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      @go_config_service.stub(:getCurrentConfig).and_return(@cruise_config)
      @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)

      setup_other_form_objects
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @repo_id = "repo-id"
    end

    describe "create" do
      it "should add new material with new package definition" do
        controller.stub(:package_definition_service).with().and_return(StubPackageDefinitionService.new)
        stub_save_for_success
        controller.stub(:populate_config_validity)

        @pipeline.materialConfigs().size.should == 1

        post :create, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => {:create_or_associate_pkg_def => "create", :package_definition => {:repositoryId => @repo_id, :name => "pkg-name", :configuration => {"0" => configuration_for("key1", "value1"), "1" => configuration_for("key2", "value2")}}}

        @pipeline.materialConfigs().size.should == 2
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_create
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end
    end

    describe "update" do
      before :each do
        @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should update existing material with new package definition" do
        controller.stub(:package_definition_service).with().and_return(StubPackageDefinitionService.new)
        stub_save_for_success

        @pipeline.materialConfigs().size.should == 1

        put :update, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => {:create_or_associate_pkg_def => "create", :package_definition => {:repositoryId => @repo_id, :name => "pkg-name", :configuration => {"0" => configuration_for("key1", "value1"), "1" => configuration_for("key2", "value2")}}}, :finger_print => @material.getPipelineUniqueFingerprint()

        @pipeline.materialConfigs().size.should == 1
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_update
        assigns[:material].should_not == nil
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end
    end
  end

  def new_material
    PackageMaterialConfig.new
  end

  def assert_successful_create
    assert_values(@pipeline.materialConfigs().get(1))
  end

  def update_payload
    {:packageId => "pkg-id", :create_or_associate_pkg_def => "associate", :package_definition => {:repositoryId => "repo-id"}}
  end

  def assert_successful_update
    assert_values(@pipeline.materialConfigs().get(0))
  end

  def setup_other_form_objects
    setup_metadata
  end

  def setup_metadata
    metadata_store = double("PackageMetadataStore")
    PackageMetadataStore.stub(:getInstance).and_return(metadata_store)
    metadata_store.stub(:getMetadata).with("pluginid").and_return(PackageConfigurations.new)
    metadata_store.stub(:getMetadata).with("invalid-pluginid").and_return(nil)
  end

  def setup_for_new_material
    setup_metadata
  end

  private
  def configuration_for name, value
    {:configurationKey => {:name => name}, :configurationValue => {:value => value}}
  end

  def assert_values(package_material)
    package_material.getType().should == "PackageMaterial"
    package_material.getPackageId().should_not be_nil
    package_material.getPackageId().should == "pkg-id" if (controller.params[:material][:create_or_associate_pkg_def] == "associate")
    package_material.getPackageDefinition().should_not be_nil
    package_material.getPackageDefinition().getRepository().should_not be_nil
  end

end
