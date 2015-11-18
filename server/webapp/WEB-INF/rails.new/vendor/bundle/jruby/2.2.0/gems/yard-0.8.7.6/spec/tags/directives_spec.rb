require File.dirname(__FILE__) + "/../spec_helper"

def tag_parse(content, object = nil, handler = nil)
  @parser = DocstringParser.new
  @parser.parse(content, object, handler)
  @parser
end

describe YARD::Tags::ParseDirective do
  describe '#call' do
    after { Registry.clear }

    it "should parse if handler=nil but use file=(stdin)" do
      tag_parse %{@!parse
        # Docstring here
        def foo; end
      }
      Registry.at('#foo').docstring.should == "Docstring here"
      Registry.at('#foo').file.should == '(stdin)'
    end

    it "should allow parser type to be specified in type" do
      tag_parse %{@!parse [c]
        void Init_Foo() {
          rb_define_method(rb_cMyClass, "foo", foo, 1);
        }
      }
      Registry.at('MyClass#foo').should_not be_nil
    end

    it "should parse code in context of current handler" do
      src = <<-eof
        class A
          # @!parse
          #   def foo; end
          eval "def foo; end"
        end
      eof
      parser = Parser::SourceParser.new
      parser.file = "myfile.rb"
      parser.parse(StringIO.new(src))
      Registry.at('A#foo').file.should == 'myfile.rb'
    end
  end
end

describe YARD::Tags::GroupDirective do
  describe '#call' do
    it "should do nothing if handler=nil" do
      tag_parse("@!group foo")
    end

    it "should set group value in parser state (with handler)" do
      handler = OpenStruct.new(:extra_state => OpenStruct.new)
      tag_parse("@!group foo", nil, handler)
      handler.extra_state.group.should == 'foo'
    end
  end
end

describe YARD::Tags::EndGroupDirective do
  describe '#call' do
    it "should do nothing if handler=nil" do
      tag_parse("@!endgroup foo")
    end

    it "should set group value in parser state (with handler)" do
      handler = OpenStruct.new(:extra_state => OpenStruct.new(:group => "foo"))
      tag_parse("@!endgroup", nil, handler)
      handler.extra_state.group.should be_nil
    end
  end
end

describe YARD::Tags::MacroDirective do
  def handler
    OpenStruct.new(:call_params => %w(a b c),
                   :caller_method => 'foo',
                   :scope => :instance, :visibility => :public,
                   :namespace => P('Foo::Bar'),
                   :statement => OpenStruct.new(:source => 'foo :a, :b, :c'))
  end

  after(:all) { Registry.clear }

  describe '#call' do
    it "should define new macro when [new] is provided" do
      tag_parse("@!macro [new] foo\n  foo")
      CodeObjects::MacroObject.find('foo').macro_data.should == 'foo'
    end

    it "should define new macro if text block is provided" do
      tag_parse("@!macro bar\n  bar")
      CodeObjects::MacroObject.find('bar').macro_data.should == 'bar'
    end

    it "should expand macros and return #expanded_text to tag parser" do
      tag_parse("@!macro [new] foo\n  foo")
      tag_parse("@!macro foo").text.should == 'foo'
    end

    it "should not expand new macro if docstring is unattached" do
      tag_parse("@!macro [new] foo\n  foo").text.should_not == 'foo'
    end

    it "should expand new anonymous macro even if docstring is unattached" do
      tag_parse("@!macro\n  foo").text.should == 'foo'
    end

    it "should allow multiple macros to be expanded" do
      tag_parse("@!macro [new] foo\n  foo")
      tag_parse("@!macro bar\n  bar")
      tag_parse("@!macro foo\n@!macro bar").text.should == "foo\nbar"
    end

    it "should allow anonymous macros" do
      tag_parse("@!macro\n  a b c", nil, handler)
      @parser.text.should == 'a b c'
    end

    it "should expand call_params and caller_method using $N when handler is provided" do
      tag_parse("@!macro\n  $1 $2 $3", nil, handler)
      @parser.text.should == 'a b c'
    end

    it "should attach macro to method if one exists" do
      tag_parse("@!macro [attach] attached\n  $1 $2 $3", nil, handler)
      macro = CodeObjects::MacroObject.find('attached')
      macro.method_object.should == P('Foo::Bar.foo')
    end

    it "should not expand new attached macro if defined on class method" do
      baz = CodeObjects::MethodObject.new(P('Foo::Bar'), :baz, :class)
      baz.visibility.should == :public
      tag_parse("@!macro [attach] attached2\n  @!visibility private", baz, handler)
      macro = CodeObjects::MacroObject.find('attached2')
      macro.method_object.should == P('Foo::Bar.baz')
      baz.visibility.should == :public
    end

    it "should expand macro if defined on class method and there is no data block" do
      tag_parse("@!macro [new] attached3\n  expanded_data")
      baz = CodeObjects::MethodObject.new(P('Foo::Bar'), :baz, :class)
      doc = DocstringParser.new.parse('@!macro attached3', baz, handler).to_docstring
      doc.should == 'expanded_data'
    end

    it "should not attempt to expand macro values if handler = nil" do
      tag_parse("@!macro [attach] xyz\n  $1 $2 $3")
    end
  end
end

describe YARD::Tags::MethodDirective do
  describe '#call' do
    after { Registry.clear }

    it "should use entire docstring if no indented data is found" do
      YARD.parse_string <<-eof
        class Foo
          # @!method foo
          # @!method bar
          # @!scope class
        end
      eof
      Registry.at('Foo.foo').should be_a(CodeObjects::MethodObject)
      Registry.at('Foo.bar').should be_a(CodeObjects::MethodObject)
    end

    it "should handle indented block text in @!method" do
      YARD.parse_string <<-eof
        # @!method foo(a)
        #   Docstring here
        #   @return [String] the foo
        # Ignore this
        # @param [String] a
      eof
      foo = Registry.at('#foo')
      foo.docstring.should == "Docstring here"
      foo.docstring.tag(:return).should_not be_nil
      foo.tag(:param).should be_nil
    end

    it "should execute directives on object in indented block" do
      YARD.parse_string <<-eof
        class Foo
          # @!method foo(a)
          #   @!scope class
          #   @!visibility private
          # @!method bar
          #   Hello
          # Ignore this
        end
      eof
      foo = Registry.at('Foo.foo')
      foo.visibility.should == :private
      bar = Registry.at('Foo#bar')
      bar.visibility.should == :public
    end

    it "should be able to define multiple @methods in docstring" do
      YARD.parse_string <<-eof
        class Foo
          # @!method foo1
          #   Docstring1
          # @!method foo2
          #   Docstring2
          # @!method foo3
          #   @!scope class
          #   Docstring3
        end
      eof
      foo1 = Registry.at('Foo#foo1')
      foo2 = Registry.at('Foo#foo2')
      foo3 = Registry.at('Foo.foo3')
      foo1.docstring.should == 'Docstring1'
      foo2.docstring.should == 'Docstring2'
      foo3.docstring.should == 'Docstring3'
    end

    it "should define the method inside namespace if attached to namespace object" do
      YARD.parse_string <<-eof
        module Foo
          # @!method foo
          #   Docstring1
          # @!method bar
          #   Docstring2
          class Bar
          end
        end
      eof
      Registry.at('Foo::Bar#foo').docstring.should == 'Docstring1'
      Registry.at('Foo::Bar#bar').docstring.should == 'Docstring2'
    end

    it "should set scope to class if signature has 'self.' prefix" do
      YARD.parse_string <<-eof
        # @!method self.foo
        # @!method self. bar
        # @!method self.baz()
        class Foo
        end
      eof
      %w(foo bar baz).each do |name|
        Registry.at("Foo.#{name}").should be_a(CodeObjects::MethodObject)
      end
    end

    it "should define parameters from signature" do
      YARD.parse_string <<-eof
        # @!method foo(a, b, c = nil)
      eof
      Registry.at('#foo').parameters.should == [['a', nil], ['b', nil], ['c', 'nil']]
    end

    it "should be able to define method with module scope (module function)" do
      YARD.parse_string <<-eof
        # @!method foo
        #   @!scope module
        #   This is a docstring
        #   @return [Boolean] whether this is true
        class Foo
        end
      eof
      foo_c = Registry.at('Foo.foo')
      foo_i = Registry.at('Foo#foo')
      foo_c.should_not be_nil
      foo_i.should_not be_nil
      foo_c.should be_module_function
      foo_c.docstring.should == foo_i.docstring
      foo_c.tag(:return).text.should == foo_i.tag(:return).text
    end
  end
end

describe YARD::Tags::AttributeDirective do
  describe '#call' do
    after { Registry.clear }

    it "should use entire docstring if no indented data is found" do
      YARD.parse_string <<-eof
        class Foo
          # @!attribute foo
          # @!attribute bar
          # @!scope class
        end
      eof
      Registry.at('Foo.foo').should be_reader
      Registry.at('Foo.bar').should be_reader
    end

    it "should handle indented block in @!attribute" do
      YARD.parse_string <<-eof
        # @!attribute foo
        #   Docstring here
        #   @return [String] the foo
        # Ignore this
        # @param [String] a
      eof
      foo = Registry.at('#foo')
      foo.is_attribute?.should == true
      foo.docstring.should == "Docstring here"
      foo.docstring.tag(:return).should_not be_nil
      foo.tag(:param).should be_nil
    end

    it "should be able to define multiple @attributes in docstring" do
      YARD.parse_string <<-eof
        class Foo
          # @!attribute [r] foo1
          #   Docstring1
          # @!attribute [w] foo2
          #   Docstring2
          # @!attribute foo3
          #   @!scope class
          #   Docstring3
        end
      eof
      foo1 = Registry.at('Foo#foo1')
      foo2 = Registry.at('Foo#foo2=')
      foo3 = Registry.at('Foo.foo3')
      foo4 = Registry.at('Foo.foo3=')
      foo1.should be_reader
      foo2.should be_writer
      foo3.should be_reader
      foo1.docstring.should == 'Docstring1'
      foo2.docstring.should == 'Docstring2'
      foo3.docstring.should == 'Docstring3'
      foo4.should be_writer
      foo1.attr_info[:write].should be_nil
      foo2.attr_info[:read].should be_nil
    end

    it "should define the attr inside namespace if attached to namespace object" do
      YARD.parse_string <<-eof
        module Foo
          # @!attribute [r] foo
          # @!attribute [r] bar
          class Bar
          end
        end
      eof
      Registry.at('Foo::Bar#foo').should be_reader
      Registry.at('Foo::Bar#bar').should be_reader
    end
  end

  it "should set scope to class if signature has 'self.' prefix" do
    YARD.parse_string <<-eof
      # @!attribute self.foo
      # @!attribute self. bar
      # @!attribute self.baz
      class Foo
      end
    eof
    %w(foo bar baz).each do |name|
      Registry.at("Foo.#{name}").should be_reader
    end
  end
end

describe YARD::Tags::ScopeDirective do
  describe '#call' do
    after { Registry.clear }

    it "should set state on tag parser if object = nil" do
      tag_parse("@!scope class")
      @parser.state.scope.should == :class
    end

    it "should set state on tag parser if object is namespace" do
      object = CodeObjects::ClassObject.new(:root, 'Foo')
      tag_parse("@!scope class", object)
      object[:scope].should be_nil
      @parser.state.scope.should == :class
    end

    it "should set scope on object if object is a method object" do
      object = CodeObjects::MethodObject.new(:root, 'foo')
      tag_parse("@!scope class", object)
      object.scope.should == :class
    end

    %w(class instance module).each do |type|
      it "should allow #{type} as value" do
        tag_parse("@!scope #{type}")
        @parser.state.scope.should == type.to_sym
      end
    end

    %w(invalid foo FOO CLASS INSTANCE).each do |type|
      it "should not allow #{type} as value" do
        tag_parse("@!scope #{type}")
        @parser.state.scope.should be_nil
      end
    end
  end
end

describe YARD::Tags::VisibilityDirective do
  describe '#call' do
    after { Registry.clear }

    it "should set visibility on tag parser if object = nil" do
      tag_parse("@!visibility private")
      @parser.state.visibility.should == :private
    end

    it "should set state on tag parser if object is namespace" do
      object = CodeObjects::ClassObject.new(:root, 'Foo')
      tag_parse("@!visibility protected", object)
      object.visibility.should == :protected
      @parser.state.visibility.should be_nil
    end

    it "should set visibility on object if object is a method object" do
      object = CodeObjects::MethodObject.new(:root, 'foo')
      tag_parse("@!visibility private", object)
      object.visibility.should == :private
    end

    %w(public private protected).each do |type|
      it "should allow #{type} as value" do
        tag_parse("@!visibility #{type}")
        @parser.state.visibility.should == type.to_sym
      end
    end

    %w(invalid foo FOO PRIVATE INSTANCE).each do |type|
      it "should not allow #{type} as value" do
        tag_parse("@!visibility #{type}")
        @parser.state.visibility.should be_nil
      end
    end

    it "updates visibility on future methods" do
      Registry.clear
      YARD.parse_string <<-eof
        class Foo
          # @!visibility private


          def foo; end
          def bar; end
          def baz; end
        end
      eof
      %w(foo bar baz).each do |name|
        Registry.at("Foo##{name}").visibility.should == :private
      end
    end if YARD::Parser::SourceParser.parser_type == :ruby
  end
end
