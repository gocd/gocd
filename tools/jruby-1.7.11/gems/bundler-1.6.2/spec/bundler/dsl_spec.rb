require 'spec_helper'

describe Bundler::Dsl do
  before do
    @rubygems = double("rubygems")
    allow(Bundler::Source::Rubygems).to receive(:new){ @rubygems }
  end

  describe "#register_host" do
    it "registers custom hosts" do
      subject.git_source(:example){ |repo_name| "git@git.example.com:#{repo_name}.git" }
      subject.git_source(:foobar){ |repo_name| "git@foobar.com:#{repo_name}.git" }
      subject.gem("dobry-pies", :example => "strzalek/dobry-pies")
      example_uri = "git@git.example.com:strzalek/dobry-pies.git"
      expect(subject.dependencies.first.source.uri).to eq(example_uri)
    end

    it "raises expection on invalid hostname" do
      expect {
        subject.git_source(:group){ |repo_name| "git@git.example.com:#{repo_name}.git" }
      }.to raise_error(Bundler::InvalidOption)
    end

    it "expects block passed" do
      expect{ subject.git_source(:example) }.to raise_error(Bundler::InvalidOption)
    end

    context "default hosts (git, gist)" do
      it "converts :github to :git" do
        subject.gem("sparks", :github => "indirect/sparks")
        github_uri = "git://github.com/indirect/sparks.git"
        expect(subject.dependencies.first.source.uri).to eq(github_uri)
      end

      it "converts numeric :gist to :git" do
        subject.gem("not-really-a-gem", :gist => 2859988)
        github_uri = "https://gist.github.com/2859988.git"
        expect(subject.dependencies.first.source.uri).to eq(github_uri)
      end

      it "converts :gist to :git" do
        subject.gem("not-really-a-gem", :gist => "2859988")
        github_uri = "https://gist.github.com/2859988.git"
        expect(subject.dependencies.first.source.uri).to eq(github_uri)
      end

      it "converts 'rails' to 'rails/rails'" do
        subject.gem("rails", :github => "rails")
        github_uri = "git://github.com/rails/rails.git"
        expect(subject.dependencies.first.source.uri).to eq(github_uri)
      end
    end
  end

  describe "#method_missing" do
    it "raises an error for unknown DSL methods" do
      expect(Bundler).to receive(:read_file).with("Gemfile").
        and_return("unknown")

      error_msg = "Undefined local variable or method `unknown'" \
        " for Gemfile\\s+from Gemfile:1"
      expect { subject.eval_gemfile("Gemfile") }.
        to raise_error(Bundler::GemfileError, Regexp.new(error_msg))
    end
  end

  describe "#eval_gemfile" do
    it "handles syntax errors with a useful message" do
      expect(Bundler).to receive(:read_file).with("Gemfile").and_return("}")
      expect { subject.eval_gemfile("Gemfile") }.
        to raise_error(Bundler::GemfileError, /Gemfile syntax error/)
    end
  end

  describe "syntax errors" do
    it "will raise a Bundler::GemfileError" do
      gemfile "gem 'foo', :path => /unquoted/string/syntax/error"
      expect { Bundler::Dsl.evaluate(bundled_app("Gemfile"), nil, true) }.
        to raise_error(Bundler::GemfileError, /Gemfile syntax error/)
    end
  end
end
