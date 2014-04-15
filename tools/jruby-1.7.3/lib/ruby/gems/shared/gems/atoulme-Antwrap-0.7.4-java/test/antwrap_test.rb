# Copyright 2008 Caleb Powell 
# Licensed under the Apache License, Version 2.0 (the "License"); 
# you may not use this file except in compliance with the License. 
# You may obtain a copy of the License at 
#
#   http://www.apache.org/licenses/LICENSE-2.0 
# 
# Unless required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" BASIS, 
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
# See the License for the specific language governing permissions and limitations 
# under the License.

require 'test/unit'
require 'fileutils'
$LOAD_PATH.push(FileUtils::pwd + '/lib')
require 'antwrap'
require 'logger'

class AntwrapTest < Test::Unit::TestCase
  
  def setup
    @output_dir = FileUtils::pwd + File::SEPARATOR + 'test' + File::SEPARATOR + 'output'
    @resource_dir = FileUtils::pwd  + File::SEPARATOR + 'test' + File::SEPARATOR + 'test-resources'
    
    @ant_home = @resource_dir  + File::SEPARATOR + "apache-ant-1.7.0"
#    @ant_home = "/Users/caleb/tools/apache-ant-1.6.5"
#    @ant_home = "/Users/caleb/tools/apache-ant-1.5.4"
    @ant_proj_props = {:name=>"testProject", :basedir=>FileUtils::pwd, :declarative=>true, 
                        :logger=>Logger.new(STDOUT), :loglevel=>Logger::DEBUG, :ant_home => @ant_home}
    @ant = Antwrap::AntProject.new(@ant_proj_props)
    
    if File.exists?(@output_dir)
      FileUtils.remove_dir(@output_dir)
    end
    FileUtils.mkdir(@output_dir, :mode => 0775)
  end
  
  def test_antproject_init
    @ant_proj_props = {:name=>"testProject", :declarative=>true, 
                        :logger=>Logger.new(STDOUT), :loglevel=>Logger::ERROR}
    ant_proj = Antwrap::AntProject.new(@ant_proj_props)
    assert(@ant_proj_props[:name] == ant_proj.name())
#    assert(FileUtils::pwd == ant_proj.basedir())
    assert(@ant_proj_props[:declarative] == ant_proj.declarative())
    assert(@ant_proj_props[:logger] == ant_proj.logger())
  end
      
  def test_unzip_task
    assert_absent @output_dir + '/parent/FooBarParent.class'
    @ant.unzip(:src => @resource_dir + '/parent.jar', :dest => @output_dir)
    assert_exists @output_dir + '/parent/FooBarParent.class'
  end
  
  def test_copyanddelete_task
    file = @output_dir + '/foo.txt'
    assert_absent file
    @ant.copy(:file => @resource_dir + '/foo.txt', 
    :todir => @output_dir)
    assert_exists file
    
    @ant.delete(:file => file)
    assert_absent file
  end
  
  def test_javac_task
    FileUtils.mkdir(@output_dir + '/classes', :mode => 0775)
    
    assert_absent @output_dir + '/classes/foo/bar/FooBar.class'
    
    @ant.javac(:srcdir => @resource_dir + '/src', 
    :destdir => @output_dir + '/classes',
    :debug => 'on',
    :verbose => 'no',
    :fork => 'no',
    :failonerror => 'yes',
    :includes => 'foo/bar/**',
    :excludes => 'foo/bar/baz/**',
    :classpath => @resource_dir + '/parent.jar')
    
    assert_exists @output_dir + '/classes/foo/bar/FooBar.class'
    assert_absent @output_dir + '/classes/foo/bar/baz/FooBarBaz.class'
  end
  
  def test_javac_task_with_property
    FileUtils.mkdir(@output_dir + '/classes', :mode => 0775)
    
    assert_absent @output_dir + '/classes/foo/bar/FooBar.class'
    @ant.property(:name => 'pattern', :value => '**/*.jar') 
    @ant.property(:name => 'resource_dir', :value => @resource_dir)
    @ant.path(:id => 'common.class.path'){
      @ant.fileset(:dir => '${resource_dir}'){
        @ant.include(:name => '${pattern}')
      }
    }
    puts "Resource dir: #{@resource_dir}"
    @ant.javac(:srcdir => @resource_dir + '/src', 
    :destdir => @output_dir + '/classes',
    :debug => true,
    :verbose => true,
    :fork => 'no',
    :failonerror => 'blahblahblah',
    :includes => 'foo/bar/**',
    :excludes => 'foo/bar/baz/**',
    :classpathref => 'common.class.path')
    
    assert_exists @output_dir + '/classes/foo/bar/FooBar.class'
    assert_absent @output_dir + '/classes/foo/bar/baz/FooBarBaz.class'
  end
  
  def test_jar_task
    assert_absent @output_dir + '/Foo.jar'
    @ant.property(:name => 'outputdir', :value => @output_dir)
    @ant.property(:name => 'destfile', :value => '${outputdir}/Foo.jar') 
    @ant.jar( :destfile => "${destfile}", 
    :basedir => @resource_dir + '/src',
    :duplicate => 'preserve')
    
    assert_exists @output_dir + '/Foo.jar'
  end
  
  def test_java_task
  
    return if @ant.ant_version < 1.7
    puts "executing java task" 
    FileUtils.mkdir(@output_dir + '/classes', :mode => 0775)
    @ant.javac(:srcdir => @resource_dir + '/src',  
    :destdir => @output_dir + '/classes',
    :debug => 'on',
    :verbose => 'no',
    :fork => 'no',
    :failonerror => 'yes',
    :includes => 'foo/bar/**',
    :excludes => 'foo/bar/baz/**',
    :classpath => @resource_dir + '/parent.jar')
    
    @ant.property(:name => 'output_dir', :value => @output_dir)
    @ant.property(:name => 'resource_dir', :value =>@resource_dir)
    @ant.java(:classname => 'foo.bar.FooBar', :fork => 'false') {|ant|
      ant.arg(:value => 'argOne')
      ant.classpath(){
        ant.pathelement(:location => '${output_dir}/classes')
        ant.pathelement(:location => '${resource_dir}/parent.jar')
      }
      ant.arg(:value => 'argTwo')
      ant.jvmarg(:value => 'client')
      ant.sysproperty(:key=> 'antwrap', :value => 'coolio')
    }
  end
  
  def test_echo_task
    msg = "Antwrap is running an Echo task"                    
    @ant.echo(:message => msg, :level => 'info')
    @ant.echo(:message => 100000, :level => 'info')
    @ant.echo(:pcdata => 1000)
  end
  
  def test_mkdir_task
    dir = @output_dir + '/foo'
    
    assert_absent dir
    
    @ant.mkdir(:dir => dir)
    
    assert_exists dir
  end 
  
  def test_mkdir_task_with_property
    dir = @output_dir + '/foo'
    
    assert_absent dir
    
    @ant.property(:name => 'outputProperty', :value => dir)
    @ant.mkdir(:dir => "${outputProperty}")
    
    assert_exists dir
  end 
  
  def test_macrodef_task
    
    return if @ant.ant_version < 1.6
    
    dir = @output_dir + '/foo'
    
    assert_absent dir
    
    @ant.macrodef(:name => 'testmacrodef'){|ant|
      ant.attribute(:name => 'destination')
      ant.sequential(){
        ant.echo(:message => "Creating @{destination}")
        ant._mkdir(:dir => "@{destination}")
      }
    }      
    @ant.testmacrodef(:destination => dir)
    assert_exists dir
  end
  
  def test_cdata
    @ant.echo(:pcdata => "Foobar &amp; <><><>")
  end
  
  def test_ant_contrib

    return if @ant.ant_version() < 1.6

    @ant.taskdef(:resource => "net/sf/antcontrib/antlib.xml")

    @ant.property(:name => "bar", :value => "bar")
    @ant._if(){|ant|
      ant._equals(:arg1 => "${bar}", :arg2 => "bar")
      ant._then(){
        ant.echo(:message => "if 1 is equal")
      }
      ant._else(){
        ant.echo(:message => "if 1 is not equal")
      }
    }

    @ant.property(:name => "baz", :value => "foo")
    @ant._if(){|ant|
      ant._equals(:arg1 => "${baz}", :arg2 => "bar")
      ant._then(){
        ant.echo(:message => "if 2 is equal")
      }
      ant._else(){
        ant.echo(:message => "if 2 is not equal")
      }
    }

  end
  
  def test_tstamp
    @ant.tstamp
  end
  
  def test_array_argument
    begin
      @ant.echo(:message => ['This', 'should', 'fail', 'because', 'Arrays', 'are', 'not', 'supported'])
      add_failure "Arrays not permitted"
    rescue ArgumentError
    end
  end
  
  def test_declarative
    @ant = Antwrap::AntProject.new({:declarative=>false,:loglevel=>Logger::DEBUG, :ant_home => @ant_home})
    echo = @ant.echo(:message => "Echo")
    assert_not_nil(echo)

    @ant = Antwrap::AntProject.new({:declarative=>true,:loglevel=>Logger::DEBUG, :ant_home => @ant_home})
    echo = @ant.echo(:message => "Echo")
    assert_nil(echo)
  end
    
  private 
  def assert_exists(file_path)
    assert(File.exists?(file_path), "Does not exist[#{file_path}]")
  end
  
  def assert_absent(file_path)
    assert(!File.exists?(file_path), "Should not exist[#{file_path}]")
  end
end
