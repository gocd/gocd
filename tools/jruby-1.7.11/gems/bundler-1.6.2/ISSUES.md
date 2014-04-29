# Bundler Issues

So! You're having problems with Bundler. This file is here to help. If you're running into an error, try reading the rest of this file for help. If you can't figure out how to solve your problem, there are also instructions on how to report a bug.

**Please use the [Bundler
Features](https://github.com/bundler/bundler-features) repo to suggest and
discuss features. The bundler issue tracker is only for bugs.**

## Documentation

Instructions for common Bundler uses can be found on the [Bundler documentation site](http://bundler.io/).

Detailed information about each Bundler command, including help with common problems, can be found in the [Bundler man pages](http://bundler.io/v1.3/man/bundle.1.html).

## Troubleshooting

### Heroku errors

Please open a ticket with Heroku if you're having trouble deploying. They have a professional support team who can help you resolve Heroku issues far better than the Bundler team can. If the problem that you are having turns out to be a bug in Bundler itself, Heroku support can get the exact details to us.

### Other problems

First, figure out exactly what it is that you're trying to do. Then, go to the [Bundler documentation website](http://bundler.io) and see if we have instructions on how to do that.

Second, check [the compatibility
list](http://bundler.io/compatibility.html), and make sure that the version of Bundler that you are
using works with the versions of Ruby and Rubygems that you are using.

If the instructions don't work, or you can't find any instructions, you can try these troubleshooting steps:

    # remove user-specific gems and git repos
    rm -rf ~/.bundle/ ~/.gem/bundler/ ~/.gems/cache/bundler/

    # remove system-wide git repos and git checkouts
    rm -rf $GEM_HOME/bundler/ $GEM_HOME/cache/bundler/

    # remove project-specific settings
    rm -rf .bundle/

    # remove project-specific cached gems and repos
    rm -rf vendor/cache/

    # remove the saved resolve of the Gemfile
    rm -rf Gemfile.lock

    # uninstall the rubygems-bundler and open_gem gems
    rvm gemset use global # if using rvm
    gem uninstall rubygems-bundler open_gem

    # try to install one more time
    bundle install

## Reporting unresolved problems

Hopefully the troubleshooting steps above resolved your problem. If things still aren't working the way you expect them to, please let us know so that we can diagnose and hopefully fix the problem you're having.

**The best way to report a bug is by providing a reproduction script.** See these examples:

* [Git environment variables causing install to fail.](https://gist.github.com/xaviershay/6207550)
* [Multiple gems in a repository cannot be updated independently.](https://gist.github.com/xaviershay/6295889)

A half working script with comments for the parts you were unable to automate is still appreciated.

If you are unable to do that, please include the following information in your report:

 - What you're trying to accomplish
 - The command you ran
 - What you expected to happen
 - What actually happened
 - The exception backtrace(s), if any
 - Everything output by running `bundle env`

If your version of Bundler does not have the `bundle env` command, then please include:

 - Your Gemfile
 - Your Gemfile.lock
 - Your Bundler configuration settings (run `bundle config`)
 - What version of bundler you are using (run `bundle -v`)
 - What version of Ruby you are using (run `ruby -v`)
 - What version of Rubygems you are using (run `gem -v`)
 - Whether you are using RVM, and if so what version (run `rvm -v`)
 - Whether you have the `rubygems-bundler` gem, which can break gem executables (run `gem list rubygems-bundler`)
 - Whether you have the `open_gem` gem, which can cause rake activation conflicts (run `gem list open_gem`)

If you are using Rails 2.3, please also include:

  - Your boot.rb file
  - Your preinitializer.rb file
  - Your environment.rb file


If you have either `rubygems-bundler` or `open_gem` installed, please try removing them and then following the troubleshooting steps above before opening a new ticket.

[Create a gist](https://gist.github.com) containing all of that information, then visit the [Bundler issue tracker](https://github.com/bundler/bundler/issues) and [create a ticket](https://github.com/bundler/bundler/issues/new) describing your problem and linking to your gist.

Thanks for reporting issues and helping make Bundler better!
