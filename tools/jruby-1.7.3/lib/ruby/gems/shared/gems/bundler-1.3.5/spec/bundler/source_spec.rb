require 'spec_helper'

describe Bundler::Source::Rubygems do
  before do
    Bundler.stub(:root){ Pathname.new("root") }
  end

  describe "caches" do
    it "should include Bundler.app_cache" do
      expect(subject.caches).to include(Bundler.app_cache)
    end

    it "should include GEM_PATH entries" do
      Gem.path.each do |path|
        expect(subject.caches).to include(File.expand_path("#{path}/cache"))
      end
    end

    it "should be an array of strings or pathnames" do
      subject.caches.each do |cache|
        expect([String, Pathname]).to include(cache.class)
      end
    end
  end
end
