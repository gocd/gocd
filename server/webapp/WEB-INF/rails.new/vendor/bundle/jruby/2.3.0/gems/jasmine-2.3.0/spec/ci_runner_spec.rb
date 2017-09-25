require 'spec_helper'

describe Jasmine::CiRunner do
  let(:runner) { double(:runner, :run => nil) }
  let(:runner_factory) { double(:runner_factory, :call => runner) }

  let(:config) do
    double(:configuration,
           :runner => runner_factory,
           :formatters => [],
           :host => 'foo.bar.com',
           :port => '1234',
           :rack_options => 'rack options',
           :stop_spec_on_expectation_failure => false
          )
  end

  let(:thread_instance) { double(:thread, :abort_on_exception= => nil) }
  let(:fake_thread) do
    thread = double(:thread)
    allow(thread).to receive(:new) do |&block|
      @thread_block = block
      thread_instance
    end
    thread
  end
  let(:application_factory) { double(:application, :app => 'my fake app') }
  let(:fake_server) { double(:server, :start => nil) }
  let(:server_factory) { double(:server_factory, :new => fake_server) }
  let(:outputter) { double(:outputter, :puts => nil) }

  before do
    allow(Jasmine).to receive(:wait_for_listener)
  end

  it 'starts a server and runner' do
    ci_runner = Jasmine::CiRunner.new(config, thread: fake_thread, application_factory: application_factory, server_factory: server_factory, outputter: outputter)

    ci_runner.run

    expect(config).to have_received(:port).with(:ci).at_least(:once)
    expect(config).not_to have_received(:port).with(:server)

    expect(runner_factory).to have_received(:call).with(instance_of(Jasmine::Formatters::Multi), 'foo.bar.com:1234/?throwFailures=false')

    expect(application_factory).to have_received(:app).with(config)
    expect(server_factory).to have_received(:new).with('1234', 'my fake app', 'rack options')

    expect(fake_thread).to have_received(:new)
    expect(thread_instance).to have_received(:abort_on_exception=).with(true)

    @thread_block.call
    expect(fake_server).to have_received(:start)

    expect(Jasmine).to have_received(:wait_for_listener).with('1234', 'jasmine server')

    expect(runner).to have_received(:run)
  end

  it 'adds runner boot files when necessary' do
    expect(runner).to receive(:boot_js).at_least(:once) { 'foo/bar/baz.js' }
    expect(config).to receive(:runner_boot_dir=).with('foo/bar')
    expect(config).to receive(:runner_boot_files=) do |proc|
      expect(proc.call).to eq ['foo/bar/baz.js']
    end

    ci_runner = Jasmine::CiRunner.new(config, thread: fake_thread, application_factory: application_factory, server_factory: server_factory, outputter: outputter)

    ci_runner.run
  end

  it 'returns true for a successful run' do
    allow(Jasmine::Formatters::ExitCode).to receive(:new) { double(:exit_code, :succeeded? => true) }

    ci_runner = Jasmine::CiRunner.new(config, thread: fake_thread, application_factory: application_factory, server_factory: server_factory, outputter: outputter)

    expect(ci_runner.run).to be(true)
  end

  it 'returns false for a failed run' do
    allow(Jasmine::Formatters::ExitCode).to receive(:new) { double(:exit_code, :succeeded? => false) }

    ci_runner = Jasmine::CiRunner.new(config, thread: fake_thread, application_factory: application_factory, server_factory: server_factory, outputter: outputter)

    expect(ci_runner.run).to be(false)
  end

  it 'can tell the jasmine page to throw expectation failures' do
    allow(config).to receive(:stop_spec_on_expectation_failure) { true }

    ci_runner = Jasmine::CiRunner.new(config, thread: fake_thread, application_factory: application_factory, server_factory: server_factory, outputter: outputter)

    ci_runner.run

    expect(runner_factory).to have_received(:call).with(instance_of(Jasmine::Formatters::Multi), 'foo.bar.com:1234/?throwFailures=true')
  end
end
