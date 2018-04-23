require 'spec_helper'

describe Jasmine do
  it "should provide the root path" do
    allow(File).to receive(:dirname).and_return('lib/jasmine')
    expect(File).to receive(:expand_path) { |path| path }
    expect(Jasmine.root).to eq 'lib/jasmine'
  end

  it "should append passed file paths" do
    allow(File).to receive(:dirname).and_return('lib/jasmine')
    expect(File).to receive(:expand_path) { |path| path }
    expect(Jasmine.root('subdir1', 'subdir2')).to eq File.join('lib/jasmine', 'subdir1', 'subdir2')
  end

  describe '#load_spec' do
    it 'assigns the spec to the spec path' do
      Jasmine.load_spec("spec/test")
      expect(Jasmine.config.spec_files.call).to eq [ "spec/test" ]
    end

    it 'does not assign a spec path if passed a nil' do
      Jasmine.load_spec("spec/test")
      Jasmine.load_spec(nil)
      expect(Jasmine.config.spec_files.call).to eq [ "spec/test" ]
    end

    it 'does not override nonspec files' do
      Jasmine.config.helper_files = lambda { ["aaa"] }
      Jasmine.load_spec("spec/test")
      expect(Jasmine.config.spec_files.call).to eq [ "spec/test" ]
      expect(Jasmine.config.helper_files.call).to eq ["aaa"]
    end
  end
end
