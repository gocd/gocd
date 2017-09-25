require File.dirname(__FILE__) + '/spec_helper'

describe YARD::Options do
  class FooOptions < YARD::Options
    attr_accessor :foo
    def initialize; self.foo = "abc" end
  end

  describe '.default_attr' do
    it "should allow default attributes to be defined with symbols" do
      class DefaultOptions1 < YARD::Options
        default_attr :foo, 'HELLO'
      end
      o = DefaultOptions1.new
      o.reset_defaults
      o.foo.should == 'HELLO'
    end

    it "should call lambda if value is a Proc" do
      class DefaultOptions2 < YARD::Options
        default_attr :foo, lambda { 100 }
      end
      o = DefaultOptions2.new
      o.reset_defaults
      o.foo.should == 100
    end
  end

  describe '#reset_defaults' do
    it "should not define defaults until reset is called" do
      class ResetDefaultOptions1 < YARD::Options
        default_attr :foo, 'FOO'
      end
      ResetDefaultOptions1.new.foo.should be_nil
      o = ResetDefaultOptions1.new
      o.reset_defaults
      o.foo.should == 'FOO'
    end

    it "should use defaults from superclass as well" do
      class ResetDefaultOptions2 < YARD::Options
        default_attr :foo, 'FOO'
      end
      class ResetDefaultOptions3 < ResetDefaultOptions2
      end
      o = ResetDefaultOptions3.new
      o.reset_defaults
      o.foo.should == 'FOO'
    end
  end

  describe '#delete' do
    it "should delete an option" do
      o = FooOptions.new
      o.delete(:foo)
      o.to_hash.should == {}
    end

    it "should not error if an option is deleted that does not exist" do
      o = FooOptions.new
      o.delete(:foo)
      o.delete(:foo)
      o.to_hash.should == {}
    end
  end

  describe '#[]' do
    it "should handle getting option values using hash syntax" do
      FooOptions.new[:foo].should == "abc"
    end
  end

  describe '#[]=' do
    it "should handle setting options using hash syntax" do
      o = FooOptions.new
      o[:foo] = "xyz"
      o[:foo].should == "xyz"
    end

    it "should allow setting of unregistered keys" do
      o = FooOptions.new
      o[:bar] = "foo"
      o[:bar].should == "foo"
    end
  end

  describe '#method_missing' do
    it "should allow setting of unregistered keys" do
      o = FooOptions.new
      o.bar = 'foo'
      o.bar.should == 'foo'
    end

    it "should allow getting values of unregistered keys (return nil)" do
      FooOptions.new.bar.should be_nil
    end

    it "should print debugging messages about unregistered keys" do
      log.should_receive(:debug).with("Attempting to access unregistered key bar on FooOptions")
      FooOptions.new.bar
      log.should_receive(:debug).with("Attempting to set unregistered key bar on FooOptions")
      FooOptions.new.bar = 1
    end
  end

  describe '#update' do
    it "should allow updating of options" do
      FooOptions.new.update(:foo => "xyz").foo.should == "xyz"
    end

    it "should not ignore keys with no setter (OpenStruct behaviour)" do
      o = FooOptions.new
      o.update(:bar => "xyz")
      o.to_hash.should == {:foo => "abc", :bar => "xyz"}
    end
  end

  describe '#merge' do
    it "should update a new object" do
      o = FooOptions.new
      o.merge(:foo => "xyz").object_id.should_not == o.object_id
      o.merge(:foo => "xyz").to_hash.should == {:foo => "xyz"}
    end

    it "should add in values from original object" do
      o = FooOptions.new
      o.update(:bar => "foo")
      o.merge(:baz => 1).to_hash.should == {:foo => "abc", :bar => "foo", :baz => 1}
    end
  end

  describe '#to_hash' do
    it "should convert all instance variables and symbolized keys" do
      class ToHashOptions1 < YARD::Options
        attr_accessor :foo, :bar, :baz
        def initialize; @foo = 1; @bar = 2; @baz = "hello" end
      end
      o = ToHashOptions1.new
      hash = o.to_hash
      hash.keys.should include(:foo, :bar, :baz)
      hash[:foo].should == 1
      hash[:bar].should == 2
      hash[:baz].should == "hello"
    end

    it "should use accessor when converting values to hash" do
      class ToHashOptions2 < YARD::Options
        def initialize; @foo = 1 end
        def foo; "HELLO#{@foo}" end
      end
      o = ToHashOptions2.new
      o.to_hash.should == {:foo => "HELLO1"}
    end

    it "should ignore ivars with no accessor" do
      class ToHashOptions3 < YARD::Options
        attr_accessor :foo
        def initialize; @foo = 1; @bar = "NOIGNORE" end
      end
      o = ToHashOptions3.new
      o.to_hash.should == {:foo => 1, :bar => "NOIGNORE"}
    end
  end

  describe '#tap' do
    it "should support #tap(&block) (even in 1.8.6)" do
      o = FooOptions.new.tap {|o| o.foo = :BAR }
      o.to_hash.should == {:foo => :BAR}
    end
  end
end
