# Listen [![Gem Version](https://badge.fury.io/rb/listen.png)](http://badge.fury.io/rb/listen) [![Build Status](https://secure.travis-ci.org/guard/listen.png?branch=master)](http://travis-ci.org/guard/listen) [![Dependency Status](https://gemnasium.com/guard/listen.png)](https://gemnasium.com/guard/listen) [![Code Climate](https://codeclimate.com/github/guard/listen.png)](https://codeclimate.com/github/guard/listen) [![Coverage Status](https://coveralls.io/repos/guard/listen/badge.png?branch=master)](https://coveralls.io/r/guard/listen)

The Listen gem listens to file modifications and notifies you about the changes.

## Features

* Works everywhere!
* Supports watching multiple directories from a single listener.
* OS-specific adapters for Mac OS X 10.6+, Linux, *BSD and Windows.
* Automatic fallback to polling if OS-specific adapter doesn't work.
* Detects file modification, addition and removal.
* File content checksum comparison for modifications made under the same second.
* Allows supplying regexp-patterns to ignore and filter paths for better results.
* Tested on all Ruby environments via [Travis CI](https://travis-ci.org/guard/listen).

## Pending features

Still not implemented, pull requests are welcome.

* Symlinks support. [#25](https://github.com/guard/listen/issues/25)
* Signal handling. [#105](https://github.com/guard/listen/issues/105)
* Non-recursive directory scanning. [#111](https://github.com/guard/listen/issues/111)

## Install

### Using Bundler

The simplest way to install Listen is to use Bundler.

Add Listen to your Gemfile:

```ruby
group :development do
  gem 'listen'
end
```

and install it by running Bundler:

```bash
$ bundle
```

### Install the gem with RubyGems

```bash
$ gem install listen
```

### On Windows

If your are on Windows and using Ruby MRI >= 1.9.2 you can try to use the [`wdm`](https://github.com/Maher4Ever/wdm) instead of polling.
Please add the following to your Gemfile:

```ruby
require 'rbconfig'
gem 'wdm', '>= 0.1.0' if RbConfig::CONFIG['target_os'] =~ /mswin|mingw/i
```

## Usage

There are **two ways** to use Listen:

1. Block API: Call `Listen.to`/`Listen.to!` with either a single directory or multiple directories, then define the `change` callback in a block.
2. "Object" API: Create a `listener` object and use it in a chainable way.

### Block API

``` ruby
# Listen to a single directory.
Listen.to('dir/path/to/listen', :filter => /\.rb$/, :ignore => %r{ignored/path/}) do |modified, added, removed|
  # ...
end

# Listen to multiple directories.
Listen.to('dir/to/awesome_app', 'dir/to/other_app', :filter => /\.rb$/, :latency => 0.1) do |modified, added, removed|
  # ...
end
```

### "Object" API

``` ruby
listener = Listen.to('dir/path/to/listen')
listener = listener.ignore(%r{^ignored/path/})
listener = listener.filter(/\.rb$/)
listener = listener.latency(0.5)
listener = listener.force_polling(true)
listener = listener.polling_fallback_message(false)
listener = listener.force_adapter(Listen::Adapters::Linux)
listener = listener.change(&callback)
listener.start
```

**Note**: All the "Object" API methods except `start`/`start!` return the listener
and are thus chainable:

``` ruby
Listen.to('dir/path/to/listen')
      .ignore(%r{^ignored/path/})
      .filter(/\.rb$/)
      .latency(0.5)
      .force_polling(true)
      .polling_fallback_message('custom message')
      .change(&callback)
      .start
```

### Pause/Unpause

Listener can also easily be paused/unpaused:

``` ruby
listener = Listen.to('dir/path/to/listen')
listener.start   # non-blocking mode
listener.pause   # stop listening to changes
listener.paused? # => true
listener.unpause # start listening to changes again
listener.stop    # stop completely the listener
```

## Changes callback

Changes to the listened-to directories gets reported back to the user in a callback.
The registered callback gets invoked, when there are changes, with **three** parameters:
`modified_paths`, `added_paths` and `removed_paths` in that particular order.

You can register a callback in two ways. The first way is by passing a block when calling
the `Listen.to`/`Listen.to!` method or when initializing a listener object:

```ruby
Listen.to('path/to/app') do |modified, added, removed|
  # This block will be called when there are changes.
end

# or ...

listener = Listen::Listener.new('path/to/app') do |modified, added, removed|
  # This block will be called when there are changes.
end

```

The second way to register a callback is by calling the `#change` method on a
listener passing it a block:

```ruby
# Create a callback
callback = Proc.new do |modified, added, removed|
  # This proc will be called when there are changes.
end

listener = Listen.to('dir')
listener.change(&callback) # convert the callback to a block and register it

listener.start
```

### Paths in callbacks

Listeners invoke callbacks passing them absolute paths by default:

```ruby
# Assume someone changes the 'style.css' file in '/home/user/app/css' after creating
# the listener.
Listen.to('/home/user/app/css') do |modified, added, removed|
  modified.inspect # => ['/home/user/app/css/style.css']
end
```

#### Relative paths in callbacks

When creating a listener for a **single** path (more specifically a `Listen::Listener` instance),
you can pass `:relative_paths => true` as an option to get relative paths in
your callback:

```ruby
# Assume someone changes the 'style.css' file in '/home/user/app/css' after creating
# the listener.
Listen.to('/home/user/app/css', :relative_paths => true) do |modified, added, removed|
  modified.inspect # => ['style.css']
end
```

Passing the `:relative_paths => true` option won't work when listening to multiple
directories:

```ruby
# Assume someone changes the 'style.css' file in '/home/user/app/css' after creating
# the listener.
Listen.to('/home/user/app/css', '/home/user/app/js', :relative_paths => true) do |modified, added, removed|
  modified.inspect # => ['/home/user/app/css/style.css']
end
```

## Options

All the following options can be set through the `Listen.to`/`Listen.to!` params
or via ["Object" API](#object-api) methods:

```ruby
:ignore => %r{app/CMake/}, /\.pid$/           # Ignore a list of paths (root directory or sub-dir)
                                              # default: See DEFAULT_IGNORED_DIRECTORIES and DEFAULT_IGNORED_EXTENSIONS in Listen::DirectoryRecord

:filter => /\.rb$/, /\.coffee$/               # Filter files to listen to via a regexps list.
                                              # default: none

:latency => 0.5                               # Set the delay (**in seconds**) between checking for changes
                                              # default: 0.25 sec (1.0 sec for polling)

:force_adapter => Listen::Adapters::Linux     # Force the use of a particular adapter class
                                              # default: none

:force_polling => true                        # Force the use of the polling adapter
                                              # default: none

:polling_fallback_message => 'custom message' # Set a custom polling fallback message (or disable it with false)
                                              # default: "Listen will be polling for changes. Learn more at https://github.com/guard/listen#polling-fallback."

:relative_paths => true                       # Enable the use of relative paths in the callback.
                                              # default: false
```

### Note on the patterns for ignoring and filtering paths

Just like the unix convention of beginning absolute paths with the
directory-separator (forward slash `/` in unix) and with no prefix for relative paths,
Listen doesn't prefix relative paths (to the watched directory) with a directory-separator.

Therefore make sure _NOT_ to prefix your regexp-patterns for filtering or ignoring paths
with a directory-separator, otherwise they won't work as expected.

As an example: to ignore the `build` directory in a C-project, use `%r{build/}`
and not `%r{/build/}`.

Use `#filter!` and `#ignore!` methods to overwrites default patterns.

## Blocking listening to changes

Calling `Listen.to` with a block doesn't block the current thread. If you want
to block the current thread instead until the listener is stopped (which needs
to be done from another thread), you can use `Listen.to!`.

Similarly, if you're using the "Object" API, you can use `#start!` instead of `#start` to block the
current thread until the listener is stopped.

Here is an example of using a listener in the blocking mode:

```ruby
Listen.to!('dir/path/to/listen') # block execution

# Code here will not run until the listener is stopped

```

Here is an example of using a listener started with the "Object" API in blocking mode:

```ruby
listener = Listen.to('dir/path/to/listen')
listener.start! # block execution

# Code here will not run until the listener is stopped

```

**Note**: Using the `Listen.to!` helper-method with or without a callback-block
will always start the listener right away and block execution of the current thread.

## Listen adapters

The Listen gem has a set of adapters to notify it when there are changes.
There are 4 OS-specific adapters to support Mac, Linux, *BSD and Windows.
These adapters are fast as they use some system-calls to implement the notifying function.

There is also a polling adapter which is a cross-platform adapter and it will
work on any system. This adapter is unfortunately slower than the rest of the adapters.

The Listen gem will choose the best and working adapter for your machine automatically. If you
want to force the use of the polling adapter, either use the `:force_polling` option
while initializing the listener or call the `#force_polling` method on your listener
before starting it.

It is also possible to force the use of a particular adapter, by using the `:force_adapter`
option.  This option skips the usual adapter choosing mechanism and uses the given
adapter class instead.  The adapter choosing mechanism requires write permission
to your watched directories and will needlessly load code, which isn't always desirable.

## Polling fallback

When a OS-specific adapter doesn't work the Listen gem automatically falls back to the polling adapter.
Here are some things you could try to avoid the polling fallback:

* [Update your Dropbox client](http://www.dropbox.com/downloading) (if used).
* Increase latency. (Please [open an issue](https://github.com/guard/listen/issues/new)
if you think that default is too low.)
* Move or rename the listened folder.
* Update/reboot your OS.

If your application keeps using the polling-adapter and you can't figure out why, feel free to [open an issue](https://github.com/guard/listen/issues/new) (and be sure to [give all the details](https://github.com/guard/listen/blob/master/CONTRIBUTING.md)).

## Development [![Dependency Status](https://gemnasium.com/guard/listen.png?branch=master)](https://gemnasium.com/guard/listen)

* Documentation hosted at [RubyDoc](http://rubydoc.info/github/guard/listen/master/frames).
* Source hosted at [GitHub](https://github.com/guard/listen).

Pull requests are very welcome! Please try to follow these simple rules if applicable:

* Please create a topic branch for every separate change you make.
* Make sure your patches are well tested. All specs must pass on [Travis CI](https://travis-ci.org/guard/listen).
* Update the [Yard](http://yardoc.org/) documentation.
* Update the [README](https://github.com/guard/listen/blob/master/README.md).
* Update the [CHANGELOG](https://github.com/guard/listen/blob/master/CHANGELOG.md) for noteworthy changes (don't forget to run `bundle exec pimpmychangelog` and watch the magic happen)!
* Please **do not change** the version number.

For questions please join us in our [Google group](http://groups.google.com/group/guard-dev) or on
`#guard` (irc.freenode.net).

## Acknowledgments

* [Michael Kessler (netzpirat)][] for having written the [initial specs](https://github.com/guard/listen/commit/1e457b13b1bb8a25d2240428ce5ed488bafbed1f).
* [Travis Tilley (ttilley)][] for this awesome work on [fssm][] & [rb-fsevent][].
* [Nathan Weizenbaum (nex3)][] for [rb-inotify][], a thorough inotify wrapper.
* [Mathieu Arnold (mat813)][] for [rb-kqueue][], a simple kqueue wrapper.
* [stereobooster][] for [rb-fchange][], windows support wouldn't exist without him.
* [Yehuda Katz (wycats)][] for [vigilo][], that has been a great source of inspiration.

## Authors

* [Thibaud Guillaume-Gentil][] ([@thibaudgg](http://twitter.com/thibaudgg))
* [Maher Sallam][] ([@mahersalam](http://twitter.com/mahersalam))

## Contributors

[https://github.com/guard/listen/contributors](https://github.com/guard/listen/contributors)

[Thibaud Guillaume-Gentil]: https://github.com/thibaudgg
[Maher Sallam]: https://github.com/Maher4Ever
[Michael Kessler (netzpirat)]: https://github.com/netzpirat
[Travis Tilley (ttilley)]: https://github.com/ttilley
[fssm]: https://github.com/ttilley/fssm
[rb-fsevent]: https://github.com/thibaudgg/rb-fsevent
[Mathieu Arnold (mat813)]: https://github.com/mat813
[Nathan Weizenbaum (nex3)]: https://github.com/nex3
[rb-inotify]: https://github.com/nex3/rb-inotify
[stereobooster]: https://github.com/stereobooster
[rb-fchange]: https://github.com/stereobooster/rb-fchange
[rb-kqueue]: https://github.com/mat813/rb-kqueue
[Yehuda Katz (wycats)]: https://github.com/wycats
[vigilo]: https://github.com/wycats/vigilo
