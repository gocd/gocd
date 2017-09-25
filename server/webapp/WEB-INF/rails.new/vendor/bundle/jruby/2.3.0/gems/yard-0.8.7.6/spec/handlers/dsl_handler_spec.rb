require File.dirname(__FILE__) + '/spec_helper'
require 'ostruct'

describe "YARD::Handlers::Ruby::#{LEGACY_PARSER ? "Legacy::" : ""}DSLHandler" do
  before(:all) { parse_file :dsl_handler_001, __FILE__ }

  it "should create a readable attribute when @!attribute r is found" do
    obj = Registry.at('Foo#attr1')
    obj.should_not be_nil
    obj.should be_reader
    obj.tag(:return).types.should == ['Numeric']
    Registry.at('Foo#attr1=').should be_nil
  end

  it "should create a writable attribute when @!attribute w is found" do
    obj = Registry.at('Foo#attr2=')
    obj.should_not be_nil
    obj.should be_writer
    Registry.at('Foo#attr2').should be_nil
  end

  it "should default to readwrite @!attribute" do
    obj = Registry.at('Foo#attr3')
    obj.should_not be_nil
    obj.should be_reader
    obj = Registry.at('Foo#attr3=')
    obj.should_not be_nil
    obj.should be_writer
  end

  it "should allow @!attribute to define alternate method name" do
    Registry.at('Foo#attr4').should be_nil
    Registry.at('Foo#custom').should_not be_nil
  end

  it "should default to creating an instance method for any DSL method with special tags" do
    obj = Registry.at('Foo#implicit0')
    obj.should_not be_nil
    obj.docstring.should == "IMPLICIT METHOD!"
    obj.tag(:return).types.should == ['String']
  end

  it "should recognize implicit docstring when it has scope tag" do
    obj = Registry.at("Foo.implicit1")
    obj.should_not be_nil
    obj.scope.should == :class
  end

  it "should recognize implicit docstring when it has visibility tag" do
    obj = Registry.at("Foo#implicit2")
    obj.should_not be_nil
    obj.visibility.should == :protected
  end

  it "should not recognize implicit docstring with any other normal tag" do
    obj = Registry.at('Foo#implicit_invalid3')
    obj.should be_nil
  end

  it "should set the method name when using @!method" do
    obj = Registry.at('Foo.xyz')
    obj.should_not be_nil
    obj.signature.should == 'def xyz(a, b, c)'
    obj.parameters.should == [['a', nil], ['b', nil], ['c', nil]]
    obj.source.should == 'foo_bar'
    obj.docstring.should == 'The foo method'
  end

  it "should allow setting of @!scope" do
    Registry.at('Foo.xyz').scope.should == :class
  end

  it "should create module function if @!scope is module" do
    mod_c = Registry.at('Foo.modfunc1')
    mod_i = Registry.at('Foo#modfunc1')
    mod_c.scope.should == :class
    mod_i.visibility.should == :private
  end

  it "should allow setting of @!visibility" do
    Registry.at('Foo.xyz').visibility.should == :protected
  end

  it "should ignore DSL methods without tags" do
    Registry.at('Foo#implicit_invalid').should be_nil
  end

  it "should accept a DSL method without tags if it has hash_flag (##)" do
    Registry.at('Foo#implicit_valid').should_not be_nil
    Registry.at('Foo#implicit_invalid2').should be_nil
  end

  it "should allow creation of macros" do
    macro = CodeObjects::MacroObject.find('property')
    macro.should_not be_nil
    macro.should_not be_attached
    macro.method_object.should be_nil
  end

  it "should handle macros with no parameters to expand" do
    Registry.at('Foo#none').should_not be_nil
    Registry.at('Baz#none').signature.should == 'def none(foo, bar)'
  end

  it "should expand $N on method definitions" do
    Registry.at('Foo#regular_meth').docstring.should == 'a b c'
  end

  it "should apply new macro docstrings on new objects" do
    obj = Registry.at('Foo#name')
    obj.should_not be_nil
    obj.source.should == 'property :name, String, :a, :b, :c'
    obj.signature.should == 'def name(a, b, c)'
    obj.docstring.should == 'A property that is awesome.'
    obj.tag(:param).name.should == 'a'
    obj.tag(:param).text.should == 'first parameter'
    obj.tag(:return).types.should == ['String']
    obj.tag(:return).text.should == 'the property name'
  end

  it "should allow reuse of named macros" do
    obj = Registry.at('Foo#age')
    obj.should_not be_nil
    obj.source.should == 'property :age, Fixnum, :value'
    obj.signature.should == 'def age(value)'
    obj.docstring.should == 'A property that is awesome.'
    obj.tag(:param).name.should == 'value'
    obj.tag(:param).text.should == 'first parameter'
    obj.tag(:return).types.should == ['Fixnum']
    obj.tag(:return).text.should == 'the property age'
  end

  it "should know about method information on DSL with macro expansion" do
    Registry.at('Foo#right_name').should_not be_nil
    Registry.at('Foo#right_name').source.should ==
      'implicit_with_different_method_name :wrong, :right'
    Registry.at('Foo#wrong_name').should be_nil
  end

  it "should use attached macros" do
    macro = CodeObjects::MacroObject.find('parser')
    macro.macro_data.should == "@!method $1(opts = {})\n@return NOTHING!"
    macro.should_not be_nil
    macro.should be_attached
    macro.method_object.should == P('Foo.parser')
    obj = Registry.at('Foo#c_parser')
    obj.should_not be_nil
    obj.docstring.should == ""
    obj.signature.should == "def c_parser(opts = {})"
    obj.docstring.tag(:return).text.should == "NOTHING!"
  end

  it "should append docstring on DSL method to attached macro" do
    obj = Registry.at('Foo#d_parser')
    obj.should_not be_nil
    obj.docstring.should == "Another docstring"
    obj.signature.should == "def d_parser(opts = {})"
    obj.docstring.tag(:return).text.should == "NOTHING!"
  end

  it "should only use attached macros on methods defined in inherited hierarchy" do
    Registry.at('Bar#x_parser').should be_nil
    Registry.at('Baz#y_parser').should_not be_nil
  end

  it "should look through mixins for attached macros" do
    meth = Registry.at('Baz#mixin_method')
    meth.should_not be_nil
    meth.docstring.should == 'DSL method mixin_method'
  end

  it "should handle top-level DSL methods" do
    obj = Registry.at('#my_other_method')
    obj.should_not be_nil
    obj.docstring.should == "Docstring for method"
  end

  it "should handle Constant.foo syntax" do
    obj = Registry.at('#beep')
    obj.should_not be_nil
    obj.signature.should == 'def beep(a, b, c)'
  end

  it "should expand attached macros in first DSL method" do
    Registry.at('DSLMethods#foo').docstring.should == "Returns String for foo"
    Registry.at('DSLMethods#bar').docstring.should == "Returns Integer for bar"
  end

  it "should not detect implicit macros with invalid method names" do
    undoc_error <<-eof
      ##
      # IMPLICIT METHOD THAT SHOULD
      # NOT BE DETECTED
      dsl_method '/foo/bar'
    eof
  end
end