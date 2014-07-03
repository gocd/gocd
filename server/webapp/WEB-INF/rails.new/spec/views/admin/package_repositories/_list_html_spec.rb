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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")
include FormUI

describe "list.html.erb" do
  before(:each) do
    @repository1 = PackageRepositoryMother.create("id1", "name1", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    @repository1.setPackages(Packages.new([PackageDefinition.new("pid1", "pname1", nil), PackageDefinition.new("pid2", "pname2", nil)].to_java(PackageDefinition)))


    @repository2 = PackageRepositoryMother.create("id2", "name2", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    @repository2.setPackages(Packages.new([PackageDefinition.new("pid3", "pname3", nil), PackageDefinition.new("pid4", "pname4", nil)].to_java(PackageDefinition)))

    @repos = PackageRepositories.new
    @repos.add(@repository1)
    @repos.add(@repository2)

    @packageToPipelineMap = HashMap.new

    packageOnePipelines = ArrayList.new
    @pipeline = PipelineConfig.new
    @another_pipeline = PipelineConfig.new
    packageOnePipelines.add(Pair.new(@pipeline,PipelineConfigs.new))

    packageThreePipelines = ArrayList.new
    packageThreePipelines.add(Pair.new(@another_pipeline,PipelineConfigs.new))

    @packageToPipelineMap.put("pid1",packageOnePipelines)
    @packageToPipelineMap.put("pid3",packageThreePipelines)

    assign(:cruise_config, @cruise_config = mock("cruise config"))
    @cruise_config.should_receive(:canDeletePackageRepository).any_number_of_times.with(anything).and_return(true)
    @cruise_config.should_receive(:getMd5).any_number_of_times.and_return("abc")

  end

  describe "list.html" do

    it "should render repository list when available" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do

        # assertions for repo one
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id1")}']", "name1")
        end

        # assertions for repo two
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id2")}']", "name2")
        end
      end
      response.body.should_not have_tag("div.no-repo-message")
    end

    it "should select current repository" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:current_repo => 'name1', :package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do

        # assertions for repo one
        with_tag("li.selected") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id1")}']", "name1")
        end

        # assertions for repo two
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id2")}']", "name2")
        end
      end
      response.body.should_not have_tag("ul.repositories li.selected a#id2")

    end


    it "should render package list" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do
        #assertions for repo one
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id1")}']", "name1")
          with_tag("ul.packages") do
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id1', :package_id => 'pid1')}']",'pname1')
            end
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id1', :package_id => 'pid2')}']",'pname2')
            end
          end
        end

        # assertions for repo two
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id2")}']", "name2")
          with_tag("ul.packages") do
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id2', :package_id => 'pid3')}']",'pname3')
            end
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id2', :package_id => 'pid4')}']",'pname4')
            end
          end
        end
      end
    end

    it "should select current package under repository" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_id => 'pid1', :current_repo => "name1",:package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do
        #assertions for repo one
        with_tag("li.grey_selected") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id1")}']", "name1")
          with_tag("ul.packages") do
            with_tag("li.selected") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id1', :package_id => 'pid1')}']",'pname1')
            end
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id1', :package_id => 'pid2')}']",'pname2')
            end
          end
        end

        # assertions for repo two
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id2")}']", "name2")
          with_tag("ul.packages") do
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id2', :package_id => 'pid3')}']",'pname3')
            end
            with_tag("li") do
              with_tag("a[href='#{package_definitions_show_with_repository_list_path(:repo_id => 'id2', :package_id => 'pid4')}']",'pname4')
            end
          end
        end
      end
    end



    it "should render disabled remove button when package is used by pipelines" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do
        with_tag("li") do
          with_tag("a[href='#{package_repositories_edit_path(:id => "id1")}']", "name1")
          with_tag("ul.packages") do
            with_tag("li") do
              with_tag("form button[title='This package is being used in one or more pipeline(s), cannot delete the package'][disabled='disabled']")
            end
          end
        end
      end
    end

    it "should render remove button with prompt when not used by any pipeline" do

      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => @repos , :package_to_pipeline_map => @packageToPipelineMap}}

      response.body.should have_tag("ul.repositories") do
        with_tag("li") do
          with_tag("ul.packages") do
            with_tag("li") do
              with_tag("form[action='#{package_definition_delete_path(:repo_id => 'id2', :package_id => 'pid4')}'][id='delete_package_pid4'][method='post']") do
                with_tag("input[name='_method'][type='hidden'][value='delete']")
                with_tag("input[name='config_md5'][type='hidden'][value='abc']")
                with_tag("span[id='package_delete_from_tree_pid4']") do
                  with_tag("button[id='delete_button_from_tree_pid4']")
                  with_tag("div[id='warning_prompt']") do
                    with_tag("p","You are about to delete package pname4")
                  end
                end
              end
            end
          end
        end
      end
    end


    it "should not render repository list" do
      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => PackageRepositories.new}}
      response.body.should have_tag("#no-items","No repository found.")
      response.body.should_not have_tag("ul.repositories")
    end

    it "should render repository list with delete enabled only for deletable repositories" do
      repo1 = mock_repo('repo1')
      repo2 = mock_repo('repo2')
      cruise_config = mock('cruise_config')
      cruise_config.should_receive(:canDeletePackageRepository).with(repo1).and_return(true)
      cruise_config.should_receive(:canDeletePackageRepository).with(repo2).and_return(false)
      cruise_config.should_receive(:getMd5).and_return("abc")
      assign(:cruise_config, cruise_config)
      render :partial => "admin/package_repositories/list.html", :locals => {:scope => {:package_repositories => [repo1, repo2]}}

      response.body.should have_tag("form#delete_repository_repo1")
      response.body.should have_tag("form#delete_repository_repo2") do
        with_tag("button#delete_repository_button_repo2[disabled='disabled'][title='One or more packages in this repository are being used by pipeline(s). Cannot delete repository.']")
      end
    end
  end

  private
  def mock_repo(repoId, packages = Packages.new())
    repo1 = mock(repoId)
    repo1.should_receive(:getPackages).any_number_of_times.and_return(packages)
    repo1.should_receive(:getId).any_number_of_times.and_return(repoId)
    repo1.should_receive(:getName).any_number_of_times.and_return(repoId)
    repo1
  end

end