require File.join(File.dirname(__FILE__), "spec_helper")
include CodeObjects

require "thread"

describe YARD::Registry do
  before { Registry.clear }

  describe '.yardoc_file_for_gem' do
    before do
      @gem = mock('gem')
      @gem.stub!(:name).and_return('foo')
      @gem.stub!(:full_name).and_return('foo-1.0')
      @gem.stub!(:full_gem_path).and_return('/path/to/foo')
    end

    it "should return nil if gem isn't found" do
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([])
      Registry.yardoc_file_for_gem('foo').should == nil
    end

    it "should allow version to be specified" do
      Gem.source_index.should_receive(:find_name).with('foo', '= 2').and_return([])
      Registry.yardoc_file_for_gem('foo', '= 2').should == nil
    end

    it "should return existing .yardoc path for gem when for_writing=false" do
      File.should_receive(:exist?).and_return(false)
      File.should_receive(:exist?).with('/path/to/foo/.yardoc').and_return(true)
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([@gem])
      Registry.yardoc_file_for_gem('foo').should == '/path/to/foo/.yardoc'
    end

    it "should return nil if no .yardoc path exists in gem when for_writing=false" do
      File.should_receive(:exist?).and_return(false)
      File.should_receive(:exist?).with('/path/to/foo/.yardoc').and_return(false)
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([@gem])
      Registry.yardoc_file_for_gem('foo').should == nil
    end

    it "should search local gem path first if for_writing=false" do
      File.should_receive(:exist?).and_return(true)
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([@gem])
      Registry.yardoc_file_for_gem('foo').should =~ %r{/.yard/gem_index/foo-1.0.yardoc$}
    end

    it "should return global .yardoc path for gem if for_writing=true and dir is writable" do
      File.should_receive(:writable?).with(@gem.full_gem_path).and_return(true)
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([@gem])
      Registry.yardoc_file_for_gem('foo', '>= 0', true).should == '/path/to/foo/.yardoc'
    end

    it "should return local .yardoc path for gem if for_writing=true and dir is not writable" do
      File.should_receive(:writable?).with(@gem.full_gem_path).and_return(false)
      Gem.source_index.should_receive(:find_name).with('foo', '>= 0').and_return([@gem])
      Registry.yardoc_file_for_gem('foo', '>= 0', true).should =~ %r{/.yard/gem_index/foo-1.0.yardoc$}
    end

    it "should return gem path if gem starts with yard-doc- and for_writing=false" do
      @gem.stub!(:name).and_return('yard-doc-core')
      @gem.stub!(:full_name).and_return('yard-doc-core-1.0')
      @gem.stub!(:full_gem_path).and_return('/path/to/yard-doc-core')
      Gem.source_index.should_receive(:find_name).with('yard-doc-core', '>= 0').and_return([@gem])
      File.should_receive(:exist?).with('/path/to/yard-doc-core/.yardoc').and_return(true)
      Registry.yardoc_file_for_gem('yard-doc-core').should == '/path/to/yard-doc-core/.yardoc'
    end

    it "should return nil if gem starts with yard-doc- and for_writing=true" do
      @gem.stub!(:name).and_return('yard-doc-core')
      @gem.stub!(:full_name).and_return('yard-doc-core-1.0')
      @gem.stub!(:full_gem_path).and_return('/path/to/yard-doc-core')
      Gem.source_index.should_receive(:find_name).with('yard-doc-core', '>= 0').and_return([@gem])
      File.should_receive(:exist?).with('/path/to/yard-doc-core/.yardoc').and_return(true)
      Registry.yardoc_file_for_gem('yard-doc-core', '>= 0', true).should == nil
    end
  end

  describe '.root' do
    it "should have an empty path for root" do
      Registry.root.path.should == ""
    end
  end

  describe '.locale' do
    it "should load locale object" do
      fr_locale = I18n::Locale.new("fr")
      store = Registry.send(:thread_local_store)
      store.should_receive(:locale).with("fr").and_return(fr_locale)
      Registry.locale("fr").should == fr_locale
    end
  end

  describe '.resolve' do
    it "should resolve any existing namespace" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ModuleObject.new(o1, :B)
      o3 = ModuleObject.new(o2, :C)
      Registry.resolve(o1, "B::C").should == o3
      Registry.resolve(:root, "A::B::C")
    end

    it "should resolve an object in the root namespace when prefixed with ::" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ModuleObject.new(o1, :B)
      o3 = ModuleObject.new(o2, :C)
      Registry.resolve(o3, "::A").should == o1

      Registry.resolve(o3, "::String", false, true).should == P(:String)
    end

    it "should resolve instance methods with # prefix" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ModuleObject.new(o1, :B)
      o3 = ModuleObject.new(o2, :C)
      o4 = MethodObject.new(o3, :methname)
      Registry.resolve(o1, "B::C#methname").should == o4
      Registry.resolve(o2, "C#methname").should == o4
      Registry.resolve(o3, "#methname").should == o4
    end

    it "should resolve instance methods in the root without # prefix" do
      o = MethodObject.new(:root, :methname)
      Registry.resolve(:root, 'methname').should == o
    end

    it "should resolve superclass methods when inheritance = true" do
      superyard = ClassObject.new(:root, :SuperYard)
      yard = ClassObject.new(:root, :YARD)
      yard.superclass = superyard
      imeth = MethodObject.new(superyard, :hello)
      cmeth = MethodObject.new(superyard, :class_hello, :class)

      Registry.resolve(yard, "#hello", false).should be_nil
      Registry.resolve(yard, "#hello", true).should == imeth
      Registry.resolve(yard, "class_hello", false).should be_nil
      Registry.resolve(yard, "class_hello", true).should == cmeth
    end

    it "should resolve mixin methods when inheritance = true" do
      yard = ClassObject.new(:root, :YARD)
      mixin = ModuleObject.new(:root, :Mixin)
      yard.mixins(:instance) << mixin
      imeth = MethodObject.new(mixin, :hello)
      cmeth = MethodObject.new(mixin, :class_hello, :class)

      Registry.resolve(yard, "#hello", false).should be_nil
      Registry.resolve(yard, "#hello", true).should == imeth
      Registry.resolve(yard, "class_hello", false).should be_nil
      Registry.resolve(yard, "class_hello", true).should == cmeth
    end

    it "should resolve methods in Object when inheritance = true" do
      YARD.parse_string <<-eof
        class Object; def foo; end end
        class A; end
        class MyObject < A; end
      eof

      Registry.resolve(P('MyObject'), '#foo', true).should == P('Object#foo')
    end

    it "should resolve methods in BasicObject when inheritance = true" do
      YARD.parse_string <<-eof
        class BasicObject; def foo; end end
        class A; end
        class MyObject < A; end
      eof

      Registry.resolve(P('MyObject'), '#foo', true).should == P('BasicObject#foo')
    end

    it "should not resolve methods in Object if inheriting BasicObject when inheritance = true" do
      YARD.parse_string <<-eof
        class Object; def foo; end end
        class MyObject < BasicObject; end
      eof

      Registry.resolve(P('MyObject'), '#foo', true).should be_nil
    end

    it "should allow type=:typename to ensure resolved object is of a certain type" do
      YARD.parse_string "class Foo; end"
      Registry.resolve(Registry.root, 'Foo').should == Registry.at('Foo')
      Registry.resolve(Registry.root, 'Foo', false, false, :method).should be_nil
    end

    it "should allow keep trying to find obj where type equals object type" do
      YARD.parse_string <<-eof
        module Foo
          class Bar; end
          def self.Bar; end
        end
      eof
      Registry.resolve(P('Foo'), 'Bar').should == Registry.at('Foo::Bar')
      Registry.resolve(P('Foo'), 'Bar', false, false, :method).should == 
        Registry.at('Foo.Bar')
    end

    it "should return proxy fallback with given type if supplied" do
      YARD.parse_string "module Foo; end"
      proxy = Registry.resolve(P('Foo'), 'Bar', false, true, :method)
      proxy.type.should == :method
      proxy = Registry.resolve(P('Qux'), 'Bar', false, true, :method)
      proxy.type.should == :method
    end

    it "should only check 'Path' in lookup on root namespace" do
      Registry.should_receive(:at).once.with('Test').and_return(true)
      Registry.resolve(Registry.root, "Test")
    end

    it "should not perform lookup by joining namespace and name without separator" do
      yard = ClassObject.new(:root, :YARD)
      Registry.should_not_receive(:at).with('YARDB')
      Registry.resolve(yard, 'B')
    end
  end

  describe '.all' do
    it "should return objects of types specified by arguments" do
      ModuleObject.new(:root, :A)
      o1 = ClassObject.new(:root, :B)
      o2 = MethodObject.new(:root, :testing)
      r = Registry.all(:method, :class)
      r.should include(o1, o2)
    end

    it "should return code objects" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ClassObject.new(:root, :B)
      MethodObject.new(:root, :testing)
      r = Registry.all.select {|t| NamespaceObject === t }
      r.should include(o1, o2)
    end

    it "should allow .all to omit list" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ClassObject.new(:root, :B)
      r = Registry.all
      r.should include(o1, o2)
    end
  end

  describe '.paths' do
    it "should return all object paths" do
      o1 = ModuleObject.new(:root, :A)
      o2 = ClassObject.new(:root, :B)
      Registry.paths.should include('A', 'B')
    end
  end

  describe '.load_yardoc' do
    it "should delegate load to RegistryStore" do
      store = RegistryStore.new
      store.should_receive(:load).with('foo')
      RegistryStore.should_receive(:new).and_return(store)
      Registry.yardoc_file = 'foo'
      Registry.load_yardoc
    end

    it "should return itself" do
      Registry.load_yardoc.should == Registry
    end

    it "should maintain hash key equality on loaded objects" do
      Registry.clear
      Registry.load!(File.dirname(__FILE__) + '/serializers/data/serialized_yardoc')
      baz = Registry.at('Foo#baz')
      Registry.at('Foo').aliases.keys.should include(baz)
      Registry.at('Foo').aliases.has_key?(baz).should == true
    end
  end

  ['load', 'load_all', 'load!'].each do |meth|
    describe('.' + meth) do
      it "should return itself" do
        Registry.send(meth).should == Registry
      end
    end
  end

  describe '.each' do
    before do
      YARD.parse_string "def a; end; def b; end; def c; end"
    end

    after { Registry.clear }

    it "should iterate over .all" do
      items = []
      Registry.each {|x| items << x.path }
      items.sort.should == ['#a', '#b', '#c']
    end

    it "should include Enumerable and allow for find, select" do
      Registry.find {|x| x.path == "#a" }.should be_a(CodeObjects::MethodObject)
    end
  end

  describe '.instance' do
    it "should return itself" do
      Registry.instance.should == Registry
    end
  end

  describe '.single_object_db' do
    it "should default to nil" do
      Registry.single_object_db.should == nil
      Thread.new { Registry.single_object_db.should == nil }.join
    end
  end

  describe 'Thread local' do
    it "should maintain two Registries in separate threads" do
      barrier = 0
      mutex   = Mutex.new
      threads = []
      threads << Thread.new do
        Registry.clear
        YARD.parse_string "# docstring 1\nclass Foo; end"
        mutex.synchronize { barrier += 1 }
        while barrier < 2 do
          s = "barrier < 2, spinning"
        end
        Registry.at('Foo').docstring.should == "docstring 1"
      end
      threads << Thread.new do
        Registry.clear
        YARD.parse_string "# docstring 2\nclass Foo; end"
        mutex.synchronize { barrier += 1 }
        while barrier < 2 do
          s = "barrier < 2, spinning"
        end
        Registry.at('Foo').docstring.should == "docstring 2"
      end
      threads.each {|t| t.join }
    end

    it "should allow setting of yardoc_file in separate threads" do
      barrier = 0
      mutex   = Mutex.new
      threads = []
      threads << Thread.new do
        Registry.yardoc_file.should == '.yardoc'
        Registry.yardoc_file = 'foo'
        mutex.synchronize { barrier += 1 }
        while barrier == 1 do
          s = "barrier = 1, spinning"
        end
        Registry.yardoc_file.should == 'foo'
      end
      threads << Thread.new do
        while barrier == 0 do
          s = "barrier = 0, spinning"
        end
        Registry.yardoc_file.should == '.yardoc'
        mutex.synchronize { barrier += 1 }
        Registry.yardoc_file = 'foo2'
      end
      threads.each {|t| t.join }
      Registry.yardoc_file = Registry::DEFAULT_YARDOC_FILE
    end

    it "should automatically clear in new threads" do
      Thread.new { Registry.all.should be_empty }.join
    end

    it "should allow setting of po_dir in separate threads" do
      barrier = 0
      mutex   = Mutex.new
      threads = []
      threads << Thread.new do
        Registry.po_dir.should == 'po'
        Registry.po_dir = 'locale'
        mutex.synchronize { barrier += 1 }
        while barrier == 1 do
          s = "barrier = 1, spinning"
        end
        Registry.po_dir.should == 'locale'
      end
      threads << Thread.new do
        while barrier == 0 do
          s = "barrier = 0, spinning"
        end
        Registry.po_dir.should == 'po'
        mutex.synchronize { barrier += 1 }
        Registry.po_dir = '.'
      end
      threads.each {|t| t.join }
      Registry.po_dir = Registry::DEFAULT_PO_DIR
    end
  end
end
