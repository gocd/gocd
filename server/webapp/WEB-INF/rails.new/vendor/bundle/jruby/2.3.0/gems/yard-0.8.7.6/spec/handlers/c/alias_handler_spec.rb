require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::AliasHandler do
  it "should allow defining of aliases (rb_define_alias)" do
    parse <<-eof
      /* FOO */
      VALUE foo(VALUE x) { int value = x; }
      void Init_Foo() {
        rb_cFoo = rb_define_class("Foo", rb_cObject);
        rb_define_method(rb_cFoo, "foo", foo, 1);
        rb_define_alias(rb_cFoo, "bar", "foo");
      }
    eof

    Registry.at('Foo#bar').should be_is_alias
    Registry.at('Foo#bar').docstring.should == 'FOO'
  end

  it "should allow defining of aliases (rb_define_alias) of attributes" do
    parse <<-eof
      /* FOO */
      VALUE foo(VALUE x) { int value = x; }
      void Init_Foo() {
        rb_cFoo = rb_define_class("Foo", rb_cObject);
        rb_define_attr(rb_cFoo, "foo", 1, 0);
        rb_define_alias(rb_cFoo, "foo?", "foo");
      }
    eof

    Registry.at('Foo#foo').should be_reader
    Registry.at('Foo#foo?').should be_is_alias
  end
end
