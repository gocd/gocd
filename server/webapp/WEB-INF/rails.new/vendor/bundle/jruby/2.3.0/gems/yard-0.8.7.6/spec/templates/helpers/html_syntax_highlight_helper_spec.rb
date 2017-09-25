require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::Templates::Helpers::HtmlSyntaxHighlightHelper do
  include YARD::Templates::Helpers::HtmlHelper
  include YARD::Templates::Helpers::HtmlSyntaxHighlightHelper

  describe '#html_syntax_highlight' do
    before do
      stub!(:object).and_return Registry.root
      Registry.root.source_type = :ruby
    end

    it "should not highlight source if options.highlight is false" do
      should_receive(:options).and_return(Options.new.update(:highlight => false))
      html_syntax_highlight("def x\nend").should == "def x\nend"
    end

    it "should highlight source (legacy)" do
      type = Parser::SourceParser.parser_type
      Parser::SourceParser.parser_type = :ruby18
      should_receive(:options).and_return(Options.new.update(:highlight => true))
      expect = "<span class='rubyid_def def kw'>def</span><span class='rubyid_x identifier id'>x</span>
        <span class='string val'>'x'</span><span class='plus op'>+</span>
        <span class='regexp val'>/x/i</span><span class='rubyid_end end kw'>end</span>"
      result = html_syntax_highlight("def x\n  'x' + /x/i\nend")
      html_equals_string(result, expect)
      Parser::SourceParser.parser_type = type
    end

    it "should highlight source (ripper)" do
      should_receive(:options).and_return(Options.new.update(:highlight => true))
      Parser::SourceParser.parser_type = :ruby
      expect = "<span class='kw'>def</span> <span class='id identifier rubyid_x'>x</span>
        <span class='tstring'><span class='tstring_beg'>'</span>
        <span class='tstring_content'>x</span><span class='tstring_end'>'</span>
        </span> <span class='op'>+</span> <span class='tstring'>
        <span class='regexp_beg'>/</span><span class='tstring_content'>x</span>
        <span class='regexp_end'>/i</span></span>\n<span class='kw'>end</span>"
      result = html_syntax_highlight("def x\n  'x' + /x/i\nend")
      html_equals_string(result, expect)
    end if HAVE_RIPPER

    it "should return escaped unhighlighted source if a syntax error is found (ripper)" do
      should_receive(:options).and_return(Options.new.update(:highlight => true))
      html_syntax_highlight("def &x; ... end").should == "def &amp;x; ... end"
    end if HAVE_RIPPER

    it "should return escaped unhighlighted source if a syntax error is found (ripper)" do
      should_receive(:options).and_return(Options.new.update(:highlight => true))
      html_syntax_highlight("$ git clone http://url").should == "$ git clone http://url"
    end if HAVE_RIPPER
  end
end