require "spec_helper"
require 'rspec/core/drb_options'

describe RSpec::Core::DrbOptions, :isolated_directory => true, :isolated_home => true do
  include ConfigOptionsHelper

  describe "#drb_argv" do
    it "preserves extra arguments" do
      File.stub(:exist?) { false }
      expect(config_options_object(*%w[ a --drb b --color c ]).drb_argv).to match_array %w[ --color a b c ]
    end

    %w(--color --fail-fast --profile --backtrace --tty).each do |option|
      it "includes #{option}" do
        expect(config_options_object("#{option}").drb_argv).to include("#{option}")
      end
    end

    it "includes --failure-exit-code" do
      expect(config_options_object(*%w[--failure-exit-code 2]).drb_argv).to include("--failure-exit-code", "2")
    end

    it "includes --options" do
      expect(config_options_object(*%w[--options custom.opts]).drb_argv).to include("--options", "custom.opts")
    end

    it "includes --order" do
      expect(config_options_object(*%w[--order random]).drb_argv).to include('--order', 'random')
    end

    context "with --example" do
      it "includes --example" do
        expect(config_options_object(*%w[--example foo]).drb_argv).to include("--example", "foo")
      end

      it "unescapes characters which were escaped upon storing --example originally" do
        expect(config_options_object("--example", "foo\\ bar").drb_argv).to include("--example", "foo bar")
      end
    end

    context "with tags" do
      it "includes the inclusion tags" do
        coo = config_options_object("--tag", "tag")
        expect(coo.drb_argv).to eq(["--tag", "tag"])
      end

      it "includes the inclusion tags with values" do
        coo = config_options_object("--tag", "tag:foo")
        expect(coo.drb_argv).to eq(["--tag", "tag:foo"])
      end

      it "leaves inclusion tags intact" do
        coo = config_options_object("--tag", "tag")
        coo.drb_argv
        expect(coo.filter_manager.inclusions).to eq( {:tag=>true} )
      end

      it "leaves inclusion tags with values intact" do
        coo = config_options_object("--tag", "tag:foo")
        coo.drb_argv
        expect(coo.filter_manager.inclusions).to eq( {:tag=>'foo'} )
      end

      it "includes the exclusion tags" do
        coo = config_options_object("--tag", "~tag")
        expect(coo.drb_argv).to eq(["--tag", "~tag"])
      end

      it "includes the exclusion tags with values" do
        coo = config_options_object("--tag", "~tag:foo")
        expect(coo.drb_argv).to eq(["--tag", "~tag:foo"])
      end

      it "leaves exclusion tags intact" do
        coo = config_options_object("--tag", "~tag")
        coo.drb_argv
        expect(coo.filter_manager.exclusions).to include(:tag=>true)
      end

      it "leaves exclusion tags with values intact" do
        coo = config_options_object("--tag", "~tag:foo")
        coo.drb_argv
        expect(coo.filter_manager.exclusions).to include(:tag=>'foo')
      end
    end

    context "with formatters" do
      it "includes the formatters" do
        coo = config_options_object("--format", "d")
        expect(coo.drb_argv).to eq(["--format", "d"])
      end

      it "leaves formatters intact" do
        coo = config_options_object("--format", "d")
        coo.drb_argv
        expect(coo.options[:formatters]).to eq([["d"]])
      end

      it "leaves output intact" do
        coo = config_options_object("--format", "p", "--out", "foo.txt", "--format", "d")
        coo.drb_argv
        expect(coo.options[:formatters]).to eq([["p","foo.txt"],["d"]])
      end
    end

    context "with --out" do
      it "combines with formatters" do
        coo = config_options_object(*%w[--format h --out report.html])
        expect(coo.drb_argv).to       eq(%w[--format h --out report.html])
      end
    end

    context "with --line_number" do
      it "includes --line_number" do
        expect(config_options_object(*%w[--line_number 35]).drb_argv).to eq(%w[--line_number 35])
      end

      it "includes multiple lines" do
        expect(config_options_object(*%w[-l 90 -l 4 -l 55]).drb_argv).to eq(
          %w[--line_number 90 --line_number 4 --line_number 55]
        )
      end
    end

    context "with -I libs" do
      it "includes -I" do
        expect(config_options_object(*%w[-I a_dir]).drb_argv).to eq(%w[-I a_dir])
      end

      it "includes multiple paths" do
        expect(config_options_object(*%w[-I dir_1 -I dir_2 -I dir_3]).drb_argv).to eq(
                               %w[-I dir_1 -I dir_2 -I dir_3]
        )
      end
    end

    context "with --require" do
      it "includes --require" do
        expect(config_options_object(*%w[--require a_path]).drb_argv).to eq(%w[--require a_path])
      end

      it "includes multiple paths" do
        expect(config_options_object(*%w[--require dir/ --require file.rb]).drb_argv).to eq(
                               %w[--require dir/ --require file.rb]
        )
      end
    end

    context "--drb specified in ARGV" do
      it "renders all the original arguments except --drb" do
        drb_argv = config_options_object(*%w[ --drb --color --format s --example pattern
                                              --line_number 1 --profile --backtrace -I
                                              path/a -I path/b --require path/c --require
                                              path/d]).drb_argv
        expect(drb_argv).to eq(%w[ --color --profile --backtrace --example pattern --line_number 1 --format s -I path/a -I path/b --require path/c --require path/d])
      end
    end

    context "--drb specified in the options file" do
      it "renders all the original arguments except --drb" do
        File.open("./.rspec", "w") {|f| f << "--drb --color"}
        drb_argv = config_options_object(*%w[ --tty --format s --example
                                         pattern --line_number 1 --profile
                                         --backtrace ]).drb_argv

        expect(drb_argv).to eq(%w[ --color --profile --backtrace --tty
                               --example pattern --line_number 1 --format s])
      end
    end

    context "--drb specified in ARGV and the options file" do
      it "renders all the original arguments except --drb" do
        File.open("./.rspec", "w") {|f| f << "--drb --color"}
        drb_argv = config_options_object(*%w[ --drb --format s --example
                                         pattern --line_number 1 --profile
                                         --backtrace]).drb_argv

        expect(drb_argv).to eq(%w[ --color --profile --backtrace --example pattern --line_number 1 --format s])
      end
    end

    context "--drb specified in ARGV and in as ARGV-specified --options file" do
      it "renders all the original arguments except --drb and --options" do
        File.open("./.rspec", "w") {|f| f << "--drb --color"}
        drb_argv = config_options_object(*%w[ --drb --format s --example
                                         pattern --line_number 1 --profile
                                         --backtrace]).drb_argv

        expect(drb_argv).to eq(%w[ --color --profile --backtrace --example pattern --line_number 1 --format s ])
      end
    end
  end
end
