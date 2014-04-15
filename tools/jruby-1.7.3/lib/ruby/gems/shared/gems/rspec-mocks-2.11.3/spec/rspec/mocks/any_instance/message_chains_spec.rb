require 'spec_helper'

describe RSpec::Mocks::AnyInstance::MessageChains do
  let(:chains) { RSpec::Mocks::AnyInstance::MessageChains.new }
  let(:stub_chain) { RSpec::Mocks::AnyInstance::StubChain.new }
  let(:expectation_chain) { RSpec::Mocks::AnyInstance::PositiveExpectationChain.new }

  it "knows if a method does not have an expectation set on it" do
    chains.add(:method_name, stub_chain)
    chains.has_expectation?(:method_name).should be_false
  end

  it "knows if a method has an expectation set on it" do
    chains.add(:method_name, stub_chain)
    chains.add(:method_name, expectation_chain)
    chains.has_expectation?(:method_name).should be_true
  end

  it "can remove all stub chains" do
    chains.add(:method_name, stub_chain)
    chains.add(:method_name, expectation_chain)
    chains.add(:method_name, RSpec::Mocks::AnyInstance::StubChain.new)

    chains.remove_stub_chains_for!(:method_name)
    chains[:method_name].should eq([expectation_chain])
  end
  
  context "creating stub chains" do
    it "understands how to add a stub chain for a method" do
      chains.add(:method_name, stub_chain)
      chains[:method_name].should eq([stub_chain])
    end

    it "allows multiple stub chains for a method" do
      chains.add(:method_name, stub_chain)
      chains.add(:method_name, another_stub_chain = RSpec::Mocks::AnyInstance::StubChain.new)
      chains[:method_name].should eq([stub_chain, another_stub_chain])
    end
  end
end
