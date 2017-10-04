require 'spec_helper'

describe "using rspec-mocks constructs in before(:all)" do
  deprecations = []

  def in_rspec_singleton_class(&block)
    klass = class << ::RSpec; self; end
    klass.class_eval(&block)
  end

  before(:all) do
    in_rspec_singleton_class do
      alias old_deprecate deprecate
      undef deprecate
      define_method(:deprecate) { |*args| deprecations << args.first }
    end
  end

  after(:all) do
    in_rspec_singleton_class do
      undef deprecate
      alias deprecate old_deprecate
      undef old_deprecate
    end
  end

  describe "a method stub" do
    before(:all) do
      deprecations.clear
      Object.stub(:foo) { 13 }
    end

    it 'works in examples and prints a deprecation' do
      expect(Object.foo).to eq(13)
      expect(deprecations).to include(match(/outside the per-test lifecycle/))
    end
  end

  describe "an any_instance stub" do
    before(:all) do
      deprecations.clear
      Object.any_instance.stub(:foo => 13)
    end

    it 'works in examples and prints a deprecation' do
      expect(Object.new.foo).to eq(13)
      expect(deprecations).to include(match(/outside the per-test lifecycle/))
    end
  end

  describe "constant stubbing" do
    before(:all) do
      deprecations.clear
      RSpec::Mocks::ConstantMutator.stub("Foo23", 23)
    end

    it 'works in examples and prints a deprecation' do
      expect(Foo23).to eq(23)
      expect(deprecations).to include(match(/outside the per-test lifecycle/))
    end
  end

  describe "constant hiding" do
    before(:all) do
      deprecations.clear
      RSpec::Mocks::ConstantMutator.hide("SomeClass")
    end

    it 'works in examples and prints a deprecation' do
      expect(deprecations).to include(match(/outside the per-test lifecycle/))
    end
  end
end

