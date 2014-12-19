require 'spec_helper'

exit true if jruby?

require 'shared/adapter'
require 'multi_json/adapters/oj'

describe MultiJson::Adapters::Oj do
  it_behaves_like 'an adapter', described_class

  describe '.dump' do
    describe '#dump_options' do
      around{ |example| with_default_options(&example) }

      it 'ensures indent is a Fixnum' do
        expect{ MultiJson.dump(42, :indent => '')}.not_to raise_error
      end
    end
  end
end
