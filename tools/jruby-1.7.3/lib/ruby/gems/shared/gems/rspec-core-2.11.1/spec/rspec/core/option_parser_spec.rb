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

    describe "--formatter" do
      it "is deprecated" do
        RSpec.should_receive(:deprecate)
        Parser.parse!(%w[--formatter doc])
      end

      it "gets converted to --format" do
        options = Parser.parse!(%w[--formatter doc])
        options[:formatters].first.should eq(["doc"])
      end
    end

    %w[--format -f].each do |option|
      describe option do
        it "defines the formatter" do
          options = Parser.parse!([option, 'doc'])
          options[:formatters].first.should eq(["doc"])
        end
      end
    end

    %w[--out -o].each do |option|
      describe option do
        let(:options) { Parser.parse!([option, 'out.txt']) }

        it "sets the output stream for the formatter" do
          options[:formatters].last.should eq(['progress', 'out.txt'])
        end

        context "with multiple formatters" do
          context "after last formatter" do
            it "sets the output stream for the last formatter" do
              options = Parser.parse!(['-f', 'progress', '-f', 'doc', option, 'out.txt'])
              options[:formatters][0].should eq(['progress'])
              options[:formatters][1].should eq(['doc', 'out.txt'])
            end
          end

          context "after first formatter" do
            it "sets the output stream for the first formatter" do
              options = Parser.parse!(['-f', 'progress', option, 'out.txt', '-f', 'doc'])
              options[:formatters][0].should eq(['progress', 'out.txt'])
              options[:formatters][1].should eq(['doc'])
            end
          end
        end
      end
    end

    %w[--example -e].each do |option|
      describe option do
        it "escapes the arg" do
          options = Parser.parse!([option, "this (and that)"])
          options[:full_description].length.should eq(1)
          "this (and that)".should match(options[:full_description].first)
        end
      end
    end

    %w[--pattern -P].each do |option|
      describe option do
        it "sets the filename pattern" do
          options = Parser.parse!([option, 'spec/**/*.spec'])
          options[:pattern].should eq('spec/**/*.spec')
        end
      end
    end

    %w[--tag -t].each do |option|
      describe option do
        context "without ~" do
          it "treats no value as true" do
            options = Parser.parse!([option, 'foo'])
            options[:inclusion_filter].should eq(:foo => true)
          end

          it "treats 'true' as true" do
            options = Parser.parse!([option, 'foo:true'])
            options[:inclusion_filter].should eq(:foo => true)
          end

          it "treats 'nil' as nil" do
            options = Parser.parse!([option, 'foo:nil'])
            options[:inclusion_filter].should eq(:foo => nil)
          end

          it "treats 'false' as false" do
            options = Parser.parse!([option, 'foo:false'])
            options[:inclusion_filter].should eq(:foo => false)
          end

          it "merges muliple invocations" do
            options = Parser.parse!([option, 'foo:false', option, 'bar:true', option, 'foo:true'])
            options[:inclusion_filter].should eq(:foo => true, :bar => true)
          end

          it "treats 'any_string' as 'any_string'" do
            options = Parser.parse!([option, 'foo:any_string'])
            options[:inclusion_filter].should eq(:foo => 'any_string')
          end
        end

        context "with ~" do
          it "treats no value as true" do
            options = Parser.parse!([option, '~foo'])
            options[:exclusion_filter].should eq(:foo => true)
          end

          it "treats 'true' as true" do
            options = Parser.parse!([option, '~foo:true'])
            options[:exclusion_filter].should eq(:foo => true)
          end

          it "treats 'nil' as nil" do
            options = Parser.parse!([option, '~foo:nil'])
            options[:exclusion_filter].should eq(:foo => nil)
          end

          it "treats 'false' as false" do
            options = Parser.parse!([option, '~foo:false'])
            options[:exclusion_filter].should eq(:foo => false)
          end
        end
      end
    end

    describe "--order" do
      it "is nil by default" do
        Parser.parse!([])[:order].should be_nil
      end

      %w[rand random].each do |option|
        context "with #{option}" do
          it "defines the order as random" do
            options = Parser.parse!(['--order', option])
            options[:order].should eq(option)
          end
        end
      end
    end

    describe "--seed" do
      it "sets the order to rand:SEED" do
        options = Parser.parse!(%w[--seed 123])
        options[:order].should eq("rand:123")
      end
    end
  end
end
