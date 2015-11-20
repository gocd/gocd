# encoding: utf-8

require File.expand_path('../spec_helper', __FILE__)

describe ChildProcess do
  it "returns self when started" do
    process = sleeping_ruby

    expect(process.start).to eq process
    expect(process).to be_alive
  end

  # We can't detect failure to execve() when using posix_spawn() on Linux
  # without waiting for the child to exit with code 127.
  #
  # See e.g. http://repo.or.cz/w/glibc.git/blob/669704fd:/sysdeps/posix/spawni.c#l34
  #
  # We could work around this by doing the PATH search ourselves, but not sure
  # it's worth it.
  it "raises ChildProcess::LaunchError if the process can't be started", :posix_spawn_on_linux => false do
    expect { invalid_process.start }.to raise_error(ChildProcess::LaunchError)
  end

  it 'raises ArgumentError if given a non-string argument' do
    expect { ChildProcess.build(nil, "unlikelytoexist") }.to raise_error(ArgumentError)
    expect { ChildProcess.build("foo", 1) }.to raise_error(ArgumentError)
  end

  it "knows if the process crashed" do
    process = exit_with(1).start
    process.wait

    expect(process).to be_crashed
  end

  it "knows if the process didn't crash" do
    process = exit_with(0).start
    process.wait

    expect(process).to_not be_crashed
  end

  it "can wait for a process to finish" do
    process = exit_with(0).start
    return_value = process.wait

    expect(process).to_not be_alive
    expect(return_value).to eq 0
  end

  it 'ignores #wait if process already finished' do
    process = exit_with(0).start
    sleep 0.01 until process.exited?

    expect(process.wait).to eql 0
  end

  it "escalates if TERM is ignored" do
    process = ignored('TERM').start
    process.stop
    expect(process).to be_exited
  end

  it "accepts a timeout argument to #stop" do
    process = sleeping_ruby.start
    process.stop(exit_timeout)
  end

  it "lets child process inherit the environment of the current process" do
    Tempfile.open("env-spec") do |file|
      with_env('INHERITED' => 'yes') do
        process = write_env(file.path).start
        process.wait
      end

      child_env = eval rewind_and_read(file)
      expect(child_env['INHERITED']).to eql 'yes'
    end
  end

  it "can override env vars only for the current process" do
    Tempfile.open("env-spec") do |file|
      process = write_env(file.path)
      process.environment['CHILD_ONLY'] = '1'
      process.start

      expect(ENV['CHILD_ONLY']).to be_nil

      process.wait

      child_env = eval rewind_and_read(file)
      expect(child_env['CHILD_ONLY']).to eql '1'
    end
  end

  it "inherits the parent's env vars also when some are overridden" do
    Tempfile.open("env-spec") do |file|
      with_env('INHERITED' => 'yes', 'CHILD_ONLY' => 'no') do
        process = write_env(file.path)
        process.environment['CHILD_ONLY'] = 'yes'

        process.start
        process.wait

        child_env = eval rewind_and_read(file)

        expect(child_env['INHERITED']).to eq 'yes'
        expect(child_env['CHILD_ONLY']).to eq 'yes'
      end
    end
  end

  it "can unset env vars" do
    Tempfile.open("env-spec") do |file|
      ENV['CHILDPROCESS_UNSET'] = '1'
      process = write_env(file.path)
      process.environment['CHILDPROCESS_UNSET'] = nil
      process.start

      process.wait

      child_env = eval rewind_and_read(file)
      expect(child_env).to_not have_key('CHILDPROCESS_UNSET')
    end
  end

  it 'does not see env vars unset in parent' do
    Tempfile.open('env-spec') do |file|
      ENV['CHILDPROCESS_UNSET'] = nil
      process = write_env(file.path)
      process.start

      process.wait

      child_env = eval rewind_and_read(file)
      expect(child_env).to_not have_key('CHILDPROCESS_UNSET')
    end
  end


  it "passes arguments to the child" do
    args = ["foo", "bar"]

    Tempfile.open("argv-spec") do |file|
      process = write_argv(file.path, *args).start
      process.wait

      expect(rewind_and_read(file)).to eql args.inspect
    end
  end

  it "lets a detached child live on" do
    pending "how do we spec this?"
    fail
  end

  it "preserves Dir.pwd in the child" do
    Tempfile.open("dir-spec-out") do |file|
      process = ruby("print Dir.pwd")
      process.io.stdout = process.io.stderr = file

      expected_dir = nil
      Dir.chdir(Dir.tmpdir) do
        expected_dir = Dir.pwd
        process.start
      end

      process.wait

      expect(rewind_and_read(file)).to eq expected_dir
    end
  end

  it "can handle whitespace, special characters and quotes in arguments" do
    args = ["foo bar", 'foo\bar', "'i-am-quoted'", '"i am double quoted"']

    Tempfile.open("argv-spec") do |file|
      process = write_argv(file.path, *args).start
      process.wait

      expect(rewind_and_read(file)).to eq args.inspect
    end
  end

  it 'handles whitespace in the executable name' do
    path = File.expand_path('foo bar')

    with_executable_at(path) do |proc|
      expect(proc.start).to eq proc
      expect(proc).to be_alive
    end
  end

  it "times out when polling for exit" do
    process = sleeping_ruby.start
    expect { process.poll_for_exit(0.1) }.to raise_error(ChildProcess::TimeoutError)
  end

  it "can change working directory" do
    process = ruby "print Dir.pwd"

    with_tmpdir { |dir|
      process.cwd = dir

      orig_pwd = Dir.pwd

      Tempfile.open('cwd') do |file|
        process.io.stdout = file

        process.start
        process.wait

        expect(rewind_and_read(file)).to eq dir
      end

      expect(Dir.pwd).to eq orig_pwd
    }
  end

  it 'kills the full process tree', :process_builder => false do
    Tempfile.open('kill-process-tree') do |file|
      process = write_pid_in_sleepy_grand_child(file.path)
      process.leader = true
      process.start

      pid = wait_until(30) do
        Integer(rewind_and_read(file)) rescue nil
      end

      process.stop
      wait_until(3) { expect(alive?(pid)).to eql(false) }
    end
  end

  it 'releases the GIL while waiting for the process' do
    time = Time.now
    threads = []

    threads << Thread.new { sleeping_ruby(1).start.wait }
    threads << Thread.new(time) { expect(Time.now - time).to be < 0.5 }

    threads.each { |t| t.join }
  end

  it 'can check if a detached child is alive' do
    proc = ruby_process("-e", "sleep")
    proc.detach = true

    proc.start

    expect(proc).to be_alive
    proc.stop(0)

    expect(proc).to be_exited
  end
end
