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
    assign :stage, stage_with_three_runs()
  end

  it "should not display modified files if the flag is not set" do
    view.stub(:go_config_service).and_return(config_service = double('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render :partial => "shared/build_cause", :locals => {:scope => {:material_revisions => @revisions, :show_files => false, :pipeline_name => "foo"}}
    Capybara.string(response.body).find(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed").tap do |material|
      expect(material).to have_selector(".material_name", :text => "Subversion - SvnName")
      material.find(".change").tap do |change|
        change.find(".revision").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Revision:")
          expect(revision).to have_selector("dd", :text => "1234")
        end
        change.find(".modified_by").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Modified by:")
          expect(revision).to have_selector("dd", :text => "username on #{@date.iso8601}")
        end
        change.find(".comment").tap do |revision|
          expect(revision).to have_selector("dt", "Comment:")
          expect(revision).to have_selector("dd", :text => "#42 I changed something")
          revision.find("dd").tap do |comment|
            expect(comment).to have_selector("a[href='http://pavan/42'][target='story_tracker']", :text => "#42")
          end
        end
        expect(change).to_not have_selector(".modified_files")
      end

    end

    Capybara.string(response.body).find(".build_cause #material_#{@hg_revisions.materials().get(0).getPipelineUniqueFingerprint()}").tap do |material|
      expect(material).to have_selector(".material_name", :text => "Mercurial - hg-url")
      material.all(".change").tap do |changes|
        change1 = changes[0]
        change2 = changes[1]
        change1.find(".revision").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Revision:")
          expect(revision).to have_selector("dd", :text => "9fdcf27f16eadc362733328dd481d8a2c29915e1")
        end
        change1.find(".modified_by").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Modified by:")
          expect(revision).to have_selector("dd", :text => "user2 on #{ModificationsMother::TODAY_CHECKIN.iso8601}")
        end
        change1.find(".comment").tap do |revision|
          expect(revision).to have_selector("dt", "Comment:")
          expect(revision).to have_selector("dd", :text => "comment2")
        end
        expect(change1).to_not have_selector(".modified_files")

        change2.find(".revision").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Revision:")
          expect(revision).to have_selector("dd", :text => "eef77acd79809fc14ed82b79a312648d4a2801c6")
        end
        change2.find(".modified_by").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Modified by:")
          expect(revision).to have_selector("dd", :text => "user1 on #{ModificationsMother::TWO_DAYS_AGO_CHECKIN.iso8601}")
        end
        change2.find(".comment").tap do |revision|
          expect(revision).to have_selector("dt", "Comment:")
          expect(revision).to have_selector("dd", :text => "comment1")
        end
        expect(change2).to_not have_selector(".modified_files")

      end
    end

    dependency_material = @dependency_revisions.getMaterial()

    Capybara.string(response.body).find(".build_cause #material_#{dependency_material.getPipelineUniqueFingerprint()}.changed").tap do |material|
      expect(material).to have_selector(".material_name", :text => "Pipeline - #{dependency_material.getDisplayName()}")
      material.find(".change").tap do |change|
        change.find(".revision").tap do |revision|
          expect(revision).to have_selector("dt", :text => "Revision:")
          expect(revision).to have_selector("dd a[href='/pipelines/up_pipeline/10/up_stage/5']", :text => "up_pipeline/10/up_stage/5")
        end
        change.find(".label").tap do |label|
          expect(label).to have_selector("dt", :text => "Label:")
          expect(label).to have_selector("dd a[href='/pipelines/up_pipeline/10/up_stage/5/pipeline']", :text => "label-10")
        end
        change.find(".completed_at").tap do |completed_at|
          expect(completed_at).to have_selector("dt", "Completed at:")
          expect(completed_at).to have_selector("dd", :text => "#{@dependency_revisions.getModification(0).getModifiedTime().iso8601}")
        end
      end

    end
  end

  it "should html espace all the user entered fields" do
    view.stub(:go_config_service).and_return(config_service = double('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial => "shared/build_cause", :locals => {:scope => {:material_revisions => @revisions, :show_files => false, :pipeline_name => "foo"}}

    Capybara.string(response.body).find(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed").tap do |material|

      expect(material).to have_selector(".material_name", "Subversion - SvnName")
      material.find(".change").tap do |change|
        change.find(".modified_by").tap do |revision|
          expect(revision.find("dd").native.to_s).to include "&lt;script&gt;alert('Check-in user')&lt;/script&gt; on #{@date.iso8601}"
        end
        change.find(".comment").tap do |revision|
          expect(revision.find("dd").native.to_s).to include "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;"
        end
      end
    end
  end

  it "should html espace all the user entered fields" do
    view.stub(:go_config_service).and_return(config_service = double('go_config_service'));
    config_service.stub(:getCommentRendererFor).with("foo").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")
    @modification.setEmailAddress("<script>alert('Check-in email address')</script>")

    render :partial => "shared/build_cause", :locals => {:scope => {:material_revisions => @revisions, :show_files => false, :pipeline_name => "foo"}}

    Capybara.string(response.body).find(".build_cause #material_#{@svn_revisions.materials().get(0).getPipelineUniqueFingerprint()}.changed").tap do |material|
      expect(material).to have_selector(".material_name", :text => "Subversion - SvnName")
      material.find(".change").tap do |change|
        change.find(".modified_by").tap do |revision|
          expect(revision.find("dd").native.to_s).to include "&lt;script&gt;alert('Check-in user')&lt;/script&gt; on #{@date.iso8601}"
        end
        change.find(".comment").tap do |revision|
          expect(revision.find("dd").native.to_s).to include "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;"
        end
      end

    end
  end

  it "should render comment for package material" do
    modification = Modification.new("user", '{"TYPE":"PACKAGE_MATERIAL","TRACKBACK_URL" : "http://google.com", "COMMENT" : "Some comment."}', "", @date=java.util.Date.new, "12345")
    package_material = MaterialsMother.packageMaterial()
    package_material_revision = MaterialRevision.new(package_material, [modification].to_java(Modification))
    revisions = MaterialRevisions.new([package_material_revision].to_java(MaterialRevision))

    render :partial => "shared/build_cause", :locals => {:scope => {:material_revisions => revisions, :show_files => false, :pipeline_name => "foo"}}

    Capybara.string(response.body).find(".build_cause #material_#{package_material.getPipelineUniqueFingerprint()}").tap do |material|
      expect(material).to have_selector(".material_name", :text => "Package - repo-name:package-name")
      material.find(".change").tap do |change|
        change.find(".modified_by").tap do |revision|
          expect(revision).to have_selector("dd", :text => "user on #{@date.iso8601}")
        end
        change.find(".comment").tap do |revision|
          expect(revision).to have_selector("dd", :text => "Some comment.Trackback: http://google.com")
        end
      end
    end
  end

  it "should render user for display for build cause" do
    modification = Modification.new("", 'some comment', "", @date=java.util.Date.new, "12345")
    material = MaterialsMother.svnMaterial()
    material_revision = MaterialRevision.new(material, [modification].to_java(Modification))
    revisions = MaterialRevisions.new([material_revision].to_java(MaterialRevision))
    view.stub(:render_comment).with(modification, 'foo').and_return('something')

    render :partial => "shared/build_cause", :locals => {:scope => {:material_revisions => revisions, :show_files => false, :pipeline_name => "foo"}}
    Capybara.string(response.body).find(".build_cause #material_#{material.getPipelineUniqueFingerprint()}").tap do |material|
      expect(material).to have_selector(".material_name", "Subversion - url")
      expect(material).to have_selector(".modified_by dd", "anonymous on #{@date.iso8601}")
    end
  end

end
