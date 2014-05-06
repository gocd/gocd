require File.dirname(__FILE__) + '/../../spec_helper'

shared_examples_for "script/spec_server file" do
  attr_accessor :tmbundle_install_directory, :tmbundle_source_directory
  attr_reader :animals_yml_path, :original_animals_content

  before do
    @animals_yml_path = File.expand_path("#{RAILS_ROOT}/spec/fixtures/animals.yml")
    @original_animals_content = File.read(animals_yml_path)
  end

  after do
    File.open(animals_yml_path, "w") do |f|
      f.write original_animals_content
    end
  end

  after(:each) do
    system "lsof -i tcp:8989 | sed /COMMAND/d | awk '{print $2}' | xargs kill"
  end

  it "runs a spec" do
    dir = File.expand_path(File.dirname(__FILE__))
    output = ""
    Timeout.timeout(20) do
      loop do
        output = `#{RAILS_ROOT}/script/spec #{dir}/sample_spec.rb --drb 2>&1`
        break unless output.include?("No server is running")
      end
    end

    if $?.exitstatus != 0 || output !~ /0 failures/
      flunk "command 'script/spec spec/sample_spec' failed\n#{output}"
    end

    fixtures = YAML.load(@original_animals_content)
    fixtures['pig']['name'] = "Piggy"

    File.open(animals_yml_path, "w") do |f|
      f.write YAML.dump(fixtures)
    end

    Timeout.timeout(20) do
      loop do
        output = `#{RAILS_ROOT}/script/spec #{dir}/sample_modified_fixture.rb --drb 2>&1`
        break unless output.include?("No server is running")
      end
    end

    if $?.exitstatus != 0 || output !~ /0 failures/
      flunk "command 'script/spec spec/sample_modified_fixture' failed\n#{output}"
    end
  end

  def start_spec_server
    dir = File.dirname(__FILE__)
    
    Thread.start do
      system "cd #{RAILS_ROOT}; CACHE_CLASSES=#{(Rails::VERSION::STRING < '2.2')} script/spec_server"
    end

    file_content = ""
  end
end

describe "script/spec_server file without TextMate bundle" do
  it_should_behave_like "script/spec_server file"
  before(:each) do
    start_spec_server
  end
end

describe "script/spec_server file with TextMate bundle" do
  it_should_behave_like "script/spec_server file"
  def tmbundle_directory
    current_directory = File.expand_path("#{File.dirname(__FILE__)}/../../../../../../")
    directories = current_directory.split('/')
    while directories.any?
      current_directory = File.join(*directories)
      bundle_directory = File.join(current_directory, @bundle_name)
      return bundle_directory if File.directory?(bundle_directory)
      directories.pop
    end
  end

  before(:each) do
    @tmbundle_install_directory = File.expand_path("#{Dir.tmpdir}/Library/Application Support/TextMate/Bundles")
    @bundle_name = "RSpec.tmbundle"
    @tmbundle_source_directory = tmbundle_directory

    FileUtils.mkdir_p(tmbundle_install_directory)
    File.directory?(tmbundle_source_directory).should be_true
    unless system(%Q|ln -s #{tmbundle_source_directory} "#{tmbundle_install_directory}"|)
      raise "Creating link to Textmate Bundle"
    end
    start_spec_server
  end

  after(:each) do
    bundle_file_to_remove = "#{tmbundle_install_directory}/#{@bundle_name}"
    if bundle_file_to_remove == "/"
      raise "bundle file path resolved to '/' - could not call rm"
    end
    unless system(%Q|rm "#{bundle_file_to_remove}"|)
      raise "Removing Textmate bundle link failed"
    end
  end
end
