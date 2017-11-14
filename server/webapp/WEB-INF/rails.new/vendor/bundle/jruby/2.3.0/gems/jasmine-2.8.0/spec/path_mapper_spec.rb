require 'spec_helper'

describe Jasmine::PathMapper do
  it "correctly remaps src files" do
    config = double(:config, :src_dir => '/src_dir', :src_path => '/__src__')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_src_paths(['/src_dir/foo'])).to eq ['/__src__/foo']
    expect(mapper.map_src_paths(['foo/bar'])).to eq ['/__src__/foo/bar']
  end
  it "correctly remaps spec files" do
    config = double(:config, :spec_dir => '/spec_dir', :spec_path => '/__spec__')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_spec_paths(['/spec_dir/foo'])).to eq ['/__spec__/foo']
    expect(mapper.map_spec_paths(['foo/bar'])).to eq ['/__spec__/foo/bar']
  end
  it "correctly remaps jasmine files" do
    config = double(:config, :jasmine_dir => '/jasmine_dir', :jasmine_path => '/__jasmine__')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_jasmine_paths(['/jasmine_dir/foo'])).to eq ['/__jasmine__/foo']
    expect(mapper.map_jasmine_paths(['foo/bar'])).to eq ['/__jasmine__/foo/bar']
  end
  it "correctly remaps boot files" do
    config = double(:config, :boot_dir => '/boot_dir', :boot_path => '/__boot__')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_boot_paths(['/boot_dir/foo'])).to eq ['/__boot__/foo']
    expect(mapper.map_boot_paths(['foo/bar'])).to eq ['/__boot__/foo/bar']
  end
  it "handles edge case where dir == path" do
    config = double(:config, :src_dir => '/src_dir', :src_path => '/src_dir')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_src_paths(['/src_dir/foo'])).to eq ['/src_dir/foo']
  end
  it 'handles edge case with multiple instances of src dir' do
    config = double(:config, :src_dir => '/app', :src_path => '/')
    mapper = Jasmine::PathMapper.new(config)
    expect(mapper.map_src_paths(['/app/assets/application.js'])).to eq ['/assets/application.js']
  end
end
