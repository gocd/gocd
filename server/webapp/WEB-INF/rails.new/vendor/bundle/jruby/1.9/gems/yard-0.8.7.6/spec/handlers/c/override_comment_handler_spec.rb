require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::OverrideCommentHandler do
  [:class, :module].each do |type|
    it "should handle Document-#{type}" do
      parse(<<-eof)
        void something;
        /* Document-#{type}: A
         * Foo bar baz
         */
        void
      eof
      Registry.at('A').type.should == type
      Registry.at('A').docstring.should == 'Foo bar baz'
      Registry.at('A').file.should == '(stdin)'
      Registry.at('A').line.should == 2
    end
  end

  it "should handle multiple class/module combinations" do
    parse(<<-eof)
      /* Document-class: A
       * Document-class: B
       * Document-module: C
       * Foo bar baz
       */
    eof
    Registry.at('A').docstring.should == 'Foo bar baz'
    Registry.at('B').docstring.should == 'Foo bar baz'
    Registry.at('C').docstring.should == 'Foo bar baz'
    Registry.at('C').type == :module
  end

  it "should handle Document-class with inheritance" do
    parse(<<-eof)
      /* Document-class: A < B
       * Foo bar baz
       */
      void
    eof
    obj = Registry.at('A')
    obj.type.should == :class
    obj.docstring.should == 'Foo bar baz'
    obj.superclass.should == P('B')
  end
end
