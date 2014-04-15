require 'spec_helper'

describe 'bundle install with gem sources' do
  describe 'when gems include post install messages' do
    it "should display the post-install messages after installing" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
        gem 'thin'
        gem 'rack-obama'
      G

      bundle :install
      expect(out).to include("Post-install message from rack:")
      expect(out).to include("Rack's post install message")
      expect(out).to include("Post-install message from thin:")
      expect(out).to include("Thin's post install message")
      expect(out).to include("Post-install message from rack-obama:")
      expect(out).to include("Rack-obama's post install message")
    end
  end

  describe 'when gems do not include post install messages' do
    it "should not display any post-install messages" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activesupport"
      G

      bundle :install
      expect(out).not_to include("Post-install message")
    end
  end

  describe "when a dependecy includes a post install message" do
    it "should display the post install message" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack_middleware'
      G

      bundle :install
      expect(out).to include("Post-install message from rack:")
      expect(out).to include("Rack's post install message")
    end
  end
end
