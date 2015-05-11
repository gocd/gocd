require File.dirname(__FILE__) + "/spec_helper"

describe YARD::DocstringParser do
  after(:all) do
    YARD::Registry.clear
  end

  def parse(content, object = nil, handler = nil)
    @library ||= Tags::Library.instance
    @parser = DocstringParser.new(@library)
    @parser.parse(content, object, handler)
    @parser
  end

  def docstring(content, object = nil, handler = nil)
    parse(content, object, handler).to_docstring
  end

  describe '#parse' do
    it "should parse comments into tags" do
      doc = docstring(<<-eof)
@param name Hello world
  how are you?
@param name2
  this is a new line
@param name3 and this
  is a new paragraph:

  right here.
      eof
      tags = doc.tags(:param)
      tags[0].name.should == "name"
      tags[0].text.should == "Hello world\nhow are you?"
      tags[1].name.should == "name2"
      tags[1].text.should == "this is a new line"
      tags[2].name.should == "name3"
      tags[2].text.should == "and this\nis a new paragraph:\n\nright here."
    end

    it "should end parsing a tag on de-dent" do
      doc = docstring(<<-eof)
@note test
  one two three
rest of docstring
      eof
      doc.tag(:note).text.should == "test\none two three"
      doc.should == "rest of docstring"
    end

    it "should parse examples embedded in doc" do
      doc = docstring(<<-eof)
test string here
@example code

  def foo(x, y, z)
  end

  class A; end

more stuff
eof
      doc.should == "test string here\nmore stuff"
      doc.tag(:example).text.should == "\ndef foo(x, y, z)\nend\n\nclass A; end"
    end

    it "should remove only original indentation from beginning of line in tags" do
      doc = docstring(<<-eof)
@param name
  some value
  foo bar
   baz
eof
      doc.tag(:param).text.should == "some value\nfoo bar\n baz"
    end

    it "should allow numbers in tags" do
      Tags::Library.define_tag(nil, :foo1)
      Tags::Library.define_tag(nil, :foo2)
      Tags::Library.define_tag(nil, :foo3)
      doc = docstring(<<-eof)
@foo1 bar1
@foo2 bar2
@foo3 bar3
eof
      doc.tag(:foo1).text.should == "bar1"
      doc.tag(:foo2).text.should == "bar2"
    end

    it "should end tag on newline if next line is not indented" do
      doc = docstring(<<-eof)
@author bar1
@api bar2
Hello world
eof
      doc.tag(:author).text.should == "bar1"
      doc.tag(:api).text.should == "bar2"
    end

    it "should warn about unknown tag" do
      log.should_receive(:warn).with(/Unknown tag @hello$/)
      docstring("@hello world")
    end

    it "should not add trailing whitespace to freeform tags" do
      doc = docstring("@api private   \t   ")
      doc.tag(:api).text.should == "private"
    end
  end

  describe '#parse with custom tag library' do
    class TestLibrary < Tags::Library; end

    before { @library = TestLibrary.new }

    it "should accept valid tags" do
      valid = %w( testing valid is_a is_A __ )
      valid.each do |tag|
        TestLibrary.define_tag("Tag", tag)
        doc = docstring('@' + tag + ' foo bar')
        doc.tag(tag).text.should == 'foo bar'
      end
    end

    it "should not parse invalid tag names" do
      invalid = %w( @ @return@ @param, @x-y @.x.y.z )
      invalid.each do |tag|
        docstring(tag + ' foo bar').should == tag + ' foo bar'
      end
    end

    it "should allow namespaced tags in the form @x.y.z" do
      TestLibrary.define_tag("Tag", 'x.y.z')
      doc = docstring("@x.y.z foo bar")
      doc.tag('x.y.z').text.should == 'foo bar'
    end

    it "should ignore new directives without @! prefix syntax" do
      TestLibrary.define_directive('dir1', Tags::ScopeDirective)
      log.should_receive(:warn).with(/@dir1/)
      docstring("@dir1")
    end

    %w(attribute endgroup group macro method scope visibility).each do |tag|
      it "should handle non prefixed @#{tag} syntax as directive, not tag" do
        TestLibrary.define_directive(tag, Tags::ScopeDirective)
        parse("@#{tag}")
        @parser.directives.first.should be_a(Tags::ScopeDirective)
      end
    end

    it "should handle directives with @! prefix syntax" do
      TestLibrary.define_directive('dir1', Tags::ScopeDirective)
      docstring("@!dir1 class")
      @parser.state.scope.should == :class
    end
  end

  describe '#text' do
    it "should only return text data" do
      parse("Foo\n@param foo x y z\nBar")
      @parser.text.should == "Foo\nBar"
    end
  end

  describe '#raw_text' do
    it "should return the entire original data" do
      data = "Foo\n@param foo x y z\nBar"
      parse(data)
      @parser.raw_text.should == data
    end
  end

  describe '#tags' do
    it "should return the parsed tags" do
      data = "Foo\n@param foo x y z\nBar"
      parse(data)
      @parser.tags.size.should == 1
      @parser.tags.first.tag_name.should == 'param'
    end
  end

  describe '#directives' do
    it "should group all processed directives" do
      data = "Foo\n@!scope class\n@!visibility private\nBar"
      parse(data)
      dirs = @parser.directives
      dirs.size == 2
      dirs[0].should be_a(Tags::ScopeDirective)
      dirs[0].tag.text.should == 'class'
      dirs[1].should be_a(Tags::VisibilityDirective)
      dirs[1].tag.text.should == 'private'
    end
  end

  describe '#state' do
    it "should handle modified state" do
      parse("@!scope class")
      @parser.state.scope.should == :class
    end
  end

  describe 'after_parse' do
    it "should allow specifying of callbacks" do
      parser = DocstringParser.new
      the_yielded_obj = nil
      DocstringParser.after_parse {|obj| the_yielded_obj = obj }
      parser.parse("Some text")
      the_yielded_obj.should == parser
    end

    it "should warn about invalid named parameters" do
      log.should_receive(:warn).with(/@param tag has unknown parameter name: notaparam/)
      YARD.parse_string <<-eof
        # @param notaparam foo
        def foo(a) end
      eof
    end

    it "should warn about duplicate named parameters" do
      log.should_receive(:warn).with(/@param tag has duplicate parameter name: a/)
      YARD.parse_string <<-eof
        # @param a foo
        # @param a foo
        def foo(a) end
      eof
    end
  end
end
