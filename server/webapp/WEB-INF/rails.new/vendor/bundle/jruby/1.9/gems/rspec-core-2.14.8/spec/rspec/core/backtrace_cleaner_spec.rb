require "spec_helper"

module RSpec::Core
  describe BacktraceCleaner do
    context "with no patterns" do
      it "keeps all lines" do
        lines = ["/tmp/a_file", "some_random_text", "hello\330\271!"]
        cleaner = BacktraceCleaner.new([], [])
        expect(lines.all? {|line| cleaner.exclude? line}).to be_false
      end

      it 'is considered a full backtrace' do
        expect(BacktraceCleaner.new([], []).full_backtrace?).to be_true
      end
    end

    context "with an exclusion pattern but no inclusion patterns" do
      it "excludes lines that match the exclusion pattern" do
        cleaner = BacktraceCleaner.new([], [/remove/])
        expect(cleaner.exclude? "remove me").to be_true
      end

      it "keeps lines that do not match the exclusion pattern" do
        cleaner = BacktraceCleaner.new([], [/remove/])
        expect(cleaner.exclude? "apple").to be_false
      end

      it 'is considered a partial backtrace' do
        expect(BacktraceCleaner.new([], [/remove/]).full_backtrace?).to be_false
      end
    end

    context "with an exclusion pattern and an inclusion pattern" do
      it "excludes lines that match the exclusion pattern but not the inclusion pattern" do
        cleaner = BacktraceCleaner.new([/keep/], [/discard/])
        expect(cleaner.exclude? "discard").to be_true
      end

      it "keeps lines that match the inclusion pattern and the exclusion pattern" do
        cleaner = BacktraceCleaner.new([/hi/], [/.*/])
        expect(cleaner.exclude? "hi").to be_false
      end

      it "keeps lines that match neither pattern" do
        cleaner = BacktraceCleaner.new([/hi/], [/delete/])
        expect(cleaner.exclude? "fish").to be_false
      end

      it 'is considered a partial backtrace' do
        expect(BacktraceCleaner.new([], [/remove/]).full_backtrace?).to be_false
      end
    end

    context "with an exclusion pattern that matches the current working directory" do
      it "defaults to having one inclusion pattern, the current working directory" do
        cleaner = BacktraceCleaner.new(nil, [/.*/])
        expect(Dir.getwd =~ cleaner.inclusion_patterns.first).to be_true
      end
    end

    context "with an exclusion pattern that does not match the current working directory" do
      it "defaults to having no exclusion patterns" do
        cleaner = BacktraceCleaner.new(nil, [/i_wont_match_a_directory/])
        expect(cleaner.inclusion_patterns.length).to be_zero
      end
    end
  end
end
