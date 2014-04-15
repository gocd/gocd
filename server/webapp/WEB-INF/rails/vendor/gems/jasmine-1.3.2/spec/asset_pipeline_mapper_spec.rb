require 'spec_helper'

describe Jasmine::AssetPipelineMapper do
  it "expands asset paths if available" do
    expander = lambda do |dir, path|
      if dir == "/some_location/" && path == 'asset1'
        ['asset1', 'asset2']
      elsif dir == "/some_location/" && path == 'asset2'
        ['asset1', 'asset3']
      end

    end
    config = double(:config, :src_dir => "/some_location/")

    mapper = Jasmine::AssetPipelineMapper.new(config, expander)

    mapper.map_src_paths(['asset1', 'asset2', 'asset4']).should == ['asset1', 'asset2', 'asset3', 'asset4']
  end
end
