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

shared_examples_for :material_controller do

  include ConfigSaveStubbing
  include MockRegistryModule

  describe "action" do
    before do
      setup_data
    end

    describe "new" do

      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should load new material" do
        setup_for_new_material

        get :new, params: { :pipeline_name => "pipeline-name" }

        assert_material_is_initialized
        expect(assigns[:cruise_config]).to eq(@cruise_config)
        assert_template layout: false
      end
    end

    describe "create" do
      before :each do
        setup_for_new_material
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should add new material" do
        stub_save_for_success

        expect(@pipeline.materialConfigs().size).to eq(1)

        post :create, params: { :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload }

        expect(@pipeline.materialConfigs().size).to eq(2)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_successful_create
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should assign config_errors for display when create fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        post :create, params: { :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload }

        expect(@cruise_config.getAllErrors().size).to eq(1)

        expect(assigns[:errors].size).to eq(1)
        expect(response.status).to eq(400)
        assert_template layout: false
      end
    end

    describe "edit" do
      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should edit an existing material" do
        setup_other_form_objects

        get :edit, params: { :pipeline_name => "pipeline-name", :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(assigns[:material]).to eq(@material)
        expect(assigns[:cruise_config]).to eq(@cruise_config)
        controller_specific_assertion
        assert_template layout: false
      end
    end

    describe "update" do

      before :each do
        setup_other_form_objects
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        allow(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should update existing material" do
        stub_save_for_success

        expect(@pipeline.materialConfigs().size).to eq(1)

        put :update, params: { :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(@pipeline.materialConfigs().size).to eq(1)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_successful_update
        expect(assigns[:material]).not_to eq(nil)
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should not fail when subject is nil, because of last material being deleted concurrently by another user" do
        stub_config_save_with_subject nil

        expect(@pipeline.materialConfigs().size).to eq(1)

        put :update, params: { :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(@pipeline.materialConfigs().size).to eq(1)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_successful_update
        expect(assigns[:material]).to eq(nil)
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        put :update, params: { :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(assigns[:errors].size).to eq(1)
        expect(response.status).to eq(400)
        expect(assigns[:material]).not_to eq(nil)
        assert_template layout: false
      end
    end
  end

  def assert_material_is_initialized
    expect(assigns[:material]).to eq(new_material)
  end

  def setup_data
    allow(controller).to receive(:populate_config_validity)

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
    allow(controller).to receive(:current_user).and_return(@user)
    @result = stub_localized_result

    @go_config_service = stub_service(:go_config_service)
    @pipeline_pause_service = stub_service(:pipeline_pause_service)
    @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
    expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
    allow(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
  end

  def controller_specific_assertion
  end
end
