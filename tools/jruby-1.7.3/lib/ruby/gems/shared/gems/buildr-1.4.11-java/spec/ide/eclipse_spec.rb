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


JAVA_CONTAINER   = Buildr::Eclipse::Java::CONTAINER
SCALA_CONTAINER  = Buildr::Eclipse::Scala::CONTAINER
PLUGIN_CONTAINER = Buildr::Eclipse::Plugin::CONTAINER

JAVA_NATURE   = Buildr::Eclipse::Java::NATURE
SCALA_NATURE  = Buildr::Eclipse::Scala::NATURE
PLUGIN_NATURE = Buildr::Eclipse::Plugin::NATURE

JAVA_BUILDER    = Buildr::Eclipse::Java::BUILDER
SCALA_BUILDER   = Buildr::Eclipse::Scala::BUILDER
PLUGIN_BUILDERS = Buildr::Eclipse::Plugin::BUILDERS


module EclipseHelper
  def classpath_xml_elements
    task('eclipse').invoke
    File.open('.classpath') { |f| REXML::Document.new(f).root.elements }
  end

  def classpath_sources(attribute='path')
    classpath_xml_elements.collect("classpathentry[@kind='src']") { |n| n.attributes[attribute] }
  end

  # <classpathentry path="PATH" output="RETURNED_VALUE"/>
  def classpath_specific_output(path)
    specific_output = classpath_xml_elements.collect("classpathentry[@path='#{path}']") { |n| n.attributes['output'] }
    raise "expected: one output attribute for path '#{path}, got: #{specific_output.inspect}" if specific_output.length > 1
    specific_output[0]
  end

  # <classpathentry path="RETURNED_VALUE" kind="output"/>
  def classpath_default_output
    default_output = classpath_xml_elements.collect("classpathentry[@kind='output']") { |n| n.attributes['path'] }
    raise "expected: one path attribute for kind='output', got: #{default_output.inspect}" if default_output.length > 1
    default_output[0]
  end

  # <classpathentry path="PATH" sourcepath="RETURNED_VALUE" kind="var"/>
  def sourcepath_for_path(path)
    classpath_xml_elements.collect("classpathentry[@kind='var',@path='#{path}']") do |n|
      n.attributes['sourcepath'] || 'no source artifact'
    end
  end

  # <classpathentry path="PATH" javadocpath="RETURNED_VALUE" kind="var"/>
  def javadocpath_for_path(path)
    classpath_xml_elements.collect("classpathentry[@kind='var',@path='#{path}']") do |n|
      n.attributes['javadocpath'] || 'no javadoc artifact'
    end
  end

  def project_xml_elements
    task('eclipse').invoke
    File.open('.project') { |f| REXML::Document.new(f).root.elements }
  end

  def project_natures
    project_xml_elements.collect("natures/nature") { |n| n.text }
  end

  def build_commands
    project_xml_elements.collect("buildSpec/buildCommand/name") { |n| n.text }
  end

  def classpath_containers(attribute='path')
    classpath_xml_elements.collect("classpathentry[@kind='con']") { |n| n.attributes[attribute] }
  end
end


describe Buildr::Eclipse do
  include EclipseHelper

  describe "eclipse's .project file" do

    describe 'default project' do
      before do
        write 'buildfile'
        write 'src/main/nono/Main.nono'
      end

      it 'should not have natures' do
        define('foo')
        project_natures.should be_empty
      end

      it 'should not have build commands' do
        define('foo')
        build_commands.should be_empty
      end

      it 'should generate a .project file' do
        define('foo')
        task('eclipse').invoke
        File.open('.project') do |f|
          REXML::Document.new(f).root.
            elements.collect("name") { |e| e.text }.should == ['foo']
        end
      end

      it 'should use eclipse project name if specified' do
        define('foo') { eclipse.name = 'bar' }
        task('eclipse').invoke
        File.open('.project') do |f|
          REXML::Document.new(f).root.
            elements.collect("name") { |e| e.text }.should == ['bar']
        end
      end

      it 'should not generate a .classpath file' do
        define('foo')
        task('eclipse').invoke
        File.exists?('.classpath').should be_false
      end
    end

    describe 'parent project' do
      before do
        write 'buildfile'
        mkpath 'bar'
      end

      it 'should not generate a .project for the parent project' do
        define('foo') do
          define('bar')
        end
        task('eclipse').invoke
        File.exists?('.project').should be_false
        File.exists?(File.join('bar','.project')).should be_true
      end
    end

    describe 'java project' do
      before do
        write 'buildfile'
        write 'src/main/java/Main.java'
      end

      it 'should have Java nature' do
        define('foo')
        project_natures.should include(JAVA_NATURE)
      end

      it 'should have Java build command' do
        define('foo')
        build_commands.should include(JAVA_BUILDER)
      end
    end

    describe 'nested java project' do

      it 'should have name corresponding to its project definition' do
        mkdir 'foo'
        define('myproject') {
          project.version = '1.0'
          define('foo') { compile.using(:javac); package :jar }
        }
        task('eclipse').invoke
        File.open(File.join('foo', '.project')) do |f|
          REXML::Document.new(f).root.
            elements.collect("name") { |e| e.text }.should == ['myproject-foo']
        end
      end

      it 'should use eclipse name for child project if set' do
        mkdir 'foo'
        define('myproject') {
          project.version = '1.0'
          define('foo') { eclipse.name = 'bar'; compile.using(:javac); package :jar }
        }
        task('eclipse').invoke
        File.open(File.join('foo', '.project')) do |f|
          REXML::Document.new(f).root.
            elements.collect("name") { |e| e.text }.should == ['bar']
        end
      end

      it 'should use short name for child project if eclipse.options.short_names = true' do
        mkdir 'foo'
        define('myproject') {
          project.version = '1.0'
          eclipse.options.short_names = true
          define('foo') { compile.using(:javac); package :jar }
        }
        task('eclipse').invoke
        File.open(File.join('foo', '.project')) do |f|
          REXML::Document.new(f).root.
            elements.collect("name") { |e| e.text }.should == ['foo']
        end
      end
    end

    describe 'scala project' do

      before do
        define 'foo' do
          eclipse.natures :scala
        end
      end

      it 'should have Scala nature before Java nature' do
        project_natures.should include(SCALA_NATURE)
        project_natures.should include(JAVA_NATURE)
        project_natures.index(SCALA_NATURE).should < project_natures.index(JAVA_NATURE)
      end

      it 'should have Scala build command and no Java build command' do
        build_commands.should include(SCALA_BUILDER)
        build_commands.should_not include(JAVA_BUILDER)
      end
    end

    describe 'standard scala project' do

      before do
        write 'buildfile'
        write 'src/main/scala/Main.scala'
        define 'foo'
      end

      it 'should have Scala nature before Java nature' do
        project_natures.should include(SCALA_NATURE)
        project_natures.should include(JAVA_NATURE)
        project_natures.index(SCALA_NATURE).should < project_natures.index(JAVA_NATURE)
      end

      it 'should have Scala build command and no Java build command' do
        build_commands.should include(SCALA_BUILDER)
        build_commands.should_not include(JAVA_BUILDER)
      end
    end

    describe 'non-standard scala project' do

      before do
        write 'buildfile'
        write 'src/main/foo/Main.scala'
        define 'foo' do
          eclipse.natures = :scala
        end
      end

      it 'should have Scala nature before Java nature' do
        project_natures.should include(SCALA_NATURE)
        project_natures.should include(JAVA_NATURE)
        project_natures.index(SCALA_NATURE).should < project_natures.index(JAVA_NATURE)
      end

      it 'should have Scala build command and no Java build command' do
        build_commands.should include(SCALA_BUILDER)
        build_commands.should_not include(JAVA_BUILDER)
      end
    end

    describe 'Plugin project' do

      before do
        write 'buildfile'
        write 'src/main/java/Activator.java'
        write 'plugin.xml'
      end

      it 'should have plugin nature before Java nature' do
        define('foo')
        project_natures.should include(PLUGIN_NATURE)
        project_natures.should include(JAVA_NATURE)
        project_natures.index(PLUGIN_NATURE).should < project_natures.index(JAVA_NATURE)
      end

      it 'should have plugin build commands and the Java build command' do
        define('foo')
        build_commands.should include(PLUGIN_BUILDERS[0])
        build_commands.should include(PLUGIN_BUILDERS[1])
        build_commands.should include(JAVA_BUILDER)
      end
    end

    describe 'Plugin project' do

      before do
        write 'buildfile'
        write 'src/main/java/Activator.java'
        write 'plugin.xml'
      end

      it 'should have plugin nature before Java nature' do
        define('foo')
        project_natures.should include(PLUGIN_NATURE)
        project_natures.should include(JAVA_NATURE)
        project_natures.index(PLUGIN_NATURE).should < project_natures.index(JAVA_NATURE)
      end

      it 'should have plugin build commands and the Java build command' do
        define('foo')
        build_commands.should include(PLUGIN_BUILDERS[0])
        build_commands.should include(PLUGIN_BUILDERS[1])
        build_commands.should include(JAVA_BUILDER)
      end
    end

    describe 'Plugin project with META-INF/MANIFEST.MF' do

      before do
        write 'buildfile'
        write 'src/main/java/Activator.java'
      end

      it 'should have plugin nature by default if MANIFEST.MF contains "Bundle-SymbolicName:"' do
        write 'META-INF/MANIFEST.MF', <<-MANIFEST
Manifest-Version: 1.0
Name: example/
Specification-Title: "Examples"
Specification-Version: "1.0"
Specification-Vendor: "Acme Corp.".
Implementation-Title: "example"
Implementation-Version: "build57"
Implementation-Vendor: "Acme Corp."
Bundle-SymbolicName: acme.plugin.example
MANIFEST
        define('foo')
        project_natures.should include(PLUGIN_NATURE)
      end

      it 'should not have plugin nature if MANIFEST.MF exists but doesn\'t contain "Bundle-SymbolicName:"' do
        write 'META-INF/MANIFEST.MF', <<-MANIFEST
Manifest-Version: 1.0
Name: example/
Specification-Title: "Examples"
Specification-Version: "1.0"
Specification-Vendor: "Acme Corp.".
Implementation-Title: "example"
Implementation-Version: "build57"
Implementation-Vendor: "Acme Corp."
MANIFEST
        define('foo')
        project_natures.should_not include(PLUGIN_NATURE)
      end
    end
  end

  describe "eclipse's .classpath file" do

    describe 'scala project' do

      before do
        write 'buildfile'
        write 'src/main/scala/Main.scala'
      end

      it 'should have SCALA_CONTAINER before JAVA_CONTAINER' do
        define('foo')
        classpath_containers.should include(SCALA_CONTAINER)
        classpath_containers.should include(JAVA_CONTAINER)
        classpath_containers.index(SCALA_CONTAINER).should < classpath_containers.index(JAVA_CONTAINER)
      end
    end

    describe 'source folders' do

      before do
        write 'buildfile'
        write 'src/main/java/Main.java'
        write 'src/test/java/Test.java'
      end

      shared_examples_for 'source' do
        it 'should ignore CVS and SVN files' do
          define('foo')
          classpath_sources('excluding').each do |excluding_attribute|
            excluding = excluding_attribute.split('|')
            excluding.should include('**/.svn/')
            excluding.should include('**/CVS/')
          end
        end
      end

      describe 'main code' do
        it_should_behave_like 'source'

        it 'should accept to come from the default directory' do
          define('foo')
          classpath_sources.should include('src/main/java')
        end

        it 'should accept to come from a user-defined directory' do
          define('foo') { compile path_to('src/java') }
          classpath_sources.should include('src/java')
        end

        it 'should accept a file task as a main source folder' do
          define('foo') { compile apt }
          classpath_sources.should include('target/generated/apt')
        end

        it 'should go to the default target directory' do
          define('foo')
          classpath_specific_output('src/main/java').should be(nil)
          classpath_default_output.should == 'target/classes'
        end
      end

      describe 'test code' do
        it_should_behave_like 'source'

        it 'should accept to come from the default directory' do
          define('foo')
          classpath_sources.should include('src/test/java')
        end

        it 'should accept to come from a user-defined directory' do
          define('foo') { test.compile path_to('src/test') }
          classpath_sources.should include('src/test')
        end

        it 'should go to the default target directory' do
          define('foo')
          classpath_specific_output('src/test/java').should == 'target/test/classes'
        end

        it 'should accept to be the only code in the project' do
          rm 'src/main/java/Main.java'
          define('foo')
          classpath_sources.should include('src/test/java')
        end
      end

      describe 'main resources' do
        it_should_behave_like 'source'

        before do
          write 'src/main/resources/config.xml'
        end

        it 'should accept to come from the default directory' do
          define('foo')
          classpath_sources.should include('src/main/resources')
        end

        it 'should share a classpath entry if it comes from a directory with code' do
          write 'src/main/java/config.properties'
          define('foo') { resources.from('src/main/java').exclude('**/*.java') }
          classpath_sources.select { |path| path == 'src/main/java'}.length.should == 1
        end

        it 'should go to the default target directory' do
          define('foo')
          classpath_specific_output('src/main/resources').should == 'target/resources'
        end
      end

      describe 'test resources' do
        it_should_behave_like 'source'

        before do
          write 'src/test/resources/config-test.xml'
        end

        it 'should accept to come from the default directory' do
          define('foo')
          classpath_sources.should include('src/test/resources')
        end

        it 'should share a classpath entry if it comes from a directory with code' do
          write 'src/test/java/config-test.properties'
          define('foo') { test.resources.from('src/test/java').exclude('**/*.java') }
          classpath_sources.select { |path| path == 'src/test/java'}.length.should == 1
        end

        it 'should go to the default target directory' do
          define('foo')
          classpath_specific_output('src/test/resources').should == 'target/test/resources'
        end
      end
    end

    describe 'project depending on another project' do
      it 'should have the underlying project in its classpath' do
        mkdir 'foo'
        mkdir 'bar'
        define('myproject') {
          project.version = '1.0'
          define('foo') { package :jar }
          define('bar') { compile.using(:javac).with project('foo'); }
        }
        task('eclipse').invoke
        File.open(File.join('bar', '.classpath')) do |f|
          REXML::Document.new(f).root.
            elements.collect("classpathentry[@kind='src']") { |n| n.attributes['path'] }.should include('/myproject-foo')
        end
      end

      it 'should use eclipse name in its classpath if set' do
        mkdir 'foo'
        mkdir 'bar'
        define('myproject') {
          project.version = '1.0'
          define('foo') { eclipse.name = 'eclipsefoo'; package :jar }
          define('bar') { eclipse.name = 'eclipsebar'; compile.using(:javac).with project('foo'); }
        }
        task('eclipse').invoke
        File.open(File.join('bar', '.classpath')) do |f|
          REXML::Document.new(f).root.
            elements.collect("classpathentry[@kind='src']") { |n| n.attributes['path'] }.should include('/eclipsefoo')
        end
      end
    end
  end

  describe 'local dependency' do
    before do
      write 'lib/some-local.jar'
      define('foo') { compile.using(:javac).with(_('lib/some-local.jar')) }
    end

    it 'should have a lib artifact reference in the .classpath file' do
      classpath_xml_elements.collect("classpathentry[@kind='lib']") { |n| n.attributes['path'] }.
        should include('lib/some-local.jar')
    end
  end

  describe 'project .classpath' do
    before do
      mkdir_p '../libs'
      write '../libs/some-local.jar'
      define('foo') do
        eclipse.classpath_variables :LIBS => '../libs', :LIBS2 => '../libs2'
        compile.using(:javac).with(_('../libs/some-local.jar'))
      end
    end

    it 'supports generating library paths with classpath variables' do
      classpath_xml_elements.collect("classpathentry[@kind='var']") { |n| n.attributes['path'] }.
        should include('LIBS/some-local.jar')
    end
  end

  describe 'generated .classes' do
    before do
      write 'lib/some.class'
      define('foo') { compile.using(:javac).with(_('lib')) }
    end

    it 'should have src reference in the .classpath file' do
      classpath_xml_elements.collect("classpathentry[@kind='src']") { |n| n.attributes['path'] }.
        should include('lib')
    end
  end

  describe 'maven2 artifact dependency' do
    before do
      define('foo') { compile.using(:javac).with('com.example:library:jar:2.0') }
      artifact('com.example:library:jar:2.0') { |task| write task.name }
      task('eclipse').invoke
    end

    it 'should have a reference in the .classpath file relative to the local M2 repo' do
      classpath_xml_elements.collect("classpathentry[@kind='var']") { |n| n.attributes['path'] }.
        should include('M2_REPO/com/example/library/2.0/library-2.0.jar')
    end

    it 'should be downloaded' do
      file(artifact('com.example:library:jar:2.0').name).should exist
    end

    it 'should have a source artifact reference in the .classpath file' do
      sourcepath_for_path('M2_REPO/com/example/library/2.0/library-2.0.jar').
        should == ['M2_REPO/com/example/library/2.0/library-2.0-sources.jar']
    end

    it 'should have a javadoc artifact reference in the .classpath file' do
      javadocpath_for_path('M2_REPO/com/example/library/2.0/library-2.0.jar').
        should == ['M2_REPO/com/example/library/2.0/library-2.0-javadoc.jar']
    end
  end

  describe 'maven2 repository variable' do
    it 'should be configurable' do
      define('foo') do
        eclipse.options.m2_repo_var = 'PROJ_REPO'
        compile.using(:javac).with('com.example:library:jar:2.0')
      end
      artifact('com.example:library:jar:2.0') { |task| write task.name }

      task('eclipse').invoke
      classpath_xml_elements.collect("classpathentry[@kind='var']") { |n| n.attributes['path'] }.
        should include('PROJ_REPO/com/example/library/2.0/library-2.0.jar')
    end

    it 'should pick the parent value by default' do
      define('foo') do
        eclipse.options.m2_repo_var = 'FOO_REPO'
        define('bar')

        define('bar2') do
          eclipse.options.m2_repo_var = 'BAR2_REPO'
        end
      end
      project('foo:bar').eclipse.options.m2_repo_var.should eql('FOO_REPO')
      project('foo:bar2').eclipse.options.m2_repo_var.should eql('BAR2_REPO')
    end
  end

  describe 'natures variable' do
    it 'should be configurable' do
      define('foo') do
        eclipse.natures = 'dummyNature'
        compile.using(:javac).with('com.example:library:jar:2.0')
      end
      artifact('com.example:library:jar:2.0') { |task| write task.name }
      project_natures.should include('dummyNature')
    end

    it 'should pick the parent value by default' do
      define('foo') do
        eclipse.natures = 'foo_nature'
        define('bar')

        define('bar2') do
          eclipse.natures = 'bar2_nature'
        end
      end
      project('foo:bar').eclipse.natures.should include('foo_nature')
      project('foo:bar2').eclipse.natures.should include('bar2_nature')
    end

    it 'should handle arrays correctly' do
      define('foo') do
        eclipse.natures ['foo_nature', 'bar_nature']
      end
      project('foo').eclipse.natures.should == ['foo_nature', 'bar_nature']
    end
  end

  describe 'builders variable' do
    it 'should be configurable' do
      define('foo') do
        eclipse.builders 'dummyBuilder'
        compile.using(:javac).with('com.example:library:jar:2.0')
      end
      artifact('com.example:library:jar:2.0') { |task| write task.name }
      build_commands.should include('dummyBuilder')
    end

    it 'should pick the parent value by default' do
      define('foo') do
        eclipse.builders = 'foo_builder'
        define('bar')

        define('bar2') do
          eclipse.builders = 'bar2_builder'
        end
      end
      project('foo:bar').eclipse.builders.should include('foo_builder')
      project('foo:bar2').eclipse.builders.should include('bar2_builder')
    end

    it 'should handle arrays correctly' do
      define('foo') do
        eclipse.builders ['foo_builder', 'bar_builder']
      end
      project('foo').eclipse.builders.should == ['foo_builder', 'bar_builder']
    end
  end

  describe 'classpath_containers variable' do
    it 'should be configurable' do
      define('foo') do
        eclipse.classpath_containers = 'myOlGoodContainer'
        compile.using(:javac).with('com.example:library:jar:2.0')
      end
      artifact('com.example:library:jar:2.0') { |task| write task.name }
      classpath_containers.should include('myOlGoodContainer')
    end

    it 'should pick the parent value by default' do
      define('foo') do
        eclipse.classpath_containers = 'foo_classpath_containers'
        define('bar')

        define('bar2') do
          eclipse.classpath_containers = 'bar2_classpath_containers'
        end
      end
      project('foo:bar').eclipse.classpath_containers.should include('foo_classpath_containers')
      project('foo:bar2').eclipse.classpath_containers.should include('bar2_classpath_containers')
    end

    it 'should handle arrays correctly' do
      define('foo') do
        eclipse.classpath_containers ['foo_cc', 'bar_cc']
      end
      project('foo').eclipse.classpath_containers.should == ['foo_cc', 'bar_cc']
    end
  end

  describe 'exclude_libs' do
    it 'should support artifacts' do
      define('foo') do
        compile.using(:javac).with('com.example:library:jar:2.0')
        eclipse.exclude_libs += [ artifact('com.example:library:jar:2.0') ]
      end
      artifact('com.example:library:jar:2.0') { |task| write task.name }

      task('eclipse').invoke
      classpath_xml_elements.collect("classpathentry[@kind='var']") { |n| n.attributes['path'] }.
        should_not include('M2_REPO/com/example/library/2.0/library-2.0.jar')
    end
    it 'should support string paths' do
      define('foo') do
        compile.using(:javac).with _('path/to/library.jar')
        eclipse.exclude_libs += [ _('path/to/library.jar') ]
      end
      write project('foo').path_to('path/to/library.jar')

      task('eclipse').invoke
      classpath_xml_elements.collect("classpathentry[@kind='lib']") { |n| n.attributes['path'] }.
        should_not include('path/to/library.jar')
    end
  end
end
