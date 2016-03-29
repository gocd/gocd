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

describe "/shared/_build_cause_popup.html.erb" do
  include PipelineModelMother

  HG_MATERIAL_NAME = "hg-url"
  SVN_MATERIAL_NAME = "SvnName"
  UPSTREAM_PIPELINE_NAME = "up_pipeline"
  REVISION_NUMBER = 5
  PIPELINE_NAME = "foo"

  before do
    @modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
    @modification.setUserName("username")
    @modification.setComment("#42 I changed something")
    @modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added), ModifiedFile.new("nimmappa/bar.txt", "", ModifiedAction::deleted),
                                    ModifiedFile.new("nimmappa/baz.txt", "", ModifiedAction::modified), ModifiedFile.new("nimmappa/quux.txt", "", ModifiedAction::unknown)])
    @revisions = MaterialRevisions.new([].to_java(MaterialRevision))

    @svn_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", "Folder", "user", "pass", true, "*.doc"), @modification)
    @svn_revisions.getMaterialRevision(0).markAsChanged()
    @svn_revisions.materials().get(0).setName(CaseInsensitiveString.new(SVN_MATERIAL_NAME))
    @revisions.addAll(@svn_revisions)

    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @revisions.addAll(@hg_revisions)

    @dependency_revisions = ModificationsMother.changedDependencyMaterialRevision(UPSTREAM_PIPELINE_NAME, 10, "label-10", "up_stage", REVISION_NUMBER, Time.now)
    @revisions.addRevision(@dependency_revisions)
    @pim = pipeline_model(PIPELINE_NAME, "blah-label", false, false, "working with agent", false, @revisions).getLatestPipelineInstance()

    @svn_material_id = "material_#{PIPELINE_NAME}_#{REVISION_NUMBER}_#{SVN_MATERIAL_NAME}"
    @hg_material_id = "material_#{PIPELINE_NAME}_#{REVISION_NUMBER}_#{HG_MATERIAL_NAME}"
    @dependency_material_id = "material_#{PIPELINE_NAME}_#{REVISION_NUMBER}_#{UPSTREAM_PIPELINE_NAME}"
  end

  it "should not display modified files if the flag is not set" do
    allow(view).to receive(:go_config_service).and_return(config_service = double('go_config_service'))
    config_service.stub(:getCommentRendererFor).with(PIPELINE_NAME).and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    Capybara.string(response.body).find(".build_cause").tap do |build_cause|
      build_cause.find("##{@svn_material_id}").tap do |svn_material|
        expect(svn_material).to have_selector("#material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", :text => "Subversion - #{SVN_MATERIAL_NAME}")
      end
      build_cause.find("##{@svn_material_id}_0.change.changed").tap do |first_svn_material_modification|
        expect(first_svn_material_modification).to have_selector(".revision", text: "1234 - vsm")
        vsm_url = "/materials/value_stream_map/#{@svn_revisions.materials().get(0).getFingerprint()}/1234"
        expect(first_svn_material_modification).to have_selector(".revision a[href='#{vsm_url}']", text: "vsm")
        expect(first_svn_material_modification).to have_selector(".modified_by", text: "username#{@date.iso8601}")
        #expect(first_svn_material_modification.find(".comment").text.strip).to eq("<p><a href=\"http://pavan/42\" target=\"story_tracker\">#42</a> I changed something</p>")
        expect(first_svn_material_modification).to_not have_selector(".modified_files")
      end

      build_cause.find("##{@hg_material_id}").tap do |hg_material|
        expect(hg_material).to have_selector("#material_#{@hg_revisions.materials().get(0).getPipelineUniqueFingerprint()}", text: "Mercurial - #{HG_MATERIAL_NAME}")
      end
      build_cause.find("##{@hg_material_id}_0.change").tap do |first_hg_material_modification|
        expect(first_hg_material_modification).to have_selector(".revision", text: "9fdcf27f16eadc362733328dd481d8a2c29915e1 - vsm")
        vsm_url = "/materials/value_stream_map/#{@hg_revisions.materials().get(0).getFingerprint()}/9fdcf27f16eadc362733328dd481d8a2c29915e1"
        expect(first_hg_material_modification).to have_selector(".revision a[href='#{vsm_url}']", text: "vsm")
        expect(first_hg_material_modification).to have_selector(".modified_by", text: "user2#{ModificationsMother::TODAY_CHECKIN.iso8601}")
        expect(first_hg_material_modification.find(".comment").text.strip).to eq("comment2")
        expect(first_hg_material_modification).to_not have_selector(".modified_files")
      end
      build_cause.find("##{@hg_material_id}_1.change").tap do |second_hg_material_modification|
        expect(second_hg_material_modification).to have_selector(".revision", text: "eef77acd79809fc14ed82b79a312648d4a2801c6 - vsm")
        vsm_url = "/materials/value_stream_map/#{@hg_revisions.materials().get(0).getFingerprint()}/eef77acd79809fc14ed82b79a312648d4a2801c6"
        expect(second_hg_material_modification).to have_selector(".revision a[href='#{vsm_url}']", text: "vsm")
        expect(second_hg_material_modification).to have_selector(".modified_by", text: "user1#{ModificationsMother::TWO_DAYS_AGO_CHECKIN.iso8601}")
        expect(second_hg_material_modification.find(".comment").text.strip).to eq("comment1")
        expect(second_hg_material_modification).to_not have_selector(".modified_files")
      end

      dependency_material = @dependency_revisions.getMaterial()
      build_cause.find("##{@dependency_material_id}").tap do |dependency_material_node|
        expect(dependency_material_node).to have_selector("#material_#{dependency_material.getPipelineUniqueFingerprint()}.changed", text: "Pipeline - #{dependency_material.getDisplayName()}")
      end
      build_cause.find("##{@dependency_material_id}_0.change.changed").tap do |first_dependency_material_modification|
        expect(first_dependency_material_modification).to have_selector(".revision a[href='/pipelines/up_pipeline/10/up_stage/5']", text: "up_pipeline/10/up_stage/5")
        expect(first_dependency_material_modification).to have_selector(".label a[href='/pipelines/up_pipeline/10/up_stage/5/pipeline']", text: "label-10")
        expect(first_dependency_material_modification).to have_selector(".completed_at", text: "#{@dependency_revisions.getModification(0).getModifiedTime().iso8601}")
      end
    end
  end

  it "should html escape all the user entered fields" do
    allow(view).to receive(:go_config_service).and_return(config_service = double('go_config_service'));
    config_service.stub(:getCommentRendererFor).with(PIPELINE_NAME).and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    Capybara.string(response.body).find(".build_cause").tap do |build_cause|
      build_cause.find("##{@svn_material_id}").tap do |svn_material|
        expect(svn_material).to have_selector("#material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", text: "Subversion - #{SVN_MATERIAL_NAME}")
      end
      build_cause.find("##{@svn_material_id}_0.change.changed") do |first_svn_material_modification|
        expect(first_svn_material_modification).to have_selector(".modified_by", text: "&lt;script&gt;alert('Check-in user')&lt;/script&gt;#{@date.iso8601}")
        expect(first_svn_material_modification.find(".comment").text.strip).to eq("&lt;script&gt;alert('Check-in comment')&lt;/script&gt;")
      end
    end
  end

  it "should render user for display in build cause" do
    allow(view).to receive(:go_config_service).and_return(config_service = double('go_config_service'));
    config_service.stub(:getCommentRendererFor).with(PIPELINE_NAME).and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setUserName("")

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    Capybara.string(response.body).find(".build_cause").tap do |build_cause|
      build_cause.find("##{@svn_material_id}").tap do |svn_material|
        expect(svn_material).to have_selector("#material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", text: "Subversion - #{SVN_MATERIAL_NAME}")
      end
      build_cause.find("##{@svn_material_id}_0.change.changed") do |first_svn_material_modification|
        expect(first_svn_material_modification).to have_selector(".modified_by", text: "anonymous#{@date.iso8601}")
      end
    end
  end


  describe :package_materials do
    before do
      @package_material_name = "repo-name:package-name"
      @package_material_id = "material_#{PIPELINE_NAME}_#{REVISION_NUMBER}_#{@package_material_name}"
    end

    it "should render comment for package in build cause popup" do
      modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL","TRACKBACK_URL" : "http://google.com", "COMMENT" : "Some comment."}', "", @date=java.util.Date.new, "12345")
      package_material_revision = MaterialRevision.new(MaterialsMother.packageMaterial(), [modification].to_java(Modification))
      revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

      pim = pipeline_model(PIPELINE_NAME, "blah-label", false, false, "working with agent", false, revisions).getLatestPipelineInstance()

      render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => pim}}

      Capybara.string(response.body).find(".build_cause").tap do |build_cause|
        all_rows = build_cause.all("tr")

        package_material_name_row_node = all_rows[1]
        expect(package_material_name_row_node[:id]).to eq(@package_material_id)
        expect(package_material_name_row_node).to have_selector("#material_#{revisions.materials().get(0).getPipelineUniqueFingerprint()}", text: "Package - #{@package_material_name}")

        package_material_modification_row_node = all_rows[2]
        expect(package_material_modification_row_node[:id]).to eq("#{@package_material_id}_0")
        expect(text_without_whitespace(package_material_modification_row_node.find(".comment"))).to eq('Some comment.<br>Trackback: <a href="http://google.com">http://google.com</a>')
      end
    end

    it "should render comment as not provided if publisher url is empty for package in build cause popup" do
      modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL"}', "", @date=java.util.Date.new, "12345")
      package_material_revision = MaterialRevision.new(MaterialsMother.packageMaterial(), [modification].to_java(Modification))
      revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

      pim = pipeline_model(PIPELINE_NAME, "blah-label", false, false, "working with agent", false, revisions).getLatestPipelineInstance()

      render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => pim}}

      Capybara.string(response.body).find(".build_cause").tap do |build_cause|
        all_rows = build_cause.all("tr")

        package_material_name_row_node = all_rows[1]
        expect(package_material_name_row_node[:id]).to eq(@package_material_id)
        expect(package_material_name_row_node).to have_selector("#material_#{revisions.materials().get(0).getPipelineUniqueFingerprint()}", text: "Package - #{@package_material_name}")

        package_material_modification_row_node = all_rows[2]
        expect(package_material_modification_row_node[:id]).to eq("#{@package_material_id}_0")
        expect(package_material_modification_row_node.find(".comment").text.strip).to eq("Trackback: Not Provided")
      end
    end

    def text_without_whitespace element
      element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
    end
  end
end
