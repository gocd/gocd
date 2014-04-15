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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/shared/_build_cause_popup.html.erb" do
  include PipelineModelMother

  before do
    @modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
    @modification.setUserName("username")
    @modification.setComment("#42 I changed something")
    @modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added), ModifiedFile.new("nimmappa/bar.txt", "", ModifiedAction::deleted),
                                    ModifiedFile.new("nimmappa/baz.txt", "", ModifiedAction::modified), ModifiedFile.new("nimmappa/quux.txt", "", ModifiedAction::unknown)])
    @revisions = MaterialRevisions.new([].to_java(MaterialRevision))

    @svn_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", "Folder", "user", "pass", true, "*.doc"), @modification)
    @svn_revisions.getMaterialRevision(0).markAsChanged()
    @svn_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))
    @revisions.addAll(@svn_revisions)

    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @revisions.addAll(@hg_revisions)

    @dependency_revisions = ModificationsMother.changedDependencyMaterialRevision("up_pipeline", 10, "label-10", "up_stage", 5, Time.now)
    @revisions.addRevision(@dependency_revisions)
    @pim = pipeline_model("foo", "blah-label", false, false, "working with agent", false, @revisions).getLatestPipelineInstance()
  end

  it "should not display modified files if the flag is not set" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    response.body.should have_tag(".build_cause") do |material|
      material.should have_tag(" #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", "Subversion - SvnName")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision", "1234")
        change.should have_tag(".modified_by", "username#{@date.iso8601}")
        change.should have_tag(".comment", "#42 I changed something") do |comment|
          comment.should have_tag("a[href='http://pavan/42'][target='story_tracker']", "#42")
        end
        change.should_not have_tag(".modified_files")
      end
    end

    response.body.should have_tag(".build_cause") do |material|
      material.should have_tag("#material_#{@hg_revisions.materials().get(0).getPipelineUniqueFingerprint()}", "Mercurial - hg-url")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision", "9fdcf27f16eadc362733328dd481d8a2c29915e1")
        change.should have_tag(".modified_by", "user2#{ModificationsMother::TODAY_CHECKIN.iso8601}")
        change.should have_tag(".comment", "comment2")
        change.should_not have_tag(".modified_files")
      end
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision", "eef77acd79809fc14ed82b79a312648d4a2801c6")
        change.should have_tag(".modified_by", "user1#{ModificationsMother::TWO_DAYS_AGO_CHECKIN.iso8601}")
        change.should have_tag(".comment", "comment1")
        change.should_not have_tag(".modified_files")
      end
    end

    dependency_material = @dependency_revisions.getMaterial()
    response.body.should have_tag(".build_cause") do |material|
      material.should have_tag("#material_#{dependency_material.getPipelineUniqueFingerprint()}.changed", "Pipeline - #{dependency_material.getDisplayName()}")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision") do |revision|
          revision.should have_tag("a[href='/pipelines/up_pipeline/10/up_stage/5']", "up_pipeline/10/up_stage/5")
        end
        change.should have_tag(".label") do |revision|
          revision.should have_tag("a[href='/pipelines/up_pipeline/10/up_stage/5/pipeline']", "label-10")
        end
        change.should have_tag(".completed_at", "#{@dependency_revisions.getModification(0).getModifiedTime().iso8601}")
      end
    end
  end

  it "should html espace all the user entered fields" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    response.body.should have_tag(".build_cause") do |material|
      material.should have_tag(" #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", "Subversion - SvnName")
      material.should have_tag(".change") do |change|
        change.should have_tag(".comment", "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;")
        change.should have_tag(".modified_by", "&lt;script&gt;alert('Check-in user')&lt;/script&gt;#{@date.iso8601}")
      end
    end
  end

  it "should render user for display in build cause" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setUserName("")

    render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => @pim}}

    response.body.should have_tag(".build_cause") do |material|
      material.should have_tag(" #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed", "Subversion - SvnName")
      change.should have_tag(".modified_by", "anonymous#{@date.iso8601}")
    end
  end


  describe :package_materials do

    it "should render comment for package in build cause popup" do
      modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL","TRACKBACK_URL" : "http://google.com", "COMMENT" : "Some comment."}', "", @date=java.util.Date.new, "12345")
      package_material_revision = MaterialRevision.new(MaterialsMother.packageMaterial(), [modification].to_java(Modification))
      revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

      pim = pipeline_model("foo", "blah-label", false, false, "working with agent", false, revisions).getLatestPipelineInstance()

      render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => pim}}

      response.body.should have_tag(".build_cause") do |material|
        material.should have_tag(".change") do
          change.should have_tag(".comment", "Some comment.Trackback: http://google.com")
        end
      end
    end

    it "should render comment as not provided if publisher url is empty for package in build cause popup" do
      modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL"}', "", @date=java.util.Date.new, "12345")
      package_material_revision = MaterialRevision.new(MaterialsMother.packageMaterial(), [modification].to_java(Modification))
      revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

      pim = pipeline_model("foo", "blah-label", false, false, "working with agent", false, revisions).getLatestPipelineInstance()

      render :partial => "shared/build_cause_popup.html", :locals => {:scope => {:pipeline_instance => pim}}

      response.body.should have_tag(".build_cause") do |material|
        material.should have_tag(".change") do
          change.should have_tag(".comment", "Trackback: Not Provided")
        end
      end

    end
  end
end
