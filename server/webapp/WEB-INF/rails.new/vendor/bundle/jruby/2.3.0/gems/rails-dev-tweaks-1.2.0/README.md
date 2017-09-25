rails-dev-tweaks
================

A collection of tweaks to improve your Rails (3.1+) development experience.

To install, simply add it to your gemfile:

    gem 'rails-dev-tweaks', '~> 1.1'

And review the following section to make sure that `rails-dev-tweaks` is
configured the way you expect:


Intended Usage (and Caveats)
----------------------------

This gem is intended to provide a default configuration that covers most rails
apps:

* _All_ asset requests _will not_ reload your app's code.  This is probably only
  a problem if you are using custom sass functions, or otherwise referencing
  your app from within assets.

* XHR requests **will reload** your app's code.  (This was not the case in prior
  versions of `rails-dev-tweaks`)

If any of these points don't work out for you, don't fret!  You can override the
defaults with some simple configuration tweaks to your environment.  Read on:


Granular Autoload
=================

You can specify autoload rules for your app via a configuration block in your
application or environment configuration. These rules are specified via
exclusion (`skip`) and inclusion (`keep`).  Rules defined later override those
defined before.

    config.dev_tweaks.autoload_rules do
      # You can used named matchers (see below).  This particular matcher
      # effectively clears any default matchers
      keep :all

      # Exclude all requests that begin with /search
      skip '/search'
      # But include routes that include smerch
      keep /smerch/

      # Use a block if you want to inspect the request
      skip {|request| request.post?}
    end

The default autoload rules should cover most development patterns:

    config.dev_tweaks.autoload_rules do
      keep :all

      skip '/favicon.ico'
      skip :assets
      keep :forced
    end

By default, every request that skips the autoload hooks will generate an
additional log line saying so in an effort to be transparent about what is going
on.  If you prefer, you can disable that log message to keep things a bit more
tidy in your logs:

    config.dev_tweaks.log_autoload_notice = false


Named Matchers
--------------

Named matchers are classes defined under
RailsDevTweaks::GranularAutoload::Matchers:: and simply define a call method
that is given a ActionDispatch::Request and returns true/false on whether that
request matches. Match names are converted into a module name via
"#{name.to\_s.classify}Matcher".  E.g. :assets will specify
`RailsDevTweaks::GranularAutoload::Matchers::AssetMatcher`.

Any additional arguments given to a `skip` or `keep` call will be passed as
initializer arguments to the matcher.


### :all

Matches every request passed to it.


### :assets

Rails 3.1 integrated [Sprockets](http://getsprockets.org/) as its asset
packager.  Unfortunately, since the asset packager is mounted using the
traditional Rails dispatching infrastructure, it's hidden behind the Rails
autoloader (unloader). This matcher will match any requests that are routed to
Sprockets (specifically any mounted Sprockets::Base instance).


### :forced

To aid in live-debugging when you need to, this matcher will match any request
that has `force_autoload` set as a parameter (GET or POST), or that has the
`Force-Autoload` header set to something.

If you are live-debugging jQuery ajax requests, this helpful snippet will turn
on forced autoloading for the remainder of the browser's session:

    $.ajaxSetup({"beforeSend": function(xhr) {xhr.setRequestHeader("Force-Autoload", "true")} })


### :path

Matches the path of the request via a regular expression.

    keep :path, /thing/ # Match any request with "thing" in the path.

Note that `keep '/stuff'` is just shorthand for `keep :path, /^\/stuff/`.
Similarly, `keep /thing/` is shorthand for `keep :path, /thing/`


### :xhr

Matches any XHR request (via request.xhr?).  The assumption here is that you
generally don't live-debug your XHR requests, and are instead refreshing the
page that kicks them off before running against new response code.


License
=======

`rails-dev-tweaks` is MIT licensed by Wavii, Inc.  http://wavii.com

See the accompanying file, `MIT-LICENSE`, for the full text.
