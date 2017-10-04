require 'spec_helper'

module RSpec::Core::SharedExampleGroup
  describe Collection do

    # this represents:
    #
    # shared_examples "top level group"
    #
    # context do
    #   shared_examples "nested level one"
    # end
    #
    # context do
    #   shared_examples "nested level two"
    # end
    #
    let(:examples) do
      Hash.new { |hash,k| hash[k] = Hash.new }.tap do |hash|
        hash["main"]     = { "top level group"  => example_1 }
        hash["nested 1"] = { "nested level one" => example_2 }
        hash["nested 2"] = { "nested level two" => example_3 }
      end
    end
    (1..3).each { |num| let("example_#{num}") { double "example #{num}" } }

    context 'setup with one source, which is the top level' do

      let(:collection) { Collection.new ['main'], examples }

      it 'fetches examples from the top level' do
        expect(collection['top level group']).to eq example_1
      end

      it 'fetches examples across the nested context' do
        RSpec.stub(:warn_deprecation)
        expect(collection['nested level two']).to eq example_3
      end

      it 'warns about deprecation when you fetch across nested contexts' do
        RSpec.should_receive(:warn_deprecation)
        collection['nested level two']
      end
    end

    context 'setup with multiple sources' do

      let(:collection) { Collection.new ['main','nested 1'], examples }

      it 'fetches examples from the context' do
        expect(collection['nested level one']).to eq example_2
      end

      it 'fetches examples from main' do
        expect(collection['top level group']).to eq example_1
      end

      it 'fetches examples across the nested context' do
        RSpec.stub(:warn_deprecation)
        expect(collection['nested level two']).to eq example_3
      end

      it 'warns about deprecation when you fetch across nested contexts' do
        RSpec.should_receive(:warn_deprecation)
        collection['nested level two']
      end

    end
  end
end
