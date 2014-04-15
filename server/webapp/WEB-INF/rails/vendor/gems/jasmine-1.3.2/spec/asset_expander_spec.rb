require 'spec_helper'

describe Jasmine::AssetExpander do
  it "expands asset files" do
    bundled_asset = double(:bundled_asset,
                           :to_a => ['asset1', 'asset2'],
                           :pathname => double(:pathname, :to_s => '/some_src_dir/asset_file'))

    bundled_asset_getter = lambda do |filepath, ext|
      if filepath == 'asset_file' && ext == 'js'
        bundled_asset
      end
    end

    asset_path_getter = lambda do |asset|
      if asset == 'asset1'
        'asset1_path'
      elsif asset == 'asset2'
        'asset2_path'
      end
    end

    expander = Jasmine::AssetExpander.new(bundled_asset_getter, asset_path_getter)
    expanded_assets = expander.expand('/some_src_dir', 'asset_file')
    expanded_assets.should == ['/asset1_path?body=true',
                               '/asset2_path?body=true']
  end

  it "return nil if no bundled asset is found" do
    bundled_asset = nil
    bundled_asset_getter = lambda do |filepath, ext|
      if filepath == 'asset_file' && ext == 'js'
        bundled_asset
      end
    end

    expander = Jasmine::AssetExpander.new(bundled_asset_getter, lambda {})
    expanded_assets = expander.expand('/some_src_dir', 'asset_file')
    expanded_assets.should be_nil
  end
end
