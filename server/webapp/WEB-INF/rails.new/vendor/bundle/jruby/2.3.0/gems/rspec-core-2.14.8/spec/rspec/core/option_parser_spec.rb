require "spec_helper"

module RSpec::Core
  describe OptionParser do
    let(:output_file){ mock File }

    before do
      RSpec.stub(:deprecate)
      File.stub(:open).with("foo.txt",'w') { (output_file) }
    end

    it "does not parse empty args" do
      parser = Parser.new
      OptionParser.should_not_receive(:new)
      parser.parse!([])
    end

    it "proposes you to use --help and returns an error on incorrect argument" do
      parser = Parser.new
      option = "--my_wrong_arg"

      parser.should_receive(:abort) do |msg|
        expect(msg).to include('use --help', option)
      end

      parser.parse!([option])
    end

    describe "--formatter" do
      it "is deprecated" do
        RSpec.should_receive(:deprecate)
        Parser.parse!(%w[--formatter doc])
      end

      it "gets converted to --format" do
        options = Parser.parse!(%w[--formatter doc])
        expect(options[:formatters].first).to eq(["doc"])
      end
    end

    describe "--default_path" do
      it "gets converted to --default-path" do
        options = Parser.parse!(%w[--default_path foo])
        expect(options[:default_path]).to eq "foo"
      end
    end

    describe "--line_number" do
      it "gets converted to --line-number" do
        options = Parser.parse!(%w[--line_number 3])
        expect(options[:line_numbers]).to eq ["3"]
      end
    end


    describe "--default-path" do
      it "sets the default path where RSpec looks for examples" do
        options = Parser.parse!(%w[--default-path foo])
        expect(options[:default_path]).to eq "foo"
      end
    end

    %w[--line-number -l].each do |option|
      describe option do
        it "sets the line number of an example to run" do
          options = Parser.parse!([option, "3"])
          expect(options[:line_numbers]).to eq ["3"]
        end
      end
    end

    %w[--format -f].each do |option|
      describe option do
        it "defines the formatter" do
          options = Parser.parse!([option, 'doc'])
          expect(options[:formatters].first).to eq(["doc"])
        end
      end
    end

    %w[--out -o].each do |option|
      describe option do
        let(:options) { Parser.parse!([option, 'out.txt']) }

        it "sets the output stream for the formatter" do
          expect(options[:formatters].last).to eq(['progress', 'out.txt'])
        end

        context "with multiple formatters" do
          context "after last formatter" do
            it "sets the output stream for the last formatter" do
              options = Parser.parse!(['-f', 'progress', '-f', 'doc', option, 'out.txt'])
              expect(options[:formatters][0]).to eq(['progress'])
              expect(options[:formatters][1]).to eq(['doc', 'out.txt'])
            end
          end

          context "after first formatter" do
            it "sets the output stream for the first formatter" do
              options = Parser.parse!(['-f', 'progress', option, 'out.txt', '-f', 'doc'])
              expect(options[:formatters][0]).to eq(['progress', 'out.txt'])
              expect(options[:formatters][1]).to eq(['doc'])
            end
          end
        end
      end
    end

    %w[--example -e].each do |option|
      describe option do
        it "escapes the arg" do
          options = Parser.parse!([option, "this (and that)"])
          expect(options[:full_description].length).to eq(1)
          expect("this (and that)").to match(options[:full_description].first)
        end
      end
    end

    %w[--pattern -P].each do |option|
      describe option do
        it "sets the filename pattern" do
          options = Parser.parse!([option, 'spec/**/*.spec'])
          expect(options[:pattern]).to eq('spec/**/*.spec')
        end
      end
    end

    %w[--tag -t].each do |option|
      describe option do
        context "without ~" do
          it "treats no value as true" do
            options = Parser.parse!([option, 'foo'])
            expect(options[:inclusion_filter]).to eq(:foo => true)
          end

          it "treats 'true' as true" do
            options = Parser.parse!([option, 'foo:true'])
            expect(options[:inclusion_filter]).to eq(:foo => true)
          end

          it "treats 'nil' as nil" do
            options = Parser.parse!([option, 'foo:nil'])
            expect(options[:inclusion_filter]).to eq(:foo => nil)
          end

          it "treats 'false' as false" do
            options = Parser.parse!([option, 'foo:false'])
            expect(options[:inclusion_filter]).to eq(:foo => false)
          end

          it "merges muliple invocations" do
            options = Parser.parse!([option, 'foo:false', option, 'bar:true', option, 'foo:true'])
            expect(options[:inclusion_filter]).to eq(:foo => true, :bar => true)
          end

          it "treats 'any_string' as 'any_string'" do
            options = Parser.parse!([option, 'foo:any_string'])
            expect(options[:inclusion_filter]).to eq(:foo => 'any_string')
          end
        end

        context "with ~" do
          it "treats no value as true" do
            options = Parser.parse!([option, '~foo'])
            expect(options[:exclusion_filter]).to eq(:foo => true)
          end

          it "treats 'true' as true" do
            options = Parser.parse!([option, '~foo:true'])
            expect(options[:exclusion_filter]).to eq(:foo => true)
          end

          it "treats 'nil' as nil" do
            options = Parser.parse!([option, '~foo:nil'])
            expect(options[:exclusion_filter]).to eq(:foo => nil)
          end

          it "treats 'false' as false" do
            options = Parser.parse!([option, '~foo:false'])
            expect(options[:exclusion_filter]).to eq(:foo => false)
          end
        end
      end
    end

    describe "--order" do
      it "is nil by default" do
        expect(Parser.parse!([])[:order]).to be_nil
      end

      %w[rand random].each do |option|
        context "with #{option}" do
          it "defines the order as random" do
            options = Parser.parse!(['--order', option])
            expect(options[:order]).to eq(option)
          end
        end
      end
    end

    describe "--seed" do
      it "sets the order to rand:SEED" do
        options = Parser.parse!(%w[--seed 123])
        expect(options[:order]).to eq("rand:123")
      end
    end

    describe '--profile' do
      it 'sets profile_examples to true by default' do
        options = Parser.parse!(%w[--profile])
        expect(options[:profile_examples]).to eq true
      end

      it 'sets profile_examples to supplied int' do
        options = Parser.parse!(%w[--profile 10])
        expect(options[:profile_examples]).to eq 10
      end

      it 'sets profile_examples to true when accidentally combined with path' do
        allow(Kernel).to receive(:warn)
        options = Parser.parse!(%w[--profile some/path])
        expect(options[:profile_examples]).to eq true
      end

      it 'warns when accidentally combined with path' do
        expect(Kernel).to receive(:warn) do |msg|
          expect(msg).to match "Non integer specified as profile count"
        end
        options = Parser.parse!(%w[--profile some/path])
        expect(options[:profile_examples]).to eq true
      end
    end

    describe '--warning' do
      it 'enables warnings' do
        options = Parser.parse!(%w[--warnings])
        expect(options[:warnings]).to eq true
      end
    end

  end
end
