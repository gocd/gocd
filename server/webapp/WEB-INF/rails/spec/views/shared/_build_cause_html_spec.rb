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

describe "/shared/_build_cause.html.erb" do
  include StageModelMother
  
  before do
    @modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
    @modification.setUserName("username")
    @modification.setComment("#42 I changed something")
    @modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added), ModifiedFile.new("nimmappa/bar.txt", "", ModifiedAction::deleted),
                                   ModifiedFile.new("nimmappa/baz.txt", "", ModifiedAction::modified), ModifiedFile.new("nimmappa/quux.txt", "", ModifiedAction::unknown)])
    @revisions = MaterialRevisions.new([].to_java(MaterialRevision))

    @svn_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", "Folder", nil, "pass", true, "*.doc"), @modification)
    @svn_revisions.getMaterialRevision(0).markAsChanged()
    @svn_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))
    @revisions.addAll(@svn_revisions)

    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @revisions.addAll(@hg_revisions)

    @dependency_revisions = ModificationsMother.changedDependencyMaterialRevision("up_pipeline", 10, "label-10", "up_stage", 5, Time.now)
    @revisions.addRevision(@dependency_revisions)
    assigns[:stage] = stage_with_three_runs()
  end

  it "should not display modified files if the flag is not set" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render :partial=>"shared/build_cause", :locals => {:scope => { :material_revisions => @revisions, :show_files => false, :pipeline_name => "foo" }}

    response.body.should have_tag(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed") do |material|
      material.should have_tag(".material_name", "Subversion - SvnName")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision") do |revision|
          revision.should have_tag("dt", "Revision:")
          revision.should have_tag("dd", "1234")
        end
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dt", "Modified by:")
          revision.should have_tag("dd", "username on #{@date.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dt", "Comment:")
          revision.should have_tag("dd", "#42 I changed something") do |comment|
            comment.should have_tag("a[href='http://pavan/42'][target='story_tracker']", "#42")
          end
        end
        change.should_not have_tag(".modified_files")
      end
    end

    response.body.should have_tag(".build_cause #material_#{@hg_revisions.materials().get(0).getPipelineUniqueFingerprint()}") do |material|
      material.should have_tag(".material_name", "Mercurial - hg-url")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision") do |revision|
          revision.should have_tag("dt", "Revision:")
          revision.should have_tag("dd", "9fdcf27f16eadc362733328dd481d8a2c29915e1")
        end
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dt", "Modified by:")
          revision.should have_tag("dd", "user2 on #{ModificationsMother::TODAY_CHECKIN.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dt", "Comment:")
          revision.should have_tag("dd", "comment2")
        end
        change.should_not have_tag(".modified_files")
      end
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision") do |revision|
          revision.should have_tag("dt", "Revision:")
          revision.should have_tag("dd", "eef77acd79809fc14ed82b79a312648d4a2801c6")
        end
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dt", "Modified by:")
          revision.should have_tag("dd", "user1 on #{ModificationsMother::TWO_DAYS_AGO_CHECKIN.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dt", "Comment:")
          revision.should have_tag("dd", "comment1")
        end
        change.should_not have_tag(".modified_files")
      end
    end

    dependency_material = @dependency_revisions.getMaterial()
    response.body.should have_tag(".build_cause #material_#{dependency_material.getPipelineUniqueFingerprint()}.changed") do |material|
      material.should have_tag(".material_name", "Pipeline - #{dependency_material.getDisplayName()}")
      material.should have_tag(".change") do |change|
        change.should have_tag(".revision") do |revision|
          revision.should have_tag("dt", "Revision:")
          revision.should have_tag("dd a[href='/pipelines/up_pipeline/10/up_stage/5']", "up_pipeline/10/up_stage/5")
        end
        change.should have_tag(".label") do |revision|
          revision.should have_tag("dt", "Label:")
          revision.should have_tag("dd a[href='/pipelines/up_pipeline/10/up_stage/5/pipeline']", "label-10")
        end
        change.should have_tag(".completed_at") do |revision|
          revision.should have_tag("dt", "Completed at:")
          revision.should have_tag("dd", "#{@dependency_revisions.getModification(0).getModifiedTime().iso8601}")
        end
      end
    end
  end

  it "should html espace all the user entered fields" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial=>"shared/build_cause", :locals => {:scope => { :material_revisions => @revisions, :show_files => false, :pipeline_name => "foo" }}

    response.body.should have_tag(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed") do |material|
      material.should have_tag(".material_name", "Subversion - SvnName")
      material.should have_tag(".change") do |change|
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dd", "&lt;script&gt;alert('Check-in user')&lt;/script&gt; on #{@date.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dd", "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;")
        end
      end
    end
  end

  it "should html espace all the user entered fields" do
    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial=>"shared/build_cause", :locals => {:scope => { :material_revisions => @revisions, :show_files => false, :pipeline_name => "foo" }}

    response.body.should have_tag(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed") do |material|
      material.should have_tag(".material_name", "Subversion - SvnName")
      material.should have_tag(".change") do |change|
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dd", "&lt;script&gt;alert('Check-in user')&lt;/script&gt; on #{@date.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dd", "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;")
        end
      end
    end
  end

  it "should render comment for package material" do
    modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL","TRACKBACK_URL" : "http://google.com", "COMMENT" : "Some comment."}', "", @date=java.util.Date.new, "12345")
    package_material = MaterialsMother.packageMaterial()
    package_material_revision = MaterialRevision.new(package_material, [modification].to_java(Modification))
    revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

    render :partial=>"shared/build_cause", :locals => {:scope => { :material_revisions => revisions, :show_files => false, :pipeline_name => "foo" }}

    response.body.should have_tag(".build_cause #material_#{package_material.getPipelineUniqueFingerprint()}") do |material|
      material.should have_tag(".material_name", "Package - repo-name:package-name")
      material.should have_tag(".change") do |change|
        change.should have_tag(".modified_by") do |revision|
          revision.should have_tag("dd", "user on #{@date.iso8601}")
        end
        change.should have_tag(".comment") do |revision|
          revision.should have_tag("dd", "Some comment.Trackback: http://google.com")
        end
      end
    end
  end

  it "should render user for display for build cause" do
    modification = Modification.new("", 'some comment', "", @date=java.util.Date.new, "12345")
    material = MaterialsMother.svnMaterial()
    material_revision = MaterialRevision.new(material, [modification].to_java(Modification))
    revisions = MaterialRevisions.new([material_revision].to_java(MaterialRevision))
    template.stub!(:render_comment).with(modification, 'foo').and_return('something')

    render :partial=>"shared/build_cause", :locals => {:scope => { :material_revisions => revisions, :show_files => false, :pipeline_name => "foo" }}

    response.body.should have_tag(".build_cause #material_#{material.getPipelineUniqueFingerprint()}") do |material|
      material.should have_tag(".material_name", "Subversion - url")
      material.should have_tag(".modified_by dd", "anonymous on #{@date.iso8601}")
    end
  end

end