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

describe "admin/materials/index.html.erb" do

  include GoUtil

  before do
    @new_job = JobConfig.new(CaseInsensitiveString.new("job-name"), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([@task = ExecTask.new("ls", "-la", "my_work_dir")].to_java(Task)))
    @stage = StageConfig.new(CaseInsensitiveString.new("stage-name"), JobConfigs.new([@new_job].to_java(JobConfig)))

    setup_meta_data

    svn_config = MaterialConfigsMother.svnMaterialConfig("http://some/svn/url", "svnDir", nil, nil, false, nil)
    package_config = MaterialConfigsMother.packageMaterialConfig()
    pluggable_scm_config = MaterialConfigsMother.pluggableSCMMaterialConfig('scm-id', 'scm-dest', nil)
    material_configs = MaterialConfigs.new([svn_config, package_config, pluggable_scm_config].to_java(com.thoughtworks.go.domain.materials.MaterialConfig))

    @pipeline_config = PipelineConfigMother.pipelineConfig("pipeline-name", "foo", material_configs, ["build-1"].to_java(java.lang.String))
    assign(:pipeline_config, @pipeline_config)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline_config)

    ReflectionUtil.setField(@cruise_config, "md5", "abc")
    assign(:pipeline, @pipeline_config)

    in_params(:pipeline_name => "pipeline-name", :stage_parent => "pipelines")
  end

  it "should show the headers for a materials table" do
    render

    expect(response.body).to have_selector("h3", :text => "Materials")
    Capybara.string(response.body).find('table.list_table').tap do |table|
      expect(table).to have_selector("th", :text => "Name")
      expect(table).to have_selector("th", :text => "Type")
      expect(table).to have_selector("th", :text => "URL")
      expect(table).to have_selector("th", :text => "Remove")
    end
  end

  it "should show can delete material icon with title" do
      @pipeline_config.addMaterialConfig(HgMaterialConfig.new("url", nil))

      render

      Capybara.string(response.body).all('table.list_table tr').tap do |trs|
        expect(trs[1]).to have_selector(".icon_remove[title='Remove this material']")
      end
    end

  it "should show cannot delete material icon with title when there is only one material" do
    @pipeline_config.setMaterialConfigs(MaterialConfigs.new([@pipeline_config.materialConfigs().get(0)].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)))

    render

    Capybara.string(response.body).all('table.list_table tr').tap do |trs|
      expect(trs[1]).to have_selector(".delete_icon_disabled[title='Cannot delete this material as pipeline should have at least one material']")
    end
  end

  it "should show cannot delete material icon with title when it is used in a fetch artifact task" do
    job = @pipeline_config.getFirstStageConfig.getJobs().get(0)
    job.addTask(FetchTask.new(CaseInsensitiveString.new("up"), CaseInsensitiveString.new("stage"), CaseInsensitiveString.new("job"), "srcfile", "dest"))
    @pipeline_config.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("dep"),CaseInsensitiveString.new("up"),CaseInsensitiveString.new("stage")))

    render

    Capybara.string(response.body).all('table.list_table tr').tap do |trs|
      expect(trs[4]).to have_selector(".delete_icon_disabled[title='Cannot delete this material since it is used in a fetch artifact task.']")
    end
  end

  it "should show cannot delete material icon with title when it is used in a label template" do
    material_config = HgMaterialConfig.new("url", nil)
    material_config.setName(CaseInsensitiveString.new("some_funky_name"))
    @pipeline_config.addMaterialConfig(material_config)
    @pipeline_config.setLabelTemplate("${COUNT}-${some_funky_name}-and-some-funky-name")

    materialConfigs = @pipeline_config.materialConfigs()

    render

    Capybara.string(response.body).all('table.list_table tr').tap do |trs|
      expect(trs[4].find('.delete_icon_disabled')['title']).to eq("Cannot delete this material as the material name is used in this pipeline's label template")
    end
  end

  it "should have new material warning div when scm material does not have dest set" do
    @pipeline_config.materialConfigs().clear()
    material_config = HgMaterialConfig.new("url", nil)
    @pipeline_config.addMaterialConfig(material_config)

    render

    Capybara.string(response.body).find('div.light_box_content', :visible => false).tap do |div|
      expect(div).to have_selector("div.warnings", :visible => false, :text => "In order to configure multiple materials for this pipeline, each of its material needs have to a 'Destination Directory' specified. Please edit the existing material and specify a 'Destination Directory' in order to proceed with this operation.")
      expect(div).to have_selector("button.right.close_modalbox_control", :visible => false)
    end
    Capybara.string(response.body).find('div.enhanced_dropdown ul').tap do |ul|
      ul.all("li a[href='#']").tap do |lis|
        expect(lis[0]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Subversion'})")
        expect(lis[1]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Git'})")
        expect(lis[2]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Mercurial'})")
        expect(lis[3]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Perforce'})")
        expect(lis[4]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Team Foundation Server'})")
        expect(lis[5]['onclick']).to eq("Modalbox.show(jQuery('.light_box_content')[0], {overlayClose: false, title: 'Add Material - Display Name'})")
      end
    end
  end

  it "should not have new material warning div when scm material has dest set" do
    @pipeline_config.setMaterialConfigs(MaterialConfigs.new([@pipeline_config.materialConfigs().get(0)].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)))

    render

    expect(response.body).not_to have_selector("div.light_box_content")
    expect(response.body).not_to have_selector("div.warning")
  end

  describe "material name in the listing with the link to edit" do
    it "should display material name in the listing with the link to edit for svn" do
      render

      pipeline_name = @pipeline_config.name()
      material_fingerprint = @pipeline_config.materialConfigs().get(0).getPipelineUniqueFingerprint()
      expect(Capybara.string(response.body).all("td a[href='#'][class='material_name wrapped_word']")[0]['onclick']).to eq("Util.ajax_modal('/admin/pipelines/#{pipeline_name}/materials/svn/#{material_fingerprint}/edit', {overlayClose: false, title: 'Edit Material - Subversion'}, function(text){return text})")
    end

    it "should display material name in the listing with the link to edit for package material" do
      render

      pipeline_name = @pipeline_config.name()
      material_fingerprint = @pipeline_config.materialConfigs().get(1).getPipelineUniqueFingerprint()
      material_type = "package"
      expect(Capybara.string(response.body).all("td a[href='#'][class='material_name wrapped_word']")[1]['onclick']).to eq("Util.ajax_modal('/admin/pipelines/#{pipeline_name}/materials/#{material_type}/#{material_fingerprint}/edit', {overlayClose: false, title: 'Edit Material - Package'}, function(text){return text})")
    end
  end

  describe :package_material do
    it "should list package material in the add new material dropdown" do
      render

      Capybara.string(response.body).find('div.enhanced_dropdown ul').tap do |ul|
        ul.all("li a[href='#']").tap do |lis|
          expect(lis[7]['onclick']).to eq("Modalbox.show('#{admin_package_new_path}', {overlayClose: false, title: 'Add Material - Package'})")
          expect(lis[7].text).to eq("Package")
        end
      end
    end
  end

  describe :pluggable_scm_material do
    it "should list pluggable SCM material in the add new material dropdown" do
      render

      Capybara.string(response.body).find('div.enhanced_dropdown ul').tap do |ul|
        ul.all("li a[href='#']").tap do |lis|
          expect(lis[5]['onclick']).to eq("Modalbox.show('#{admin_pluggable_scm_new_path(:pipeline_name => @pipeline_config.name(), :plugin_id => 'plugin')}', {overlayClose: false, title: 'Add Material - Display Name'})")
          expect(lis[5].text).to eq("Display Name")
        end
      end
    end

    it "should display material name in the listing with the link to edit for pluggable SCM material" do
      render

      pipeline_name = @pipeline_config.name()
      material_fingerprint = @pipeline_config.materialConfigs().get(2).getPipelineUniqueFingerprint()
      expect(Capybara.string(response.body).all("td a[href='#'][class='material_name wrapped_word']")[2]['onclick']).to eq("Util.ajax_modal('#{admin_pluggable_scm_edit_path(:pipeline_name => pipeline_name, :finger_print => material_fingerprint)}', {overlayClose: false, title: 'Edit Material - Display Name'}, function(text){return text})")
    end
  end

  def setup_meta_data
    SCMMetadataStore.getInstance().clear()

    scm_view = double('SCMView')
    scm_view.stub(:displayValue).and_return('Display Name')
    scm_view.stub(:template).and_return('Plugin Template')
    SCMMetadataStore.getInstance().addMetadataFor('plugin', SCMConfigurations.new, scm_view)
  end
end
