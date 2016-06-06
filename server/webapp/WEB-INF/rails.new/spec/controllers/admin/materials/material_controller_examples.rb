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

  describe "routes should resolve and generate" do
    it "new" do
      {:get => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/new"}.should route_to(:controller => "admin/materials/#{@short_material_type}", :action => "new", :pipeline_name => "pipeline.name")
      send("admin_#{@short_material_type}_new_path", :pipeline_name => "foo.bar").should == "/admin/pipelines/foo.bar/materials/#{@short_material_type}/new"
    end

    it "create" do
      {:post => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}"}.should route_to(:controller => "admin/materials/#{@short_material_type}", :action => "create", :pipeline_name => "pipeline.name")
      send("admin_#{@short_material_type}_create_path", :pipeline_name => "foo.bar").should == "/admin/pipelines/foo.bar/materials/#{@short_material_type}"
    end

    it "update" do
      {:put => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/finger_print"}.should route_to(:controller => "admin/materials/#{@short_material_type}", :action => "update", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      send("admin_#{@short_material_type}_update_path", :pipeline_name => "foo.bar", :finger_print => "abc").should == "/admin/pipelines/foo.bar/materials/#{@short_material_type}/abc"
    end

    it "edit" do
      {:get => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/finger_print/edit"}.should route_to(:controller => "admin/materials/#{@short_material_type}", :action => "edit", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      send("admin_#{@short_material_type}_edit_path", :pipeline_name => "foo.bar", :finger_print => "finger_print").should == "/admin/pipelines/foo.bar/materials/#{@short_material_type}/finger_print/edit"
    end

    it "delete" do
      {:delete => "/admin/pipelines/pipeline.name/materials/finger_print"}.should route_to(:controller => "admin/materials", :action => "destroy", :stage_parent => "pipelines", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      send("admin_material_delete_path", :pipeline_name => "foo.bar", :finger_print => "finger_print").should == "/admin/pipelines/foo.bar/materials/finger_print"
    end
  end

  describe "action" do
    before do
      setup_data
    end

    describe "new" do

      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should load new material" do
        setup_for_new_material

        get :new, :pipeline_name => "pipeline-name"

        assert_material_is_initialized
        assigns[:cruise_config].should == @cruise_config
        assert_template layout: false
      end
    end

    describe "create" do
      before :each do
        setup_for_new_material
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should add new material" do
        stub_save_for_success

        @pipeline.materialConfigs().size.should == 1

        post :create, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload

        @pipeline.materialConfigs().size.should == 2
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_create
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should assign config_errors for display when create fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        post :create, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload

        @cruise_config.getAllErrors().size.should == 1

        assigns[:errors].size.should == 1
        response.status.should == 400
        assert_template layout: false
      end
    end

    describe "edit" do
      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should edit an existing material" do
        setup_other_form_objects

        get :edit, :pipeline_name => "pipeline-name", :finger_print => @material.getPipelineUniqueFingerprint()

        assigns[:material].should == @material
        assigns[:cruise_config].should == @cruise_config
        controller_specific_assertion
        assert_template layout: false
      end
    end

    describe "update" do

      before :each do
        setup_other_form_objects
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should update existing material" do
        stub_save_for_success

        @pipeline.materialConfigs().size.should == 1

        put :update, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint()

        @pipeline.materialConfigs().size.should == 1
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_update
        assigns[:material].should_not == nil
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should not fail when subject is nil, because of last material being deleted concurrently by another user" do
        stub_config_save_with_subject nil

        @pipeline.materialConfigs().size.should == 1

        put :update, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint()

        @pipeline.materialConfigs().size.should == 1
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_update
        assigns[:material].should == nil
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        put :update, :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :material => update_payload, :finger_print => @material.getPipelineUniqueFingerprint()

        assigns[:errors].size.should == 1
        response.status.should == 400
        assigns[:material].should_not == nil
        assert_template layout: false
      end
    end
  end

  def assert_material_is_initialized
    assigns[:material].should == new_material
  end

  def setup_data
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
    @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)
  end

  def controller_specific_assertion
  end
end
