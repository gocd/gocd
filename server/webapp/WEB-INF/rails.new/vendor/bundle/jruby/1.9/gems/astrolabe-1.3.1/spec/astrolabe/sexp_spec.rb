# coding: utf-8

require 'astrolabe/sexp'

module Astrolabe
  describe Sexp do
    include Sexp

    describe '.s' do
      subject(:node) { s(:lvar, :foo) }

      it 'returns an instance of Astrolabe::Node' do
        expect(node).to be_a(::Astrolabe::Node)
      end

      it 'sets the passed type to the node' do
        expect(node.type).to eq(:lvar)
      end

      it 'sets the passed children to the node' do
        expect(node.children).to eq([:foo])
      end
    end
  end
end
