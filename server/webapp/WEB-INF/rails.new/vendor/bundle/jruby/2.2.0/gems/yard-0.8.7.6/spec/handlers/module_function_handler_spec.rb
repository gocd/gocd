require File.dirname(__FILE__) + '/spec_helper'

describe "YARD::Handlers::Ruby::#{LEGACY_PARSER ? "Legacy::" : ""}VisibilityHandler" do
  after { Registry.clear }

  def assert_module_function(namespace, name)
    klass = Registry.at("#{namespace}.#{name}")
    instance = Registry.at("#{namespace}##{name}")
    klass.should_not be_nil
    instance.should_not be_nil
    klass.should be_module_function
    instance.should_not be_module_function
    klass.visibility.should == :public
    instance.visibility.should == :private
  end

  it "should be able to create a module function with parameters" do
    YARD.parse_string <<-eof
      module Foo
        def bar; end
        def baz; end

        module_function :bar, :baz
      end
    eof
    assert_module_function('Foo', 'bar')
    assert_module_function('Foo', 'baz')
  end

  it "should be able to set scope for duration of block without params" do
    YARD.parse_string <<-eof
      module Foo
        def qux; end

        module_function

        def bar; end
        def baz; end
      end
    eof
    Registry.at('Foo.qux').should be_nil
    assert_module_function('Foo', 'bar')
    assert_module_function('Foo', 'baz')
  end

  # @bug gh-563
  it "should copy tags to module function properly" do
    YARD.parse_string <<-eof
      module Foo
        # @param [String] foo bar
        # @option foo [String] bar (nil) baz
        # @return [void]
        def bar(foo); end
        module_function :bar
      end
    eof
    assert_module_function('Foo', 'bar')
    o = Registry.at('Foo.bar')
    o.tag(:param).types.should == ['String']
    o.tag(:param).name.should == 'foo'
    o.tag(:param).text.should == 'bar'
    o.tag(:option).name.should == 'foo'
    o.tag(:option).pair.types.should == ['String']
    o.tag(:option).pair.defaults.should == ['nil']
    o.tag(:option).pair.text.should == 'baz'
    o.tag(:return).types.should == ['void']
  end

  it "should handle all method names in parameters" do
    YARD.parse_string <<-eof
      module Foo
        def -(t); end
        def ==(other); end
        def a?; end
        module_function :-, '==', :a?
      end
    eof
    assert_module_function('Foo', '-')
    assert_module_function('Foo', '==')
    assert_module_function('Foo', 'a?')
  end

  it "should only accept strings and symbols" do
    YARD.parse_string <<-eof
      module Foo
        module_function name
        module_function *argument
        module_function *(method_call)
      end
    eof
    Registry.at('Foo#name').should be_nil
    Registry.at('Foo#argument').should be_nil
    Registry.at('Foo#method_call').should be_nil
  end

  it "should handle constants passed in as symbols" do
    YARD.parse_string <<-eof
      module Foo
        def Foo; end
        module_function :Foo
      end
    eof
    assert_module_function('Foo', 'Foo')
  end
end
