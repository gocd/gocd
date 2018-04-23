require 'spec_helper'

describe "POJS jasmine install" do

  before :each do
    temp_dir_before
    Dir::chdir @tmp
    @install_directory = 'pojs-example'
    Dir::mkdir @install_directory
    Dir::chdir @install_directory

    `jasmine init`
    `jasmine examples`
  end

  after :each do
    temp_dir_after
  end

  it "should find the Jasmine configuration files" do
    expect(File.exists?("spec/javascripts/support/jasmine.yml")).to eq true
  end

  it "should find the Jasmine example files" do
    expect(File.exists?("public/javascripts/jasmine_examples/Player.js")).to eq true
    expect(File.exists?("public/javascripts/jasmine_examples/Song.js")).to eq true

    expect(File.exists?("spec/javascripts/jasmine_examples/PlayerSpec.js")).to eq true
    expect(File.exists?("spec/javascripts/helpers/jasmine_examples/SpecHelper.js")).to eq true

    expect(File.exists?("spec/javascripts/support/jasmine.yml")).to eq true
  end

  it "should show jasmine rake task" do
    output = `rake -T`
    expect(output).to include("jasmine ")
    expect(output).to include("jasmine:ci")
  end

  it "should successfully run rake jasmine:ci" do
    output = `rake jasmine:ci`
    expect(output).to match (/[1-9]\d* specs, 0 failures/)
    expect(output).to_not match /Randomized with seed/
  end

  it "should raise an error when jasmine.yml cannot be found" do
    config_path = 'some/thing/that/doesnt/exist'
    output = `rake jasmine:ci JASMINE_CONFIG_PATH=#{config_path}`
    expect($?).to_not be_success
    expect(output).to match /Unable to load jasmine config from #{config_path}/
  end

  it "rake jasmine:ci returns proper exit code when the runner raises" do
    failing_runner = File.join('spec', 'javascripts', 'support', 'failing_runner.rb')
    failing_yaml = custom_jasmine_config('raises_exception') do |config|
      config['spec_helper'] = failing_runner
    end

    FileUtils.cp(File.join(@root, 'spec', 'fixture', 'failing_runner.rb'), failing_runner)

    `rake jasmine:ci JASMINE_CONFIG_PATH=#{failing_yaml}`
    expect($?).to_not be_success
  end

  context 'with a spec with a console.log' do
    before do
      FileUtils.cp(File.join(@root, 'spec', 'fixture', 'console_log_spec.js'), File.join('spec', 'javascripts'))
    end

    it 'hides console.log by default' do
      output = `rake jasmine:ci`
      expect(output).to_not include("I'm in the webpage!")
    end

    it 'can be told to show console.log' do
      log_yaml = custom_jasmine_config('log') do |jasmine_config|
        jasmine_config['show_console_log'] = true
      end
      output = `rake jasmine:ci JASMINE_CONFIG_PATH=#{log_yaml}`
      expect(output).to include("I'm in the webpage!")
    end
  end

  it 'should allow customizing the phantom page' do
    FileUtils.cp(File.join(@root, 'spec', 'fixture', 'viewport_spec.js'), File.join('spec', 'javascripts'))
    FileUtils.cp(File.join(@root, 'spec', 'fixture', 'phantomConfig.js'), File.join('spec', 'javascripts', 'support'))
    viewport_yaml = custom_jasmine_config('viewport') do |jasmine_config|
      jasmine_config['phantom_config_script'] = 'spec/javascripts/support/phantomConfig.js'
      jasmine_config['show_console_log'] = true
    end

    output = `rake jasmine:ci JASMINE_CONFIG_PATH=#{viewport_yaml}`
    expect(output).to match /[1-9]\d* specs, 0 failures/
  end

  it 'should throw a useful error when the phantom customization fails' do
    bad_phantom_yaml = custom_jasmine_config('viewport') do |jasmine_config|
      jasmine_config['phantom_config_script'] = 'spec/javascripts/support/doesNotExist.js'
      jasmine_config['show_console_log'] = true
    end

    output = `rake jasmine:ci JASMINE_CONFIG_PATH=#{bad_phantom_yaml}`
    expect($?).to_not be_success
    expect(output).to match /Failed to configure phantom/
  end

  it 'should fail correctly with a failure in afterAll' do
    FileUtils.cp(File.join(@root, 'spec', 'fixture', 'afterall_spec.js'), File.join('spec', 'javascripts'))

    output = `rake jasmine:ci`
    expect($?).to_not be_success
    expect(output).to match /afterAll go boom/
  end

  it 'should tell jasmine to randomize the execution order' do
    randomized_yaml = custom_jasmine_config('random') do |jasmine_config|
      jasmine_config['random'] = true
    end

    output = `rake jasmine:ci JASMINE_CONFIG_PATH=#{randomized_yaml}`
    expect($?).to be_success
    expect(output).to match /Randomized with seed/
  end
end
