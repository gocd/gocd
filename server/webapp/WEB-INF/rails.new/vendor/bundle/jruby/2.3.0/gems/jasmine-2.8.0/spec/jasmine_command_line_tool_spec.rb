require 'spec_helper'

describe 'Jasmine command line tool' do
  before :each do
    temp_dir_before
    Dir::chdir @tmp
  end

  after :each do
    temp_dir_after
  end

  describe '.init' do
    describe 'without a Gemfile' do
      it 'should create files on init' do
        output = capture_stdout { Jasmine::CommandLineTool.new.process ['init'] }
        expect(output).to match /Jasmine has been installed\./

        expect(File.exists?(File.join(@tmp, 'spec/javascripts/helpers/.gitkeep'))).to eq true
        expect(File.exists?(File.join(@tmp, 'spec/javascripts/support/jasmine.yml'))).to eq true
        expect(File.exists?(File.join(@tmp, 'Rakefile'))).to eq true
        ci_output = `rake --trace jasmine:ci`
        expect(ci_output).to match (/0 specs, 0 failures/)
      end

      it 'should not have rails-like paths' do
        output = capture_stdout { Jasmine::CommandLineTool.new.process ['init'] }
        expect(output).to match /Jasmine has been installed\./

        config = YAML.load_file(File.join(@tmp, 'spec/javascripts/support/jasmine.yml'))
        expect(config['src_files']).to eq ['public/javascripts/**/*.js']
        expect(config['stylesheets']).to eq ['stylesheets/**/*.css']
      end

      it 'should create a new Rakefile if it does not exist' do
        output = capture_stdout { Jasmine::CommandLineTool.new.process ["init"] }
        expect(output).to match /Jasmine has been installed\./
        expect(File.read(File.join(@tmp, 'Rakefile'))).to include('jasmine.rake')
      end

      it "should append to an existing Rakefile" do
        FileUtils.cp("#{@old_dir}/spec/fixture/Rakefile", @tmp)
        output = capture_stdout { Jasmine::CommandLineTool.new.process ["init"] }
        expect(output).to match /Jasmine has been installed\./
        expect(File.read(File.join(@tmp, 'Rakefile'))).to include('jasmine_flunk')
      end
    end

    describe 'with a Gemfile containing Rails' do
      before :each do
        open(File.join(@tmp, "Gemfile"), 'w') do |f|
          f.puts "rails"
        end
      end

      it 'should warn the user' do
        output = capture_stdout {
          expect {
            Jasmine::CommandLineTool.new.process ['init']
          }.to raise_error SystemExit
        }
        expect(output).to match /attempting to run jasmine init in a Rails project/

        expect(Dir.entries(@tmp).sort).to eq [".", "..", "Gemfile"]
      end

      it 'should allow the user to override the warning' do
        output = capture_stdout {
          expect {
            Jasmine::CommandLineTool.new.process ['init', '--force']
          }.not_to raise_error
        }
        expect(output).to match /Jasmine has been installed\./

        expect(File.exists?(File.join(@tmp, 'spec/javascripts/helpers/.gitkeep'))).to eq true
        expect(File.exists?(File.join(@tmp, 'spec/javascripts/support/jasmine.yml'))).to eq true
      end
    end

    describe 'with a Gemfile not containing Rails' do
      before :each do
        open(File.join(@tmp, "Gemfile"), 'w') do |f|
          f.puts "sqlite3"
        end
      end

      it 'should perform normally' do
        output = capture_stdout {
          expect {
            Jasmine::CommandLineTool.new.process ['init']
          }.not_to raise_error
        }
        expect(output).to match /Jasmine has been installed\./

        expect(File.exists?(File.join(@tmp, 'spec/javascripts/helpers/.gitkeep'))).to eq true
        expect(File.exists?(File.join(@tmp, 'spec/javascripts/support/jasmine.yml'))).to eq true
      end
    end
  end

  it 'should install the examples' do
    output = capture_stdout { Jasmine::CommandLineTool.new.process ['examples'] }
    expect(output).to match /Jasmine has installed some examples\./
    expect(File.exists?(File.join(@tmp, 'public/javascripts/jasmine_examples/Player.js'))).to eq true
    expect(File.exists?(File.join(@tmp, 'public/javascripts/jasmine_examples/Song.js'))).to eq true
    expect(File.exists?(File.join(@tmp, 'spec/javascripts/jasmine_examples/PlayerSpec.js'))).to eq true
    expect(File.exists?(File.join(@tmp, 'spec/javascripts/helpers/jasmine_examples/SpecHelper.js'))).to eq true

    capture_stdout { Jasmine::CommandLineTool.new.process ['init'] }
    ci_output = `rake --trace jasmine:ci`
    expect(ci_output).to match (/[1-9]\d* specs, 0 failures/)
  end

  it 'should include license info' do
    output = capture_stdout { Jasmine::CommandLineTool.new.process ['license'] }
    expect(output).to match /Copyright/
  end

  it 'should copy boot.js' do
    output = capture_stdout { Jasmine::CommandLineTool.new.process ['copy_boot_js'] }
    expect(output).to match /Jasmine has copied an example boot.js to spec\/javascripts\/support/

    expect(File.exists?(File.join(@tmp, 'spec/javascripts/support/boot.js'))).to eq true
  end

  it 'should not overwrite an existing boot.js' do
    capture_stdout { Jasmine::CommandLineTool.new.process ['copy_boot_js'] }
    output = capture_stdout { Jasmine::CommandLineTool.new.process ['copy_boot_js'] }

    expect(output).to match /already exists/
  end

  it 'should show help' do
    no_arg_output = capture_stdout { Jasmine::CommandLineTool.new.process [] }
    expect(no_arg_output).to_not match /unknown command/
    expect(no_arg_output).to match /Usage:/

    unknown_arg_output = capture_stdout { Jasmine::CommandLineTool.new.process ['blurgh', 'blargh'] }
    expect(unknown_arg_output).to match /unknown command blurgh blargh/
    expect(unknown_arg_output).to match /Usage:/
  end
end
