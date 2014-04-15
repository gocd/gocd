# childprocess

This gem aims at being a simple and reliable solution for controlling
external programs running in the background on any Ruby / OS combination.

The code originated in the [selenium-webdriver](https://rubygems.org/gems/selenium-webdriver) gem, but should prove useful as
a standalone library.

[![Build Status](https://secure.travis-ci.org/jarib/childprocess.png)](http://travis-ci.org/jarib/childprocess)

# Usage

The object returned from `ChildProcess.build` will implement `ChildProcess::AbstractProcess`.

### Basic examples

```ruby
process = ChildProcess.build("ruby", "-e", "sleep")

# inherit stdout/stderr from parent...
process.io.inherit!

# ...or pass an IO
process.io.stdout = Tempfile.new("child-output")

# modify the environment for the child
process.environment["a"] = "b"
process.environment["c"] = nil

# set the child's working directory
process.cwd = '/some/path'

# start the process
process.start

# check process status
process.alive?    #=> true
process.exited?   #=> false

# wait indefinitely for process to exit...
process.wait
process.exited?   #=> true

# get the exit code
process.exit_code #=> 0

# ...or poll for exit + force quit
begin
  process.poll_for_exit(10)
rescue ChildProcess::TimeoutError
  process.stop # tries increasingly harsher methods to kill the process.
end
```

### Advanced examples

#### Output to pipe

```ruby
r, w = IO.pipe

proc = ChildProcess.build("echo", "foo")
proc.io.stdout = proc.io.stderr = w
proc.start
w.close

begin
  loop { print r.readpartial(8192) }
rescue EOFError
end

proc.wait
```

Note that if you just want to get the output of a command, the backtick method on Kernel may be a better fit.

#### Write to stdin

```ruby
process = ChildProcess.build("cat")

out      = Tempfile.new("duplex")
out.sync = true

process.io.stdout = process.io.stderr = out
process.duplex    = true # sets up pipe so process.io.stdin will be available after .start

process.start
process.io.stdin.puts "hello world"
process.io.stdin.close

process.poll_for_exit(exit_timeout_in_seconds)

out.rewind
out.read #=> "hello world\n"
```

#### Pipe output to another ChildProcess

```ruby
search           = ChildProcess.build("grep", '-E', %w(redis memcached).join('|'))
search.duplex    = true # sets up pipe so search.io.stdin will be available after .start
search.io.stdout = $stdout
search.start

listing           = ChildProcess.build("ps", "aux")
listing.io.stdout = search.io.stdin
listing.start
listing.wait

search.io.stdin.close
search.wait
```

#### Prefer posix_spawn on *nix

If the parent process is using a lot of memory, `fork+exec` can be very expensive. The `posix_spawn()` API removes this overhead.

```ruby
ChildProcess.posix_spawn = true
process = ChildProcess.build(*args)
```

#### Detach from parent

```ruby
process = ChildProcess.build("sleep", "10")
process.detach = true
process.start
```

# Implementation

How the process is launched and killed depends on the platform:

* Unix     : `fork + exec` (or `posix_spawn` if enabled)
* Windows  : `CreateProcess()` and friends
* JRuby    : `java.lang.{Process,ProcessBuilder}`

# Note on Patches/Pull Requests

* Fork the project.
* Make your feature addition or bug fix.
* Add tests for it. This is important so I don't break it in a future version unintentionally.
* Commit, do not mess with rakefile, version, or history. (if you want to have your own version, that is fine but bump version in a commit by itself I can ignore when I pull)
* Send me a pull request. Bonus points for topic branches.

# Copyright

Copyright (c) 2010-2013 Jari Bakken. See LICENSE for details.
