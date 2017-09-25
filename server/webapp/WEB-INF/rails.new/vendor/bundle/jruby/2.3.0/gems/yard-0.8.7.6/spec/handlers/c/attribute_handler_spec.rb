require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::AttributeHandler do
  def run(read, write, commented = nil)
    parse <<-eof
      /* FOO */
      VALUE foo(VALUE x) { int value = x; }
      void Init_Foo() {
        rb_cFoo = rb_define_class("Foo", rb_cObject);
        #{commented ? '/*' : ''}
          rb_define_attr(rb_cFoo, "foo", #{read}, #{write});
        #{commented ? '*/' : ''}
      }
    eof
  end

  it "should handle readonly attribute (rb_define_attr)" do
    run(1, 0)
    Registry.at('Foo#foo').should be_reader
    Registry.at('Foo#foo=').should be_nil
  end

  it "should handle writeonly attribute (rb_define_attr)" do
    run(0, 1)
    Registry.at('Foo#foo').should be_nil
    Registry.at('Foo#foo=').should be_writer
  end

  it "should handle readwrite attribute (rb_define_attr)" do
    run(1, 1)
    Registry.at('Foo#foo').should be_reader
    Registry.at('Foo#foo=').should be_writer
  end

  it "should handle commented writeonly attribute (/* rb_define_attr */)" do
    run(1, 1, true)
    Registry.at('Foo#foo').should be_reader
    Registry.at('Foo#foo=').should be_writer
  end
end
