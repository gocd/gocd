# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))
require File.expand_path(File.join(File.dirname(__FILE__), '..', 'xpath_matchers'))

describe Buildr::IntellijIdea do

  def invoke_generate_task
    task('idea').invoke
  end

  def invoke_clean_task
    task('idea:clean').invoke
  end

  def root_project_filename(project)
    project._("#{project.name}#{Buildr::IntellijIdea::IdeaFile::DEFAULT_SUFFIX}.ipr")
  end

  def root_project_xml(project)
    xml_document(root_project_filename(project))
  end

  def root_module_filename(project)
    project._("#{project.name}#{Buildr::IntellijIdea::IdeaFile::DEFAULT_SUFFIX}.iml")
  end

  def root_module_xml(project)
    xml_document(root_module_filename(project))
  end

  def subproject_module_filename(project, sub_project_name)
    project._("#{sub_project_name}/#{sub_project_name}#{Buildr::IntellijIdea::IdeaFile::DEFAULT_SUFFIX}.iml")
  end

  def subproject_module_xml(project, sub_project_name)
    xml_document(subproject_module_filename(project, sub_project_name))
  end

  def xml_document(filename)
    File.should be_exist(filename)
    REXML::Document.new(File.read(filename))
  end

  def xpath_to_module
    "/project/component[@name='ProjectModuleManager']/modules/module"
  end

  describe "idea:clean" do
    before do
      write "foo.ipr"
      write "foo.iml"
      write "other.ipr"
      write "other.iml"
      mkdir_p 'bar'
      write "bar/bar.iml"
      write "bar/other.ipr"
      write "bar/other.iml"

      @foo = define "foo" do
        define "bar"
      end
      invoke_clean_task
    end

    it "should remove the ipr file" do
      File.exists?("foo.ipr").should be_false
    end

    it "should remove the project iml file" do
      File.exists?("foo.iml").should be_false
    end

    it "should remove the subproject iml file" do
      File.exists?("foo.iml").should be_false
    end

    it "should not remove other iml and ipr files" do
      File.exists?("other.ipr").should be_true
      File.exists?("other.iml").should be_true
      File.exists?("bar/other.ipr").should be_true
      File.exists?("bar/other.iml").should be_true
    end
  end

  describe "idea task" do

    def order_entry_xpath
      "/module/component[@name='NewModuleRootManager']/orderEntry"
    end

    describe "with a single dependency" do
      describe "of type compile" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates one exported 'module-library' orderEntry in IML" do
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library', @exported='']/library/CLASSES/root", 1)
        end
      end

      describe "with iml.main_dependencies override" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            iml.main_dependencies << 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates one exported 'module-library' orderEntry in IML" do
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library', @exported='']/library/CLASSES/root", 1)
        end
      end

      describe "of type test" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            test.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates one non-exported  test scope 'module-library' orderEntry in IML" do
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library' and @exported]/library/CLASSES/root", 0)
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library' and @scope='TEST']/library/CLASSES/root", 1)
        end
      end

      describe "with iml.test_dependencies override" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            iml.test_dependencies << 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates one non-exported 'module-library' orderEntry in IML" do
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library' and @exported]/library/CLASSES/root", 0)
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library']/library/CLASSES/root", 1)
        end
      end

      describe "with sources artifact present" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          artifact('group:id:jar:sources:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates 'module-library' orderEntry in IML with SOURCES specified" do
          root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library', @exported='']/library/SOURCES/root", 1)
        end
      end

      describe "with local_repository_env_override set to nil" do
        before do
          Buildr.repositories.instance_eval do
            @local = @remote = @release_to = nil
          end
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            iml.local_repository_env_override = nil
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates orderEntry with absolute path for classes jar" do
          root_module_xml(@foo).should match_xpath("#{order_entry_xpath}/library/CLASSES/root/@url",
                                                   "jar://$MODULE_DIR$/home/.m2/repository/group/id/1.0/id-1.0.jar!/")
        end
      end
      describe "with local_repository_env_override set to MAVEN_REPOSITORY" do
        before do
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            iml.local_repository_env_override = 'MAVEN_REPOSITORY'
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "generates orderEntry with absolute path for classes jar" do
          root_module_xml(@foo).should match_xpath("#{order_entry_xpath}/library/CLASSES/root/@url",
                                                   "jar://$MAVEN_REPOSITORY$/group/id/1.0/id-1.0.jar!/")
        end
      end
    end

    describe "with multiple dependencies" do
      before do
        artifact('group:id:jar:1.0') { |t| write t.to_s }
        artifact('group:id2:jar:1.0') { |t| write t.to_s }
        @foo = define "foo" do
          compile.with 'group:id:jar:1.0', 'group:id2:jar:1.0'
        end
        invoke_generate_task
      end

      it "generates multiple 'module-library' orderEntry in IML" do
        root_module_xml(@foo).should have_nodes("#{order_entry_xpath}[@type='module-library']", 2)
      end
    end

    describe "with a single non artifact dependency" do
      before do
        @foo = define "foo" do
          filename = _("foo-dep.jar")
          File.open(filename, "wb") { |t| write "Hello" }
          compile.with filename
        end
        invoke_generate_task
      end

      it "generates one exported 'module-library' orderEntry in IML" do
        root_module_xml(@foo).should match_xpath("#{order_entry_xpath}/library/CLASSES/root/@url",
                                                 "jar://$MODULE_DIR$/foo-dep.jar!/")
      end
    end

    describe "with extra_modules specified" do
      before do
        @foo = define "foo" do
          ipr.extra_modules << 'other.iml'
          ipr.extra_modules << 'other_other.iml'
        end
        invoke_generate_task
      end

      it "generate an IPR with extra modules specified" do
        doc = xml_document(@foo._("foo.ipr"))
        doc.should have_nodes("#{xpath_to_module}", 3)
        module_ref = "$PROJECT_DIR$/foo.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']")
        module_ref = "$PROJECT_DIR$/other.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']")
        module_ref = "$PROJECT_DIR$/other_other.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']")
      end
    end

    describe "with web and webservice facet added to root project" do
      before do
        @foo = define "foo" do
          iml.add_facet("Web", "web") do |facet|
            facet.configuration do |conf|
              conf.descriptors do |desc|
                desc.deploymentDescriptor :name => 'web.xml',
                                          :url => "file://$MODULE_DIR$/src/main/webapp/WEB-INF/web.xml",
                                          :optional => "false", :version => "2.4"
              end
              conf.webroots do |webroots|
                webroots.root :url => "file://$MODULE_DIR$/src/main/webapp", :relative => "/"
              end
            end
          end
          iml.add_facet("WebServices Client", "WebServicesClient") do |facet|
            facet.configuration "ws.engine" => "Glassfish / JAX-WS 2.X RI / Metro 1.X / JWSDP 2.0"
          end
          define 'bar'
        end
        invoke_generate_task
      end

      it "generates an IML for root project with a web and webservice facet" do
        doc = xml_document(@foo._("foo.iml"))
        facet_xpath = "/module/component[@name='FacetManager']/facet"
        doc.should have_nodes(facet_xpath, 2)
        doc.should have_xpath("#{facet_xpath}[@type='web', @name='Web']")
        doc.should have_xpath("#{facet_xpath}[@type='WebServicesClient', @name='WebServices Client']")
      end
    end

    describe "with artifacts added to root project" do
      before do
        @foo = define "foo" do
          ipr.add_artifact("MyFancy.jar", "jar") do |xml|
            xml.tag!('output-path', project._(:artifacts, "MyFancy.jar"))
            xml.element :id => "module-output", :name => "foo"
          end
          ipr.add_artifact("MyOtherFancy.jar", "jar") do |xml|
            xml.tag!('output-path', project._(:artifacts, "MyOtherFancy.jar"))
            xml.element :id => "module-output", :name => "foo"
          end
        end
        invoke_generate_task
      end

      it "generates an IPR with multiple jar artifacts" do
        doc = xml_document(@foo._("foo.ipr"))
        facet_xpath = "/project/component[@name='ArtifactManager']/artifact"
        doc.should have_nodes(facet_xpath, 2)
        doc.should have_xpath("#{facet_xpath}[@type='jar', @name='MyFancy.jar']")
        doc.should have_xpath("#{facet_xpath}[@type='jar', @name='MyOtherFancy.jar']")
      end
    end

    describe "with configurations added to root project" do
      before do
        @foo = define "foo" do
          ipr.add_configuration("Run Contacts.html", "GWT.ConfigurationType", "GWT Configuration") do |xml|
            xml.module(:name => project.iml.id)
            xml.option(:name => "RUN_PAGE", :value => "Contacts.html")
            xml.option(:name => "compilerParameters", :value => "-draftCompile -localWorkers 2")
            xml.option(:name => "compilerMaxHeapSize", :value => "512")

            xml.RunnerSettings(:RunnerId => "Run")
            xml.ConfigurationWrapper(:RunnerId => "Run")
            xml.tag! :method
          end
          ipr.add_configuration("Run Planner.html", "GWT.ConfigurationType", "GWT Configuration") do |xml|
            xml.module(:name => project.iml.id)
            xml.option(:name => "RUN_PAGE", :value => "Planner.html")
            xml.option(:name => "compilerParameters", :value => "-draftCompile -localWorkers 2")
            xml.option(:name => "compilerMaxHeapSize", :value => "512")

            xml.RunnerSettings(:RunnerId => "Run")
            xml.ConfigurationWrapper(:RunnerId => "Run")
            xml.tag! :method
          end
        end
        invoke_generate_task
      end

      it "generates an IPR with multiple configurations" do
        doc = xml_document(@foo._("foo.ipr"))
        facet_xpath = "/project/component[@name='ProjectRunConfigurationManager']/configuration"
        doc.should have_nodes(facet_xpath, 2)
        doc.should have_xpath("#{facet_xpath}[@type='GWT.ConfigurationType', @name='Run Contacts.html']")
        doc.should have_xpath("#{facet_xpath}[@type='GWT.ConfigurationType', @name='Run Planner.html']")
      end
    end

    describe "with iml.group specified" do
      before do
        @foo = define "foo" do
          iml.group = true
          define 'bar' do
            define 'baz' do

            end
          end
          define 'rab' do
            iml.group = "MyGroup"
          end
        end
        invoke_generate_task
      end

      it "generate an IPR with correct group references" do
        doc = xml_document(@foo._("foo.ipr"))
        doc.should have_nodes("#{xpath_to_module}", 4)
        module_ref = "$PROJECT_DIR$/foo.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']")
        module_ref = "$PROJECT_DIR$/rab/rab.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}' @group = 'MyGroup']")
        module_ref = "$PROJECT_DIR$/bar/bar.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}' @group = 'foo']")
        module_ref = "$PROJECT_DIR$/bar/baz/baz.iml"
        doc.should have_xpath("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}' @group = 'foo/bar']")
      end
    end

    describe "with a single project definition" do
      describe "and default naming" do
        before do
          @foo = define "foo"
          invoke_generate_task
        end

        it "generates a single IPR" do
          Dir[@foo._("**/*.ipr")].should have(1).entry
        end

        it "generate an IPR in the root directory" do
          File.should be_exist(@foo._("foo.ipr"))
        end

        it "generates a single IML" do
          Dir[@foo._("**/*.iml")].should have(1).entry
        end

        it "generates an IML in the root directory" do
          File.should be_exist(@foo._("foo.iml"))
        end

        it "generate an IPR with the reference to correct module file" do
          File.should be_exist(@foo._("foo.ipr"))
          doc = xml_document(@foo._("foo.ipr"))
          module_ref = "$PROJECT_DIR$/foo.iml"
          doc.should have_nodes("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']", 1)
        end
      end

      describe "with no_iml generation disabled" do
        before do
          @foo = define "foo" do
            project.no_iml
          end
          invoke_generate_task
        end

        it "generates no IML" do
          Dir[@foo._("**/*.iml")].should have(0).entry
        end

        it "generate an IPR with no references" do
          File.should be_exist(@foo._("foo.ipr"))
          doc = xml_document(@foo._("foo.ipr"))
          doc.should have_nodes("#{xpath_to_module}", 0)
        end
      end

      describe "with ipr generation disabled" do
        before do
          @foo = define "foo" do
            project.no_ipr
          end
          invoke_generate_task
        end

        it "generates a single IML" do
          Dir[@foo._("**/*.iml")].should have(1).entry
        end

        it "generate no IPR" do
          File.should_not be_exist(@foo._("foo.ipr"))
        end
      end

      describe "and id overrides" do
        before do
          @foo = define "foo" do
            ipr.id = 'fooble'
            iml.id = 'feap'
            define "bar" do
              iml.id = "baz"
            end
          end
          invoke_generate_task
        end

        it "generate an IPR in the root directory" do
          File.should be_exist(@foo._("fooble.ipr"))
        end

        it "generates an IML in the root directory" do
          File.should be_exist(@foo._("feap.iml"))
        end

        it "generates an IML in the subproject directory" do
          File.should be_exist(@foo._("bar/baz.iml"))
        end

        it "generate an IPR with the reference to correct module file" do
          File.should be_exist(@foo._("fooble.ipr"))
          doc = xml_document(@foo._("fooble.ipr"))
          module_ref = "$PROJECT_DIR$/feap.iml"
          doc.should have_nodes("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']", 1)
        end
      end

      describe "and a suffix defined" do
        before do
          @foo = define "foo" do
            ipr.suffix = '-ipr-suffix'
            iml.suffix = '-iml-suffix'
          end
          invoke_generate_task
        end

        it "generate an IPR in the root directory" do
          File.should be_exist(@foo._("foo-ipr-suffix.ipr"))
        end

        it "generates an IML in the root directory" do
          File.should be_exist(@foo._("foo-iml-suffix.iml"))
        end

        it "generate an IPR with the reference to correct module file" do
          File.should be_exist(@foo._("foo-ipr-suffix.ipr"))
          doc = xml_document(@foo._("foo-ipr-suffix.ipr"))
          doc.should have_nodes("#{xpath_to_module}", 1)
          module_ref = "$PROJECT_DIR$/foo-iml-suffix.iml"
          doc.should have_nodes("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']", 1)
        end
      end
    end

    describe "with a subproject" do
      before do
        @foo = define "foo" do
          define 'bar'
        end
        invoke_generate_task
      end

      it "creates the subproject directory" do
        File.should be_exist(@foo._("bar"))
      end

      it "generates an IML in the subproject directory" do
        File.should be_exist(@foo._("bar/bar.iml"))
      end

      it "generate an IPR with the reference to correct module file" do
        File.should be_exist(@foo._("foo.ipr"))
        doc = xml_document(@foo._("foo.ipr"))
        doc.should have_nodes("#{xpath_to_module}", 2)
        module_ref = "$PROJECT_DIR$/foo.iml"
        doc.should have_nodes("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']", 1)
        module_ref = "$PROJECT_DIR$/bar/bar.iml"
        doc.should have_nodes("#{xpath_to_module}[@fileurl='file://#{module_ref}', @filepath='#{module_ref}']", 1)
      end
    end

    describe "with base_dir specified" do
      before do
        @foo = define "foo" do
          define('bar', :base_dir => 'fe') do
            define('baz', :base_dir => 'fi') do
              define('foe')
            end
            define('fum')
          end
        end
        invoke_generate_task
      end

      it "generates a subproject IML in the specified directory" do
        File.should be_exist(@foo._("fe/bar.iml"))
      end

      it "generates a sub-subproject IML in the specified directory" do
        File.should be_exist(@foo._("fi/baz.iml"))
      end

      it "generates a sub-sub-subproject IML that inherits the specified directory" do
        File.should be_exist(@foo._("fi/foe/foe.iml"))
      end

      it "generates a sub-subproject IML that inherits the specified directory" do
        File.should be_exist(@foo._("fe/fum/fum.iml"))
      end

      it "generate an IPR with the references to correct module files" do
        doc = xml_document(@foo._("foo.ipr"))
        doc.should have_nodes("#{xpath_to_module}", 5)
        ["foo.iml", "fe/bar.iml", "fi/baz.iml", "fi/foe/foe.iml", "fe/fum/fum.iml"].each do |module_ref|
          doc.should have_nodes("#{xpath_to_module}[@fileurl='file://$PROJECT_DIR$/#{module_ref}', @filepath='$PROJECT_DIR$/#{module_ref}']", 1)
        end
      end
    end

    describe "with extensive intermodule dependencies" do
      before do
        mkdir_p 'foo/src/main/resources'
        mkdir_p 'foo/src/main/java/foo'
        touch 'foo/src/main/java/foo/Foo.java' # needed so that buildr will treat as a java project
        artifact('group:id:jar:1.0') { |t| write t.to_s }
        define "root" do
          repositories.remote << 'http://mirrors.ibiblio.org/pub/mirrors/maven2/'
          project.version = "2.5.2"
          define 'foo' do
            resources.from _(:source, :main, :resources)
            compile.with 'group:id:jar:1.0'
            test.using(:junit)
            package(:jar)
          end

          define 'bar' do
            # internally transitive dependencies on foo, both runtime and test
            compile.with project('root:foo'), project('root:foo').compile.dependencies
            test.using(:junit).with [project('root:foo').test.compile.target,
                                     project('root:foo').test.resources.target,
                                     project('root:foo').test.compile.dependencies].compact
            package(:jar)
          end
        end

        invoke_generate_task
        @bar_iml = xml_document(project('root:bar')._('bar.iml'))
        @bar_lib_urls = @bar_iml.get_elements("//orderEntry[@type='module-library']/library/CLASSES/root").collect do |root|
          root.attribute('url').to_s
        end
      end

      it "depends on the associated module exactly once" do
        @bar_iml.should have_nodes("//orderEntry[@type='module', @module-name='foo', @exported=""]", 1)
      end

      it "does not depend on the other project's packaged JAR" do
        @bar_lib_urls.grep(%r{#{project('root:foo').packages.first}}).should == []
      end

      it "does not depend on the the other project's target/classes directory" do
        @bar_lib_urls.grep(%r{foo/target/classes}).should == []
      end

      it "does not depend on the the other project's target/resources directory" do
        @bar_lib_urls.grep(%r{file://\$MODULE_DIR\$/../foo/target/resources}).size.should == 0
       end
    end

    describe "with a single project definition" do
      before do
        @foo = define "foo"
      end

      it "informs the user about generating IPR" do
        lambda { invoke_generate_task }.should show_info(/Writing (.+)\/foo\.ipr/)
      end

      it "informs the user about generating IML" do
        lambda { invoke_generate_task }.should show_info(/Writing (.+)\/foo\.iml/)
      end
    end
    describe "with a subproject" do
      before do
        @foo = define "foo" do
          define 'bar'
        end
      end

      it "informs the user about generating subporoject IML" do
        lambda { invoke_generate_task }.should show_info(/Writing (.+)\/bar\/bar\.iml/)
      end
    end

    describe "with compile.options.source = '1.6'" do

      before do
        @foo = define "foo" do
          compile.options.source = '1.5'
        end
        invoke_generate_task
      end

      it "generate an ProjectRootManager with 1.5 jdk specified" do
        #raise File.read(@foo._("foo.ipr"))
        xml_document(@foo._("foo.ipr")).
          should have_xpath("/project/component[@name='ProjectRootManager' and @project-jdk-name = '1.5' and @languageLevel = 'JDK_1_5']")
      end

      it "generates a ProjectDetails component with the projectName option set" do
        xml_document(@foo._("foo.ipr")).
          should have_xpath("/project/component[@name='ProjectDetails']/option[@name = 'projectName' and @value = 'foo']")
      end
    end

    describe "with compile.options.source = '1.6'" do
      before do
        @foo = define "foo" do
          compile.options.source = '1.6'
        end
        invoke_generate_task
      end

      it "generate an ProjectRootManager with 1.6 jdk specified" do
        xml_document(@foo._("foo.ipr")).
          should have_xpath("/project/component[@name='ProjectRootManager' and @project-jdk-name = '1.6' and @languageLevel = 'JDK_1_6']")
      end
    end

    describe "with iml.skip_content! specified" do
      before do
        @foo = define "foo" do
          iml.skip_content!
        end
        invoke_generate_task
      end

      it "generate an IML with no content section" do
        doc = xml_document(@foo._(root_module_filename(@foo)))
        doc.should_not have_xpath("/module/component[@name='NewModuleRootManager']/content")
      end
    end

    describe "with iml.skip_content! not specified and standard layout" do
      before do
        @foo = define "foo" do
        end
        invoke_generate_task
      end

      it "generate an IML with content section" do
        root_module_xml(@foo).should have_xpath("/module/component[@name='NewModuleRootManager']/content")
      end

      it "generate an exclude in content section for reports" do
        root_module_xml(@foo).should have_xpath("/module/component[@name='NewModuleRootManager']/content/excludeFolder[@url='file://$MODULE_DIR$/reports']")
      end

      it "generate an exclude in content section for target" do
        root_module_xml(@foo).should have_xpath("/module/component[@name='NewModuleRootManager']/content/excludeFolder[@url='file://$MODULE_DIR$/target']")
      end
    end

    describe "with subprojects" do
      before do
        @foo = define "foo" do
          define "bar" do
            compile.from _(:source, :main, :bar)
          end
        end
        invoke_generate_task
        @bar_doc = xml_document(project('foo:bar')._('bar.iml'))
      end

      it "generates the correct source directories" do
        @bar_doc.should have_xpath("//content/sourceFolder[@url='file://$MODULE_DIR$/src/main/bar']")
      end

      it "generates the correct exclude directories" do
        @bar_doc.should have_xpath("//content/excludeFolder[@url='file://$MODULE_DIR$/target']")
      end
    end

    describe "with overrides" do
      before do
        @foo = define "foo" do
          compile.from _(:source, :main, :bar)
          iml.main_source_directories << _(:source, :main, :baz)
          iml.test_source_directories << _(:source, :test, :foo)
        end
        invoke_generate_task
      end

      it "generates the correct main source directories" do
        root_module_xml(@foo).should have_xpath("//content/sourceFolder[@url='file://$MODULE_DIR$/src/main/baz' and @isTestSource='false']")
      end

      it "generates the correct test source directories" do
        root_module_xml(@foo).should have_xpath("//content/sourceFolder[@url='file://$MODULE_DIR$/src/test/foo' and @isTestSource='true']")
      end
    end

    describe "with report dir outside content" do
      before do
        layout = Layout::Default.new
        layout[:reports] = "../reports"

        @foo = define "foo", :layout => layout do
        end
        invoke_generate_task
      end

      it "generate an exclude in content section for target" do
        root_module_xml(@foo).should have_xpath("/module/component[@name='NewModuleRootManager']/content/excludeFolder[@url='file://$MODULE_DIR$/target']")
      end

      it "generates an content section without an exclude for report dir" do
        root_module_xml(@foo).should have_nodes("/module/component[@name='NewModuleRootManager']/content/excludeFolder", 1)
      end
    end

    describe "with target dir outside content" do
      before do
        layout = Layout::Default.new
        layout[:target] = "../target"
        layout[:target, :main] = "../target"

        @foo = define "foo", :layout => layout do
        end
        invoke_generate_task
      end

      it "generate an exclude in content section for reports" do
        root_module_xml(@foo).should have_xpath("/module/component[@name='NewModuleRootManager']/content/excludeFolder[@url='file://$MODULE_DIR$/reports']")
      end

      it "generates an content section without an exclude for target dir" do
        root_module_xml(@foo).should have_nodes("/module/component[@name='NewModuleRootManager']/content/excludeFolder", 1)
      end
    end

    describe "templates" do

      def ipr_template
        return <<PROJECT_XML
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="SvnBranchConfigurationManager">
    <option name="mySupportsUserInfoFilter" value="false" />
  </component>
</project>
PROJECT_XML
      end

      def ipr_existing
        return <<PROJECT_XML
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="AntConfiguration">
    <defaultAnt bundledAnt="true" />
  </component>
  <component name="SvnBranchConfigurationManager">
    <option name="mySupportsUserInfoFilter" value="true" />
  </component>
  <component name="ProjectModuleManager">
    <modules>
      <module fileurl="file://$PROJECT_DIR$/existing.iml" filepath="$PROJECT_DIR$/existing.iml" />
    </modules>
  </component>
</project>
PROJECT_XML
      end

      def ipr_from_template_xpath
        "/project/component[@name='SvnBranchConfigurationManager']/option[@name = 'mySupportsUserInfoFilter' and @value = 'false']"
      end

      def ipr_from_existing_xpath
        "/project/component[@name='AntConfiguration']"
      end

      def ipr_from_existing_shadowing_template_xpath
        "/project/component[@name='SvnBranchConfigurationManager']/option[@name = 'mySupportsUserInfoFilter' and @value = 'true']"
      end

      def ipr_from_existing_shadowing_generated_xpath
        "/project/component[@name='ProjectModuleManager']/modules/module[@fileurl = 'file://$PROJECT_DIR$/existing.iml']"
      end

      def ipr_from_generated_xpath
        "/project/component[@name='ProjectModuleManager']/modules/module[@fileurl = 'file://$PROJECT_DIR$/foo.iml']"
      end

      def iml_template
        return <<PROJECT_XML
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="FacetManager">
    <facet type="JRUBY" name="JRuby">
      <configuration number="0">
        <JRUBY_FACET_CONFIG_ID NAME="JRUBY_SDK_NAME" VALUE="JRuby SDK 1.4.0RC1" />
      </configuration>
    </facet>
  </component>
</module>
PROJECT_XML
      end

      def iml_existing
        return <<PROJECT_XML
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="FunkyPlugin"/>
  <component name="FacetManager">
    <facet type="SCALA" name="Scala"/>
  </component>
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://$MODULE_DIR$"/>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="module" module-name="buildr-bnd" exported="" />
  </component>
</module>
PROJECT_XML
      end

      def iml_from_template_xpath
        "/module/component[@name='FacetManager']/facet[@type = 'JRUBY']"
      end

      def iml_from_existing_xpath
        "/module/component[@name='FunkyPlugin']"
      end

      def iml_from_existing_shadowing_template_xpath
        "/module/component[@name='FacetManager']/facet[@type = 'SCALA']"
      end

      def iml_from_existing_shadowing_generated_xpath
        "/module/component[@name='NewModuleRootManager']/orderEntry[@module-name = 'buildr-bnd']"
      end

      def iml_from_generated_xpath
        "/module/component[@name='NewModuleRootManager']/orderEntry[@type = 'module-library']"
      end

      describe "with existing project files" do
        before do
          write "foo.ipr", ipr_existing
          write "foo.iml", iml_existing
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            ipr.template = nil
            iml.template = nil
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "replaces ProjectModuleManager component in existing ipr file" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_generated_xpath)
          xml_document(@foo._("foo.ipr")).should_not have_xpath(ipr_from_existing_shadowing_generated_xpath)
        end

        it "merges component in existing ipr file" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_existing_xpath)
        end

        def iml_from_generated_xpath
          "/module/component[@name='NewModuleRootManager']/orderEntry[@type = 'module-library']"
        end

        it "replaces NewModuleRootManager component in existing iml file" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_generated_xpath)
          xml_document(@foo._("foo.iml")).should_not have_xpath(iml_from_existing_shadowing_generated_xpath)
        end

        it "merges component in existing iml file" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_existing_xpath)
        end
      end

      describe "with an iml template" do
        before do
          write "module.template.iml", iml_template
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            ipr.template = nil
            iml.template = "module.template.iml"
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "replaces generated components" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_generated_xpath)
        end

        it "merges component in iml template" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_template_xpath)
        end
      end

      describe "with an iml template and existing iml" do
        before do
          write "module.template.iml", iml_template
          write "foo.iml", iml_existing
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            ipr.template = nil
            iml.template = "module.template.iml"
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "replaces generated components" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_generated_xpath)
        end

        it "merges component in iml template" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_template_xpath)
        end

        it "merges components not in iml template and not generated by task" do
          xml_document(@foo._("foo.iml")).should have_xpath(iml_from_existing_xpath)
          xml_document(@foo._("foo.iml")).should_not have_xpath(iml_from_existing_shadowing_template_xpath)
          xml_document(@foo._("foo.iml")).should_not have_xpath(iml_from_existing_shadowing_generated_xpath)
        end
      end

      describe "with an ipr template" do
        before do
          write "project.template.iml", ipr_template
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            ipr.template = "project.template.iml"
            iml.template = nil
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "replaces generated component in ipr template" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_generated_xpath)
        end

        it "merges component in ipr template" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_template_xpath)
        end
      end

      describe "with an ipr template and existing ipr" do
        before do
          write "project.template.iml", ipr_template
          write "foo.ipr", ipr_existing
          artifact('group:id:jar:1.0') { |t| write t.to_s }
          @foo = define "foo" do
            ipr.template = "project.template.iml"
            iml.template = nil
            compile.with 'group:id:jar:1.0'
          end
          invoke_generate_task
        end

        it "replaces generated component in ipr template" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_generated_xpath)
        end

        it "merges component in ipr template" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_template_xpath)
        end

        it "merges components not in ipr template and not generated by task" do
          xml_document(@foo._("foo.ipr")).should have_xpath(ipr_from_existing_xpath)
          xml_document(@foo._("foo.ipr")).should_not have_xpath(ipr_from_existing_shadowing_generated_xpath)
          xml_document(@foo._("foo.ipr")).should_not have_xpath(ipr_from_existing_shadowing_template_xpath)
        end
      end
    end
  end

  describe "Buildr::IntellijIdea::IdeaModule" do

    describe "with no settings" do
      before do
        @foo = define "foo"
      end

      it "has correct default iml.type setting" do
        @foo.iml.type.should == "JAVA_MODULE"
      end

      it "has correct default iml.local_repository_env_override setting" do
        @foo.iml.local_repository_env_override.should == "MAVEN_REPOSITORY"
      end
    end

    describe "settings inherited in subprojects" do
      before do
        mkdir_p 'bar'
        @foo = define "foo" do
          iml.type = "FOO_MODULE_TYPE"
          define 'bar'
        end
        invoke_generate_task
      end

      it "generates root IML with specified type" do
        module_file = root_module_filename(@foo)
        File.should be_exist(module_file)
        File.read(module_file).should =~ /FOO_MODULE_TYPE/
      end

      it "generates subproject IML with inherited type" do
        module_file = subproject_module_filename(@foo, "bar")
        File.should be_exist(module_file)
        File.read(module_file).should =~ /FOO_MODULE_TYPE/
      end

    end

    describe "with local_repository_env_override = nil" do
      if Buildr::Util.win_os?
        describe "base_directory on different drive on windows" do
          before do
            @foo = define "foo", :base_dir => "C:/bar" do
              iml.local_repository_env_override = nil
            end
          end

          it "generates relative paths correctly" do
            @foo.iml.send(:resolve_path, "E:/foo").should eql('E:/foo')
          end
        end

        describe "base_directory on same drive on windows" do
          before do
            @foo = define "foo", :base_dir => "C:/bar" do
              iml.local_repository_env_override = nil
            end
          end

          it "generates relative paths correctly" do
            @foo.iml.send(:resolve_path, "C:/foo").should eql('$MODULE_DIR$/../foo')
          end
        end
      end
    end
  end

  describe "project extension" do
    it "provides an 'idea' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "idea" }.should_not be_nil
    end

    it "documents the 'idea' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "idea" }.comment.should_not be_nil
    end

    it "provides an 'idea:clean' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "idea:clean" }.should_not be_nil
    end

    it "documents the 'idea:clean' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "idea:clean" }.comment.should_not be_nil
    end

    describe "#no_iml" do
      it "makes #iml? false" do
        @foo = define "foo" do
          project.no_iml
        end
        @foo.iml?.should be_false
      end
    end

    describe "#iml" do
      before do
        define "foo" do
          iml.suffix = "-top"

          define "bar" do
            iml.suffix = "-mid"

            define "baz" do
            end
          end
        end
      end

      it "inherits the direct parent's IML settings" do
        project('foo:bar:baz').iml.suffix.should == "-mid"
      end

      it "does not modify the parent's IML settings" do
        project('foo').iml.suffix.should == "-top"
      end

      it "works even when the parent has no IML" do
        lambda {
          define "a" do
            project.no_iml
            define "b" do
              iml.suffix = "-alone"
            end
          end
        }.should_not raise_error
      end

      it "inherits from the first ancestor which has an IML" do
        define "a" do
          iml.suffix = "-a"
          define "b" do
            iml.suffix = "-b"
            define "c" do
              project.no_iml
              define "d" do
                project.no_iml
                define "e" do
                  project.no_iml
                  define "f" do
                  end
                end
              end
            end
          end
        end

        project("a:b:c:d:e:f").iml.suffix.should == "-b"
      end
    end
  end

end
