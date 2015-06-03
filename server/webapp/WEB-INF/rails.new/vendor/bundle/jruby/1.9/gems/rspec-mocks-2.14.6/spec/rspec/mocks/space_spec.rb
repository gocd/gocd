require 'spec_helper'

module RSpec::Mocks
  describe Space do

    describe "#proxies_of(klass)" do
      let(:space) { Space.new }

      it 'returns proxies' do
        space.proxy_for("")
        expect(space.proxies_of(String).map(&:class)).to eq([Proxy])
      end

      it 'returns only the proxies whose object is an instance of the given class' do
        grandparent_class = Class.new
        parent_class      = Class.new(grandparent_class)
        child_class       = Class.new(parent_class)

        grandparent = grandparent_class.new
        parent      = parent_class.new
        child       = child_class.new

        grandparent_proxy = space.proxy_for(grandparent)
        parent_proxy      = space.proxy_for(parent)
        child_proxy       = space.proxy_for(child)

        expect(space.proxies_of(parent_class)).to match_array([parent_proxy, child_proxy])
      end
    end

  end
end
