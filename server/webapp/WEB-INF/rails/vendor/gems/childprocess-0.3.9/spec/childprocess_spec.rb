# encoding: utf-8

require File.expand_path('../spec_helper', __FILE__)

describe ChildProcess do
  it "returns self when started" do
    process = sleeping_ruby

    process.start.should == process
    process.should be_started
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

    process.should be_crashed
  end

  it "knows if the process didn't crash" do
    process = exit_with(0).start
    process.wait

    process.should_not be_crashed
  end

  it "can wait for a process to finish" do
    process = exit_with(0).start
    return_value = process.wait

    process.should_not be_alive
    return_value.should == 0
  end

  it "escalates if TERM is ignored" do
    process = ignored('TERM').start
    process.stop
    process.should be_exited
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

      file.rewind
      child_env = eval(file.read)
      child_env['INHERITED'].should == 'yes'
    end
  end

  it "can override env vars only for the current process" do
    Tempfile.open("env-spec") do |file|
      process = write_env(file.path)
      process.environment['CHILD_ONLY'] = '1'
      process.start

      ENV['CHILD_ONLY'].should be_nil

      process.wait
      file.rewind

      child_env = eval(file.read)
      child_env['CHILD_ONLY'].should == '1'
    end
  end

  it "inherits the parent's env vars also when some are overridden" do
    Tempfile.open("env-spec") do |file|
      with_env('INHERITED' => 'yes', 'CHILD_ONLY' => 'no') do
        process = write_env(file.path)
        process.environment['CHILD_ONLY'] = 'yes'

        process.start
        process.wait

        file.rewind
        child_env = eval(file.read)

        child_env['INHERITED'].should == 'yes'
        child_env['CHILD_ONLY'].should == 'yes'
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
      file.rewind

      child_env = eval(file.read)
      child_env.should_not have_key('CHILDPROCESS_UNSET')
    end
  end


  it "passes arguments to the child" do
    args = ["foo", "bar"]

    Tempfile.open("argv-spec") do |file|
      process = write_argv(file.path, *args).start
      process.wait

      file.rewind
      file.read.should == args.inspect
    end
  end

  it "lets a detached child live on" do
    pending "how do we spec this?"
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

      file.rewind
      file.read.should == expected_dir
    end
  end

  it "can handle whitespace, special characters and quotes in arguments" do
    args = ["foo bar", 'foo\bar', "'i-am-quoted'", '"i am double quoted"']

    Tempfile.open("argv-spec") do |file|
      process = write_argv(file.path, *args).start
      process.wait

      file.rewind
      file.read.should == args.inspect
    end
  end

  it 'handles whitespace in the executable name' do
    path = File.expand_path('foo bar')

    with_executable_at(path) do |proc|
      proc.start.should == proc
      proc.should be_started
    end
  end

  it "times out when polling for exit" do
    process = sleeping_ruby.start
    lambda { process.poll_for_exit(0.1) }.should raise_error(ChildProcess::TimeoutError)
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

        file.rewind
        file.read.should == dir
      end

      Dir.pwd.should == orig_pwd
    }
  end
end
