require 'spec_helper'

describe Jasmine::PathMapper do
  it "correctly remaps src files" do
    config = double(:config, :src_dir => '/src_dir', :src_path => '/__src__')
    mapper = Jasmine::PathMapper.new(config)
    mapper.map_src_paths(['/src_dir/foo']).should == ['/__src__/foo']
    mapper.map_src_paths(['foo/bar']).should == ['/__src__/foo/bar']
  end
  it "correctly remaps spec files" do
    config = double(:config, :spec_dir => '/spec_dir', :spec_path => '/__spec__')
    mapper = Jasmine::PathMapper.new(config)
    mapper.map_spec_paths(['/spec_dir/foo']).should == ['/__spec__/foo']
    mapper.map_spec_paths(['foo/bar']).should == ['/__spec__/foo/bar']
  end
  it "correctly remaps jasmine files" do
    config = double(:config, :jasmine_dir => '/jasmine_dir', :jasmine_path => '/__jasmine__')
    mapper = Jasmine::PathMapper.new(config)
    mapper.map_jasmine_paths(['/jasmine_dir/foo']).should == ['/__jasmine__/foo']
    mapper.map_jasmine_paths(['foo/bar']).should == ['/__jasmine__/foo/bar']
  end
  it "correctly remaps boot files" do
    config = double(:config, :boot_dir => '/boot_dir', :boot_path => '/__boot__')
    mapper = Jasmine::PathMapper.new(config)
    mapper.map_boot_paths(['/boot_dir/foo']).should == ['/__boot__/foo']
    mapper.map_boot_paths(['foo/bar']).should == ['/__boot__/foo/bar']
  end
  it "handles edge case where dir == path" do
    config = double(:config, :src_dir => '/src_dir', :src_path => '/src_dir')
    mapper = Jasmine::PathMapper.new(config)
    mapper.map_src_paths(['/src_dir/foo']).should == ['/src_dir/foo']
  end
end
