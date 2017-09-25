require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::ConstantHandler do
  it "should register constants" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_const(mFoo, "FOO", ID2SYM(100));
    eof
    Registry.at('Foo::FOO').type.should == :constant
  end

  it "should look for override comments" do
    parse <<-eof
      /* Document-const: FOO
       * Document-const: Foo::BAR
       * Foo bar!
       */

      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_const(mFoo, "FOO", ID2SYM(100));
        rb_define_const(mFoo, "BAR", ID2SYM(101));
      }
    eof
    foo = Registry.at('Foo::FOO')
    foo.type.should == :constant
    foo.docstring.should == 'Foo bar!'
    foo.value.should == 'ID2SYM(100)'
    foo.file.should == '(stdin)'
    foo.line.should == 8
    bar = Registry.at('Foo::BAR')
    bar.type.should == :constant
    bar.docstring.should == 'Foo bar!'
    bar.file.should == '(stdin)'
    bar.line.should == 9
    bar.value.should == 'ID2SYM(101)'
  end

  it "should use comment attached to declaration as fallback" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_const(mFoo, "FOO", ID2SYM(100)); // foobar!
    eof
    foo = Registry.at('Foo::FOO')
    foo.value.should == 'ID2SYM(100)'
    foo.docstring.should == 'foobar!'
  end

  it "should allow the form VALUE: DOCSTRING to document value" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_const(mFoo, "FOO", ID2SYM(100)); // 100: foobar!
    eof
    foo = Registry.at('Foo::FOO')
    foo.value.should == '100'
    foo.docstring.should == 'foobar!'
  end

  it "should allow escaping of backslashes in VALUE: DOCSTRING syntax" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_const(mFoo, "FOO", ID2SYM(100)); // 100\\:x\\:y: foobar:x!
    eof
    foo = Registry.at('Foo::FOO')
    foo.value.should == '100:x:y'
    foo.docstring.should == 'foobar:x!'
  end
end