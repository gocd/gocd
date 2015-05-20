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

describe Buildr::ArtifactNamespace do

  before(:each) { Buildr::ArtifactNamespace.clear }

  def abc_module
    Object.module_eval 'module A; module B; module C; end; end; end'
    yield
  ensure
    Object.send :remove_const, :A
  end

  describe '.root' do
    it 'should return the top level namespace' do
      Buildr::ArtifactNamespace.root.should be_root
    end

    it 'should yield the namespace if a block is given' do
      flag = false
      Buildr::ArtifactNamespace.root { |ns| flag = true; ns.should be_root }
      flag.should == true
    end

    it 'should return the root when used outside of a project definition' do
      artifact_ns.should be_root
    end

    it 'should yield to a block when used outside of a project definition' do
      flag = false
      artifact_ns {|ns| flag = true; ns.should be_root}
      flag.should == true
    end
  end

  describe '.instance' do
    it 'should return the top level namespace when invoked outside a project definition' do
      artifact_ns.should be_root
    end

    it 'should return the namespace for the receiving project' do
      define('foo') { }
      project('foo').artifact_ns.name.should == 'foo'
    end

    it 'should return the current project namespace when invoked inside a project' do
      define 'foo' do
        artifact_ns.should_not be_root
        artifact_ns.name.should == 'foo'
        task :doit do
          artifact_ns.should_not be_root
          artifact_ns.name.should == 'foo'
        end.invoke
      end
    end

    it 'should return the root namespace if given :root' do
      artifact_ns(:root).should be_root
    end

    it 'should return the namespace for the given name' do
      artifact_ns(:foo).name.should == 'foo'
      artifact_ns('foo:bar').name.should == 'foo:bar'
      artifact_ns(['foo', 'bar', 'baz']).name.should == 'foo:bar:baz'
      abc_module do
        artifact_ns(A::B::C).name.should == 'A::B::C'
      end
      artifact_ns(:root).should be_root
      artifact_ns(:current).should be_root
      define 'foo' do
        artifact_ns(:current).name.should == 'foo'
        define 'baz' do
          artifact_ns(:current).name.should == 'foo:baz'
        end
      end
    end
  end

  describe '#parent' do
    it 'should be nil for root namespace' do
      artifact_ns(:root).parent.should be_nil
    end

    it 'should be the parent namespace for nested modules' do
      abc_module do
        artifact_ns(A::B::C).parent.should == artifact_ns(A::B)
        artifact_ns(A::B).parent.should == artifact_ns(A)
        artifact_ns(A).parent.should == artifact_ns(:root)
      end
    end

    it 'should be the parent namespace for nested projects' do
      define 'a' do
        define 'b' do
          define 'c' do
            artifact_ns.parent.should == artifact_ns(parent)
          end
          artifact_ns.parent.should == artifact_ns(parent)
        end
        artifact_ns.parent.should == artifact_ns(:root)
      end
    end
  end

  describe '#parent=' do
    it 'should reject to set parent for root namespace' do
      lambda { artifact_ns(:root).parent = :foo }.should raise_error(Exception, /cannot set parent/i)
    end

    it 'should allow to set parent' do
      artifact_ns(:bar).parent = :foo
      artifact_ns(:bar).parent.should == artifact_ns(:foo)
      artifact_ns(:bar).parent = artifact_ns(:baz)
      artifact_ns(:bar).parent.should == artifact_ns(:baz)
    end

    it 'should allow to set parent to :current' do
      abc_module do
        mod = A::B
        artifact_ns(mod).parent = :current
        def mod.stuff
          Buildr::artifact_ns(self)
        end
        define 'a' do
          define 'b' do
            mod.stuff.parent.should == artifact_ns
          end
          mod.stuff.parent.should == artifact_ns
        end
      end
    end
  end

  describe '#need' do
    it 'should accept an artifact spec' do
      define 'one' do
        artifact_ns.need 'a:b:c:1'
        # referenced by spec
        artifact_ns['a:b:c'].should_not be_selected

        # referenced by name
        artifact_ns[:b].should_not be_selected
        artifact_ns[:b].should be_satisfied_by('a:b:c:1')
        artifact_ns[:b].should_not be_satisfied_by('a:b:c:2')
        artifact_ns[:b].should_not be_satisfied_by('d:b:c:1')
        artifact_ns[:b].version.should == '1'
      end
    end

    it 'should accept an artifact spec with classifier' do
      define 'one' do
        artifact_ns.need 'a:b:c:d:1'
        # referenced by spec
        artifact_ns['a:b:c:d:'].should_not be_selected

        # referenced by name
        artifact_ns[:b].should_not be_selected
        artifact_ns[:b].should be_satisfied_by('a:b:c:d:1')
        artifact_ns[:b].should_not be_satisfied_by('a:b:c:d:2')
        artifact_ns[:b].should_not be_satisfied_by('d:b:c:d:1')
        artifact_ns[:b].version.should == '1'
      end
    end

    it 'should accept a requirement_spec' do
      define 'one' do
        artifact_ns.need 'thing -> a:b:c:2.1 -> ~>2.0'
        # referenced by spec
        artifact_ns['a:b:c'].should_not be_selected

        # referenced by name
        artifact_ns.key?(:b).should be_false
        artifact_ns[:thing].should_not be_selected
        artifact_ns[:thing].should be_satisfied_by('a:b:c:2.5')
        artifact_ns[:thing].should_not be_satisfied_by('a:b:c:3')
        artifact_ns[:thing].version.should == '2.1'
      end
    end

    it 'should accept a hash :name -> requirement_spec' do
      define 'one' do
        artifact_ns.need :thing => 'a:b:c:2.1 -> ~>2.0'
        artifact_ns[:thing].should be_satisfied_by('a:b:c:2.5')
        artifact_ns[:thing].should_not be_satisfied_by('a:b:c:3')
        artifact_ns[:thing].version.should == '2.1'
      end

      define 'two' do
        artifact_ns.need :thing => 'a:b:c:(~>2.0 | 2.1)'
        artifact_ns[:thing].should be_satisfied_by('a:b:c:2.5')
        artifact_ns[:thing].should_not be_satisfied_by('a:b:c:3')
        artifact_ns[:thing].version.should == '2.1'
      end
    end

    it 'should take a hash :name -> specs_array' do
      define 'one' do
        artifact_ns.need :things => ['foo:bar:jar:1.0',
                                     'foo:baz:jar:2.0',]
        artifact_ns['foo:bar:jar'].should_not be_selected
        artifact_ns['foo:baz:jar'].should_not be_selected
        artifact_ns[:bar, :baz].should == [nil, nil]
        artifact_ns[:things].map(&:unversioned_spec).should include('foo:bar:jar', 'foo:baz:jar')
        artifact_ns.alias :baz, 'foo:baz:jar'
        artifact_ns[:baz].should == artifact_ns['foo:baz:jar']
      end
    end

    it 'should select best matching version if defined' do
      define 'one' do
        artifact_ns.use :a => 'foo:bar:jar:1.5'
        artifact_ns.use :b => 'foo:baz:jar:2.0'
        define 'two' do
          artifact_ns[:a].requirement.should be_nil
          artifact_ns[:a].should be_selected

          artifact_ns.need :c => 'foo:bat:jar:3.0'
          artifact_ns['foo:bat:jar'].should_not be_selected
          artifact_ns[:c].should_not be_selected

          artifact_ns.need :one => 'foo:bar:jar:>=1.0'
          artifact_ns[:one].version.should == '1.5'
          artifact_ns[:one].should be_selected
          artifact_ns[:a].requirement.should be_nil

          artifact_ns.need :two => 'foo:baz:jar:>2'
          artifact_ns[:two].version.should be_nil
          artifact_ns[:two].should_not be_selected
          artifact_ns[:b].requirement.should be_nil
        end
      end
    end
  end

  describe '#use' do
    it 'should register the artifact on namespace' do
      define 'one' do
        artifact_ns.use :thing => 'a:b:c:1'
        artifact_ns[:thing].requirement.should be_nil
        artifact_ns[:thing].version.should == '1'
        artifact_ns[:thing].id.should == 'b'
        define 'one' do
          artifact_ns.use :thing => 'a:d:c:2'
          artifact_ns[:thing].requirement.should be_nil
          artifact_ns[:thing].version.should == '2'
          artifact_ns[:thing].id.should == 'd'

          artifact_ns.use :copied => artifact_ns.parent[:thing]
          artifact_ns[:copied].should_not == artifact_ns.parent[:thing]
          artifact_ns[:copied].requirement.should be_nil
          artifact_ns[:copied].version.should == '1'
          artifact_ns[:copied].id.should == 'b'

          artifact_ns.use :aliased => :copied
          artifact_ns[:aliased].should == artifact_ns[:copied]

          lambda { artifact_ns.use :invalid => :unknown }.should raise_error(NameError, /undefined/i)
        end
        artifact_ns[:copied].should be_nil
      end
    end

    it 'should register two artifacts with different version on namespace' do
      define 'one' do
        artifact_ns.use :foo => 'a:b:c:1'
        artifact_ns.use :bar => 'a:b:c:2'
        artifact_ns[:foo].version.should == '1'
        artifact_ns[:bar].version.should == '2'
        # unversioned references the last version set.
        artifact_ns['a:b:c'].version.should == '2'
      end
    end

    it 'should complain if namespace requirement is not satisfied' do
      define 'one' do
        artifact_ns.need :bar => 'foo:bar:baz:~>1.5'
        lambda { artifact_ns.use :bar => '1.4' }.should raise_error(Exception, /unsatisfied/i)
      end
    end

    it 'should be able to register a group' do
      specs = ['its:me:here:1', 'its:you:there:2']
      artifact_ns.use :them => specs
      artifact_ns[:them].map(&:to_spec).should == specs
      artifact_ns['its:me:here'].should_not be_nil
      artifact_ns[:you].should be_nil
    end

    it 'should be able to assign sub namespaces' do
      artifact_ns(:foo).bar = "foo:bar:baz:0"
      artifact_ns(:moo).foo = artifact_ns(:foo)
      artifact_ns(:moo).foo.should == artifact_ns(:foo)
      artifact_ns(:moo).foo_bar.should == artifact_ns(:foo).bar
    end

    it 'should handle symbols with dashes and periods' do
      [:'a-b', :'a.b'].each do |symbol|
        artifact_ns.use symbol => 'a:b:c:1'
        artifact_ns[symbol].version.should == '1'
        artifact_ns[symbol].id.should == 'b'
      end
    end

    it 'should handle version string' do
      foo = artifact_ns do |ns|
        ns.bar = 'a:b:c:1'
      end
      foo.use :bar => '2.0'
      foo.bar.version.should == '2.0'
    end
  end

  describe '#values' do
    it 'returns the artifacts defined on namespace' do
      define 'foo' do
        artifact_ns.use 'foo:one:baz:1.0'
        define 'bar' do
          artifact_ns.use 'foo:two:baz:1.0'

          specs = artifact_ns.values.map(&:to_spec)
          specs.should include('foo:two:baz:1.0')
          specs.should_not include('foo:one:baz:1.0')

          specs = artifact_ns.values(true).map(&:to_spec)
          specs.should include('foo:two:baz:1.0', 'foo:one:baz:1.0')
        end
      end
    end
  end

  describe '#values_at' do
    it 'returns the named artifacts' do
      define 'foo' do
        artifact_ns.use 'foo:one:baz:1.0'
        define 'bar' do
          artifact_ns.use :foo_baz => 'foo:two:baz:1.0'

          specs = artifact_ns.values_at('one').map(&:to_spec)
          specs.should include('foo:one:baz:1.0')
          specs.should_not include('foo:two:baz:1.0')

          specs = artifact_ns.values_at('foo_baz').map(&:to_spec)
          specs.should include('foo:two:baz:1.0')
          specs.should_not include('foo:one:baz:1.0')
        end
      end
    end

    it 'returns first artifacts by their unversioned spec' do
      define 'foo' do
        artifact_ns.use 'foo:one:baz:2.0'
        define 'bar' do
          artifact_ns.use :older => 'foo:one:baz:1.0'

          specs = artifact_ns.values_at('foo:one:baz').map(&:to_spec)
          specs.should include('foo:one:baz:1.0')
          specs.should_not include('foo:one:baz:2.0')
        end
        specs = artifact_ns.values_at('foo:one:baz').map(&:to_spec)
        specs.should include('foo:one:baz:2.0')
        specs.should_not include('foo:one:baz:1.0')
      end
    end

    it 'return first artifact satisfying a dependency' do
      define 'foo' do
        artifact_ns.use 'foo:one:baz:2.0'
        define 'bar' do
          artifact_ns.use :older => 'foo:one:baz:1.0'

          specs = artifact_ns.values_at('foo:one:baz:>1.0').map(&:to_spec)
          specs.should include('foo:one:baz:2.0')
          specs.should_not include('foo:one:baz:1.0')
        end
      end
    end
  end

  describe '#artifacts' do
    it 'returns artifacts in namespace' do
      define 'one' do
        artifact_ns[:foo] = 'group:foo:jar:1'
        artifact_ns[:bar] = 'group:bar:jar:1'
        artifact_ns.artifacts.map{|a| a.to_spec}.should include('group:foo:jar:1', 'group:bar:jar:1')
      end
    end
  end

  describe '#keys' do
    it 'returns names in namespace' do
      define 'one' do
        artifact_ns[:foo] = 'group:foo:jar:1'
        artifact_ns[:bar] = 'group:bar:jar:1'
        artifact_ns.keys.should include('foo', 'bar')
      end
    end
  end

  describe '#delete' do
    it 'deletes corresponding artifact requirement' do
      define 'one' do
        artifact_ns[:foo] = 'group:foo:jar:1'
        artifact_ns[:bar] = 'group:bar:jar:1'
        artifact_ns.delete :bar
        artifact_ns.artifacts.map{|a| a.to_spec}.should include('group:foo:jar:1')
        artifact_ns[:foo].to_spec.should eql('group:foo:jar:1')
      end
    end
  end

  describe '#clear' do
    it 'clears all artifact requirements in namespace' do
      define 'one' do
        artifact_ns[:foo] = 'group:foo:jar:1'
        artifact_ns[:bar] = 'group:bar:jar:1'
        artifact_ns.clear
        artifact_ns.artifacts.should be_empty
      end
    end
  end

  describe '#method_missing' do
    it 'should use cool_aid! to create a requirement' do
      define 'foo' do
        artifact_ns.cool_aid!('cool:aid:jar:2').should be_kind_of(ArtifactNamespace::ArtifactRequirement)
        artifact_ns[:cool_aid].version.should == '2'
        artifact_ns[:cool_aid].should_not be_selected
        define 'bar' do
          artifact_ns.cool_aid! 'cool:aid:man:3', '>2'
          artifact_ns[:cool_aid].version.should == '3'
          artifact_ns[:cool_aid].requirement.should be_satisfied_by('2.5')
          artifact_ns[:cool_aid].should_not be_selected
        end
      end
    end

    it 'should use cool_aid= as shorhand for [:cool_aid]=' do
      artifact_ns.cool_aid = 'cool:aid:jar:1'
      artifact_ns[:cool_aid].should be_selected
    end

    it 'should use cool_aid as shorthand for [:cool_aid]' do
      artifact_ns.need :cool_aid => 'cool:aid:jar:1'
      artifact_ns.cool_aid.should_not be_selected
    end

    it 'should use cool_aid? to test if artifact has been defined and selected' do
      artifact_ns.need :cool_aid => 'cool:aid:jar:>1'
      artifact_ns.should_not have_cool_aid
      artifact_ns.should_not have_unknown
      artifact_ns.cool_aid = '2'
      artifact_ns.should have_cool_aid
    end
  end

  describe '#ns' do
    it 'should create a sub namespace' do
      artifact_ns.ns :foo
      artifact_ns[:foo].should be_kind_of(ArtifactNamespace)
      artifact_ns(:foo).should_not === artifact_ns.foo
      artifact_ns.foo.parent.should == artifact_ns
    end

    it 'should take any use arguments' do
      artifact_ns.ns :foo, :bar => 'foo:bar:jar:0', :baz => 'foo:baz:jar:0'
      artifact_ns.foo.bar.should be_selected
      artifact_ns.foo[:baz].should be_selected
    end

    it 'should access sub artifacts using with foo_bar like syntax' do
      artifact_ns.ns :foo, :bar => 'foo:bar:jar:0', :baz => 'foo:baz:jar:0'
      artifact_ns[:foo_baz].should be_selected
      artifact_ns.foo_bar.should be_selected

      artifact_ns.foo.ns :bat, 'bat:man:jar:>1'
      batman = artifact_ns.foo.bat.man
      batman.should be_selected
      artifact_ns[:foo_bat_man] = '3'
      artifact_ns[:foo_bat_man].should == batman
      artifact_ns[:foo_bat_man].version.should == '3'
    end

    it 'should include sub artifacts when calling #values' do
      artifact_ns.ns :bat, 'bat:man:jar:>1'
      artifact_ns.values.should_not be_empty
      artifact_ns.values.first.unversioned_spec.should == 'bat:man:jar'
    end

    it 'should reopen a sub-namespace' do
      artifact_ns.ns :bat, 'bat:man:jar:>1'
      bat = artifact_ns[:bat]
      bat.should == artifact_ns.ns(:bat)
    end

    it 'should fail reopening if not a sub-namespace' do
      artifact_ns.foo = 'foo:bar:baz:0'
      lambda { artifact_ns.ns(:foo) }.should raise_error(TypeError, /not a sub/i)
    end

    it 'should clone artifacts when assigned' do
      artifact_ns(:foo).bar = "foo:bar:jar:0"
      artifact_ns(:moo).ns :muu, :miu => artifact_ns(:foo).bar
      artifact_ns(:moo).muu.miu.should_not == artifact_ns(:foo).bar
      artifact_ns(:moo).muu.miu.to_spec.should == artifact_ns(:foo).bar.to_spec
    end

    it 'should clone parent artifacts by name' do
      define 'foo' do
        artifact_ns.bar = "foo:bar:jar:0"
        define 'moo' do
          artifact_ns.ns(:muu).use :bar
          artifact_ns.muu_bar.should be_selected
          artifact_ns.muu.bar.should_not == artifact_ns.bar
        end
      end
    end
  end

  it 'should be an Enumerable' do
    artifact_ns.should be_kind_of(Enumerable)
    artifact_ns.use 'foo:bar:baz:1.0'
    artifact_ns.map(&:artifact).should include(artifact('foo:bar:baz:1.0'))
  end

end # ArtifactNamespace

describe Buildr::ArtifactNamespace::ArtifactRequirement do
  before(:each) { Buildr::ArtifactNamespace.clear }
  it 'should be created from artifact_ns' do
    foo = artifact_ns do |ns|
      ns.bar = 'a:b:c:1.0'
    end
    foo.bar.should be_kind_of(ArtifactNamespace::ArtifactRequirement)
  end

  it 'should handle version as string' do
    foo = artifact_ns do |ns|
      ns.bar = 'a:b:c:1.0'
    end
    foo.bar.version = '2.0'
    foo.bar.version.should == '2.0'
  end

  it 'should handle version string directly' do
    foo = artifact_ns do |ns|
      ns.bar = 'a:b:c:1.0'
    end
    foo.bar = '2.0'
    foo.bar.version.should == '2.0'
  end

end # ArtifactRequirement

describe Buildr do
  before(:each) { Buildr::ArtifactNamespace.clear }

  describe '.artifacts' do
    it 'should take ruby symbols and ask the current namespace for them' do
      define 'foo' do
        artifact_ns.cool = 'cool:aid:jar:1.0'
        artifact_ns.use 'some:other:jar:1.0'
        artifact_ns.use 'bat:man:jar:1.0'
        compile.with :cool, :other, :'bat:man:jar'
        compile.dependencies.map(&:to_spec).should include('cool:aid:jar:1.0', 'some:other:jar:1.0', 'bat:man:jar:1.0')
      end
    end

    it 'should take a namespace' do
      artifact_ns(:moo).muu = 'moo:muu:jar:1.0'
      define 'foo' do
        compile.with artifact_ns(:moo)
        compile.dependencies.map(&:to_spec).should include('moo:muu:jar:1.0')
      end
    end
  end

  describe '.artifact' do
    it 'should search current namespace if given a symbol' do
      define 'foo' do
        artifact_ns.use :cool => 'cool:aid:jar:1.0'
        define 'bar' do
          artifact(:cool).should == artifact_ns[:cool].artifact
        end
      end
    end

    it 'should search current namespace if given a symbol spec' do
      define 'foo' do
        artifact_ns.use 'cool:aid:jar:1.0'
        define 'bar' do
          artifact(:'cool:aid:jar').should == artifact_ns[:aid].artifact
        end
      end
    end

    it 'should fail when no artifact by that name is found' do
      define 'foo' do
        artifact_ns.use 'cool:aid:jar:1.0'
        define 'bar' do
          lambda { artifact(:cool) }.should raise_error(IndexError, /artifact/)
        end
      end
    end
  end
end

describe "Extension using ArtifactNamespace" do
  before(:each) { Buildr::ArtifactNamespace.clear }

  def abc_module
    Object.module_eval 'module A; module B; module C; end; end; end'
    yield
  ensure
    Object.send :remove_const, :A
  end

  it 'can register namespace listeners' do
    abc_module do
      # An example extension to illustrate namespace listeners and method forwarding
      class A::Example

        module Ext
          include Buildr::Extension
          def example; @example ||= A::Example.new; end
          before_define do |p|
            Rake::Task.define_task('example') { p.example.doit }
          end
        end

        REQUIRES = ArtifactNamespace.for(self) do |ns|
          ns.xmlbeans! 'org.apache.xmlbeans:xmlbeans:jar:2.3.0', '>2'
          ns.stax_api! 'stax:stax-api:jar:>=1.0.1'
        end

        attr_reader :options, :requires

        def initialize
          # We could actually use the REQUIRES namespace, but to make things
          # a bit more interesting, suppose each Example instance can have its
          # own artifact requirements in adition to those specified on REQUIRES.
          # To achieve this we create an anonymous namespace.
          @requires = ArtifactNamespace.new # a namespace per instance
          REQUIRES.each { |requirement| @requires.need requirement }

          # For user convenience, we make the options object respond to
          #    :xmlbeans, :xmlbeans=, :xmlbeans?
          # forwarding them to the namespace.
          @options = OpenObject.new.extend(@requires.accessor(:xmlbeans, :stax_api))
          # Register callbacks so we can perform some logic when an artifact
          # is selected by the user.
          options.xmlbeans.add_listener &method(:selected_xmlbeans)
          options.stax_api.add_listener do |stax|
            # Now using a proc
            stax.should be_selected
            stax.version.should == '1.6180'
            options[:math] = :golden # customize our options for this version
            # In this example we set the stax version when running outside
            # a project definition. This means we have no access to the project
            # namespace unless we had a reference to the project or knew it's name
            Buildr.artifact_ns(:current).name.should == 'root'
          end
        end

        include RSpec::Matchers # for assertions

        # Called with the ArtifactRequirement that has just been selected
        # by a user. This allows extension author to selectively perform
        # some action by inspecting the requirement state.
        def selected_xmlbeans(xmlbeans)
          xmlbeans.should be_selected
          xmlbeans.version.should == '3.1415'
          options[:math] = :pi
          # This example just sets xmlbeans for foo:bar project
          # So the currently running namespace should have the foo:bar name
          Buildr.artifact_ns(:current).name.should == 'foo:bar'
        end

        # Suppose we invoke an ant task here or something else.
        def doit
          # Now call ant task with our selected artifact and options
          classpath = requires.map(&:artifact).map(&:to_s).join(File::PATH_SEPARATOR)
          lambda { ant('thing') { |ant| ant.classpath classpath, :math => options[:math] } }

          # We are not a Project instance, hence we have no artifact_ns
          lambda { artifact_ns }.should raise_error(NameError)

          # Extension authors may NOT rely project's namespaces.
          # However the ruby-way gives you power and at the same time
          # makes you dangerous, (think open-modules, monkey-patching)
          # Given that buildr is pure ruby, consider it a sharp-edged sword.
          # Having said that, you may actually inspect a project's
          # namespace, but don't write on it without letting your users
          # know you will.
          # This example obtains the current project namespace to make
          # some assertions.

          # To obtain a project's namespace we need either
          # 1) a reference to the project, and call artifact_ns on it
          #      project.artifact_ns  # the namespace for project
          # 2) know the project name
          #      Buildr.artifact_ns('the:project')
          # 3) Use :current to reference the currently running project
          #      Buildr.artifact_ns(:current)
          name = Buildr.artifact_ns(:current).name
          case name
          when 'foo:bar'
            options[:math].should == :pi
            requires.xmlbeans.version.should == '3.1415'
            requires.stax_api.version.should == '1.0.1'
          when 'foo:baz'
            options[:math].should == :golden
            requires.xmlbeans.version.should == '2.3.0'
            requires.stax_api.version.should == '1.6180'
          else
            fail "This example expects foo:bar or foo:baz projects not #{name.inspect}"
          end
        end
      end

      define 'foo' do
        define 'bar' do
          extend A::Example::Ext
          task('setup') do
            example.options.xmlbeans = '3.1415'
          end
          task('run' => [:setup, :example])
        end
        define 'baz' do
          extend A::Example::Ext
        end
      end

      project('foo:bar').example.requires.should_not == project('foo:baz').example.requires
      project('foo:bar').example.requires.xmlbeans.should_not == project('foo:baz').example.requires.xmlbeans

      # current namespace outside a project is :root, see the stax callback
      project('foo:baz').example.options.stax_api = '1.6180'
      # we call the task outside the project, see #doit
      lambda { task('foo:bar:run').invoke }.should run_task('foo:bar:example')
      lambda { task('foo:baz:example').invoke }.should run_task('foo:baz:example')
    end
  end
end
