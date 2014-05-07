require "helper"

module Thor::Util
  def self.clear_user_home!
    @@user_home = nil
  end
end

describe Thor::Util do
  describe "#find_by_namespace" do
    it "returns 'default' if no namespace is given" do
      expect(Thor::Util.find_by_namespace("")).to eq(Scripts::MyDefaults)
    end

    it "adds 'default' if namespace starts with :" do
      expect(Thor::Util.find_by_namespace(":child")).to eq(Scripts::ChildDefault)
    end

    it "returns nil if the namespace can't be found" do
      expect(Thor::Util.find_by_namespace("thor:core_ext:ordered_hash")).to be nil
    end

    it "returns a class if it matches the namespace" do
      expect(Thor::Util.find_by_namespace("app:broken:counter")).to eq(BrokenCounter)
    end

    it "matches classes default namespace" do
      expect(Thor::Util.find_by_namespace("scripts:my_script")).to eq(Scripts::MyScript)
    end
  end

  describe "#namespace_from_thor_class" do
    it "replaces constant nesting with command namespacing" do
      expect(Thor::Util.namespace_from_thor_class("Foo::Bar::Baz")).to eq("foo:bar:baz")
    end

    it "snake-cases component strings" do
      expect(Thor::Util.namespace_from_thor_class("FooBar::BarBaz::BazBoom")).to eq("foo_bar:bar_baz:baz_boom")
    end

    it "accepts class and module objects" do
      expect(Thor::Util.namespace_from_thor_class(Thor::CoreExt::OrderedHash)).to eq("thor:core_ext:ordered_hash")
      expect(Thor::Util.namespace_from_thor_class(Thor::Util)).to eq("thor:util")
    end

    it "removes Thor::Sandbox namespace" do
      expect(Thor::Util.namespace_from_thor_class("Thor::Sandbox::Package")).to eq("package")
    end
  end

  describe "#namespaces_in_content" do
    it "returns an array of names of constants defined in the string" do
      list = Thor::Util.namespaces_in_content("class Foo; class Bar < Thor; end; end; class Baz; class Bat; end; end")
      expect(list).to include("foo:bar")
      expect(list).not_to include("bar:bat")
    end

    it "doesn't put the newly-defined constants in the enclosing namespace" do
      Thor::Util.namespaces_in_content("class Blat; end")
      expect(defined?(Blat)).not_to be
      expect(defined?(Thor::Sandbox::Blat)).to be
    end
  end

  describe "#snake_case" do
    it "preserves no-cap strings" do
      expect(Thor::Util.snake_case("foo")).to eq("foo")
      expect(Thor::Util.snake_case("foo_bar")).to eq("foo_bar")
    end

    it "downcases all-caps strings" do
      expect(Thor::Util.snake_case("FOO")).to eq("foo")
      expect(Thor::Util.snake_case("FOO_BAR")).to eq("foo_bar")
    end

    it "downcases initial-cap strings" do
      expect(Thor::Util.snake_case("Foo")).to eq("foo")
    end

    it "replaces camel-casing with underscores" do
      expect(Thor::Util.snake_case("FooBarBaz")).to eq("foo_bar_baz")
      expect(Thor::Util.snake_case("Foo_BarBaz")).to eq("foo_bar_baz")
    end

    it "places underscores between multiple capitals" do
      expect(Thor::Util.snake_case("ABClass")).to eq("a_b_class")
    end
  end

  describe "#find_class_and_command_by_namespace" do
    it "returns a Thor::Group class if full namespace matches" do
      expect(Thor::Util.find_class_and_command_by_namespace("my_counter")).to eq([MyCounter, nil])
    end

    it "returns a Thor class if full namespace matches" do
      expect(Thor::Util.find_class_and_command_by_namespace("thor")).to eq([Thor, nil])
    end

    it "returns a Thor class and the command name" do
      expect(Thor::Util.find_class_and_command_by_namespace("thor:help")).to eq([Thor, "help"])
    end

    it "falls back in the namespace:command look up even if a full namespace does not match" do
      Thor.const_set(:Help, Module.new)
      expect(Thor::Util.find_class_and_command_by_namespace("thor:help")).to eq([Thor, "help"])
      Thor.send :remove_const, :Help
    end

    it "falls back on the default namespace class if nothing else matches" do
      expect(Thor::Util.find_class_and_command_by_namespace("test")).to eq([Scripts::MyDefaults, "test"])
    end
  end

  describe "#thor_classes_in" do
    it "returns thor classes inside the given class" do
      expect(Thor::Util.thor_classes_in(MyScript)).to eq([MyScript::AnotherScript])
      expect(Thor::Util.thor_classes_in(MyScript::AnotherScript)).to be_empty
    end
  end

  describe "#user_home" do
    before do
      allow(ENV).to receive(:[])
      Thor::Util.clear_user_home!
    end

    it "returns the user path if no variable is set on the environment" do
      expect(Thor::Util.user_home).to eq(File.expand_path("~"))
    end

    it "returns the *nix system path if file cannot be expanded and separator does not exist" do
      expect(File).to receive(:expand_path).with("~").and_raise(RuntimeError)
      previous_value = File::ALT_SEPARATOR
      capture(:stderr) { File.const_set(:ALT_SEPARATOR, false) } # rubocop:disable SymbolName
      expect(Thor::Util.user_home).to eq("/")
      capture(:stderr) { File.const_set(:ALT_SEPARATOR, previous_value) } # rubocop:disable SymbolName
    end

    it "returns the windows system path if file cannot be expanded and a separator exists" do
      expect(File).to receive(:expand_path).with("~").and_raise(RuntimeError)
      previous_value = File::ALT_SEPARATOR
      capture(:stderr) { File.const_set(:ALT_SEPARATOR, true) } # rubocop:disable SymbolName
      expect(Thor::Util.user_home).to eq("C:/")
      capture(:stderr) { File.const_set(:ALT_SEPARATOR, previous_value) } # rubocop:disable SymbolName
    end

    it "returns HOME/.thor if set" do
      allow(ENV).to receive(:[]).with("HOME").and_return("/home/user/")
      expect(Thor::Util.user_home).to eq("/home/user/")
    end

    it "returns path with HOMEDRIVE and HOMEPATH if set" do
      allow(ENV).to receive(:[]).with("HOMEDRIVE").and_return("D:/")
      allow(ENV).to receive(:[]).with("HOMEPATH").and_return("Documents and Settings/James")
      expect(Thor::Util.user_home).to eq("D:/Documents and Settings/James")
    end

    it "returns APPDATA/.thor if set" do
      allow(ENV).to receive(:[]).with("APPDATA").and_return("/home/user/")
      expect(Thor::Util.user_home).to eq("/home/user/")
    end
  end

  describe "#thor_root_glob" do
    before do
      allow(ENV).to receive(:[])
      Thor::Util.clear_user_home!
    end

    it "escapes globs in path" do
      allow(ENV).to receive(:[]).with("HOME").and_return("/home/user{1}/")
      expect(Dir).to receive(:[]).with('/home/user\\{1\\}/.thor/*').and_return([])
      expect(Thor::Util.thor_root_glob).to eq([])
    end
  end

  describe "#globs_for" do
    it "escapes globs in path" do
      expect(Thor::Util.globs_for("/home/apps{1}")).to eq([
        '/home/apps\\{1\\}/Thorfile',
        '/home/apps\\{1\\}/*.thor',
        '/home/apps\\{1\\}/tasks/*.thor',
        '/home/apps\\{1\\}/lib/tasks/*.thor'
      ])
    end
  end

  describe "#escape_globs" do
    it "escapes ? * { } [ ] glob characters" do
      expect(Thor::Util.escape_globs("apps?")).to eq('apps\\?')
      expect(Thor::Util.escape_globs("apps*")).to eq('apps\\*')
      expect(Thor::Util.escape_globs("apps {1}")).to eq('apps \\{1\\}')
      expect(Thor::Util.escape_globs("apps [1]")).to eq('apps \\[1\\]')
    end
  end
end
