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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/materials index.html.erb" do

  include GoUtil

  before do
    @new_job = JobConfig.new(CaseInsensitiveString.new("job-name"), Resources.new, ArtifactPlans.new, Tasks.new([@task = ExecTask.new("ls", "-la", "my_work_dir")].to_java(Task)))
    @stage = StageConfig.new(CaseInsensitiveString.new("stage-name"), JobConfigs.new([@new_job].to_java(JobConfig)))

    svn_config = MaterialConfigsMother.svnMaterialConfig("http://some/svn/url", "svnDir", nil, nil, false, nil)
    package_config = MaterialConfigsMother.packageMaterialConfig()
    material_configs = MaterialConfigs.new([svn_config, package_config].to_java(com.thoughtworks.go.domain.materials.MaterialConfig))

    @pipeline_config = PipelineConfigMother.pipelineConfig("pipeline-name", "foo", material_configs, ["build-1"].to_java(java.lang.String))
    assigns[:pipeline_config] = @pipeline_config

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    @cruise_config.addPipeline("group-1", @pipeline_config)

    ReflectionUtil.setField(@cruise_config, "md5", "abc")
    assigns[:pipeline] = @pipeline_config

    in_params(:pipeline_name => "pipeline-name", :stage_parent => "pipelines")
  end

  it "should show the headers for a materials table" do
    render "admin/materials/index.html"

    response.body.should have_tag("h3", "Materials")
    response.body.should have_tag("table.list_table") do
      with_tag("th", "Name")
      with_tag("th", "Type")
      with_tag("th", "URL")
      with_tag("th", "Remove")
    end
  end

  it "should show can delete material icon with title" do
      @pipeline_config.addMaterialConfig(HgMaterialConfig.new("url", nil))

      render "admin/materials/index.html"

      response.body.should have_tag("table.list_table tr") do
        with_tag(".icon_remove[title=?]", "Remove this material")
      end
    end

  it "should show cannot delete material icon with title when there is only one material" do
    @pipeline_config.setMaterialConfigs(MaterialConfigs.new([@pipeline_config.materialConfigs().get(0)].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)))

    render "admin/materials/index.html"

    response.body.should have_tag("table.list_table tr") do
      with_tag(".delete_icon_disabled[title=?]", "Cannot delete this material as pipeline should have at least one material")
    end
  end


  it "should show cannot delete material icon with title when it is used in a fetch artifact task" do
    job = @pipeline_config.getFirstStageConfig.getJobs().get(0)
    job.addTask(FetchTask.new(CaseInsensitiveString.new("up"), CaseInsensitiveString.new("stage"), CaseInsensitiveString.new("job"), "srcfile", "dest"))
    @pipeline_config.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("dep"),CaseInsensitiveString.new("up"),CaseInsensitiveString.new("stage")))

    render "admin/materials/index.html"

    response.body.should have_tag("table.list_table tr") do
      with_tag(".delete_icon_disabled[title=?]", "Cannot delete this material since it is used in a fetch artifact task.")
    end
  end

  it "should show cannot delete material icon with title when it is used in a label template" do
    material_config = HgMaterialConfig.new("url", nil)
    material_config.setName(CaseInsensitiveString.new("some_funky_name"))
    @pipeline_config.addMaterialConfig(material_config)
    @pipeline_config.setLabelTemplate("${COUNT}-${some_funky_name}-and-some-funky-name")

    materialConfigs = @pipeline_config.materialConfigs()

    render "admin/materials/index.html"

    response.body.should have_tag("table.list_table tr") do
      with_tag(".delete_icon_disabled[title=?]", "Cannot delete this material as the material name is used in this pipeline's label template")
    end
  end


  it "should have new material warning div when scm material does not have dest set" do
    @pipeline_config.materialConfigs().clear()
    material_config = HgMaterialConfig.new("url", nil)
    @pipeline_config.addMaterialConfig(material_config)

    render "admin/materials/index.html"

    response.body.should have_tag("div.light_box_content") do
      with_tag("div.warnings", "In order to configure multiple materials for this pipeline, each of its material needs have to a 'Destination Directory' specified. Please edit the existing material and specify a 'Destination Directory' in order to proceed with this operation.")
      with_tag("button.right.close_modalbox_control")
    end
    response.body.should have_tag("div.enhanced_dropdown ul") do
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Subversion'})")
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Git'})")
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Mercurial'})")
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Perforce'})")
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Team Foundation Server'})")
    end
  end

  it "should not have new material warning div when scm material has dest set" do
    @pipeline_config.setMaterialConfigs(MaterialConfigs.new([@pipeline_config.materialConfigs().get(0)].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)))

    render "admin/materials/index.html"

    response.body.should_not have_tag("div.light_box_content")
    response.body.should_not have_tag("div.warning")
  end

  describe "material name in the listing with the link to edit" do
    it "should display material name in the listing with the link to edit for svn" do
      render "admin/materials/index.html"
      pipeline_name = @pipeline_config.name()
      material_fingerprint = @pipeline_config.materialConfigs().get(0).getPipelineUniqueFingerprint()
      response.body.should have_tag("td a[href='#'][class='material_name'][onclick=?]", "Util.ajax_modal('/admin/pipelines/#{pipeline_name}/materials/svn/#{material_fingerprint}/edit', {overlayClose: false, title: 'Edit Material - Subversion'}, function(text){return text})")
    end
    
    it "should display material name in the listing with the link to edit for package material" do
      render "admin/materials/index.html"
      pipeline_name = @pipeline_config.name()
      material_fingerprint = @pipeline_config.materialConfigs().get(1).getPipelineUniqueFingerprint()
      material_type = "package"
      response.body.should have_tag("td a[href='#'][class='material_name'][onclick=?]", "Util.ajax_modal('/admin/pipelines/#{pipeline_name}/materials/#{material_type}/#{material_fingerprint}/edit', {overlayClose: false, title: 'Edit Material - Package'}, function(text){return text})")
    end
  end

  describe :package_material do
  it "should list package material in the add new material dropdown" do
    render "admin/materials/index.html"
    response.body.should have_tag("div.enhanced_dropdown ul") do
      with_tag("li a[href='#'][onclick=?]", "Modalbox.show('#{admin_package_new_path}', {overlayClose: false, title: 'Add Material - Package'})", "Package")
    end
    end
  end


end
