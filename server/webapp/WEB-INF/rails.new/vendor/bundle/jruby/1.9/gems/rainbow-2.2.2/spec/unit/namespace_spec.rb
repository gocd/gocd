require 'spec_helper'
require 'rainbow'

describe Sickill::Rainbow do

  describe '.enabled' do
    before do
      ::Rainbow.enabled = :nope
    end

    it 'returns ::Rainbow.enabled' do
      expect(Sickill::Rainbow.enabled).to eq(:nope)
    end
  end

  describe '.enabled=' do
    before do
      allow(STDERR).to receive(:puts)
      Sickill::Rainbow.enabled = :yep
    end

    it 'sets ::Rainbow.enabled=' do
      expect(::Rainbow.enabled).to eq(:yep)
    end

    it 'prints the deprecation notice' do
      expect(STDERR).to have_received(:puts)
    end
  end

end
