Great to have you here! Here are a few ways you can help out with [Bundler](http://github.com/bundler/bundler).

# Where should I start?

You can start learning about Bundler by reading [the documentation](http://bundler.io). If you want, you can also read a (lengthy) explanation of [why Bundler exists and what it does](http://bundler.io/rationale.html). You can also check out discussions about Bundler on the [Bundler mailing list](https://groups.google.com/group/ruby-bundler) and in the [Bundler IRC channel](http://webchat.freenode.net/?channels=%23bundler), which is #bundler on Freenode. Please note that this project is released with a contributor [code of conduct](http://bundler.io/conduct.html). By participating in this project you agree to abide by its terms.

## Your first commits

If you’re interested in contributing to Bundler, that’s awesome! We’d love your help.

If you have any questions after reading this page, please feel free to contact either [@indirect](https://github.com/indirect), [@segiddins](https://github.com/segiddins), or [@RochesterinNYC](https://github.com/RochesterinNYC). They are all happy to provide help working through your first bug fix or thinking through the problem you’re trying to resolve.

## How you can help

We track [small bugs and features](https://github.com/bundler/bundler/labels/contribution%3A%20small) so that anyone who wants to help can start with something that's not too overwhelming. We also keep a [list of things anyone can help with, any time](https://github.com/bundler/bundler/blob/master/CONTRIBUTING.md#contributing). If nothing on those lists looks good, talk to us, and we'll figure out what you can help with. We can absolutely use your help, no matter what level of programming skill you have at the moment.

# Development setup

Bundler doesn't use a Gemfile to list development dependencies, because when we tried it we couldn't tell if we were awake or it was just another level of dreams. To work on Bundler, you'll probably want to do a couple of things.

  1. Install `groff-base` and `graphviz` packages using your package manager, e.g for ubuntu

        $ sudo apt-get install graphviz groff-base -y

     and for OS X (with brew installed)

        $ brew install graphviz homebrew/dupes/groff

  2. Install Bundler's development dependencies

        $ bin/rake spec:deps

  3. Run the test suite, to make sure things are working

        $ bin/rake spec

  4. Set up a shell alias to run Bundler from your clone, e.g. a Bash alias:

        $ alias dbundle='BUNDLE_TRAMPOLINE_DISABLE=1 ruby -I /path/to/bundler/lib /path/to/bundler/exe/bundle'

     The `BUNDLE_TRAMPOLINE_DISABLE` environment variable ensures that the version of Bundler in `/path/to/bundler/lib` will be used. Without that environment setting, Bundler will automatically download, install, and run the version of Bundler listed in `Gemfile.lock`. With that set up, you can test changes you've made to Bundler by running `dbundle`, without interfering with the regular `bundle` command.

To dive into the code with Pry: `RUBYOPT=-rpry dbundle` to require pry and then run commands.

# Submitting Pull Requests

Before you submit a pull request, please remember to do the following:

- Make sure the code formatting and styling adheres to the guidelines. We use Rubocop for this. Lack of formatting adherence will result in automatic Travis build failures.

        $ bin/rubocop -a

- Please run the test suite:

        $ bin/rspec

- If you are unable to run the entire test suite, please run the unit test suite and at least the integration specs related to the command or domain of Bundler that your code changes relate to.

- Ex. For a pull request that changes something with `bundle update`, you might run:

        $ bin/rspec spec/bundler
        $ bin/rspec spec/commands/update_spec.rb

- Please ensure that the commit messages included in the pull request __do not__ have the following:
  - `@tag` Github user or team references (ex. `@indirect` or `@bundler/core`)
  - `#id` references to issues or pull requests (ex. `#43` or `bundler/bundler-site#12`)

  If you want to use these mechanisms, please instead include them in the pull request description. This prevents multiple notifications or references being created on commit rebases or pull request/branch force pushes.

- Additionally, do not use `[ci skip]` or `[skip ci]` mechanisms in your pull request titles/descriptions or commit messages. Every potential commit and pull request should run through Bundler's CI system. This applies to all changes/commits (ex. even a change to just documentation or the removal of a comment).

# Bug triage

Triage is the work of processing tickets that have been opened into actionable issues, feature requests, or bug reports. That includes verifying bugs, categorizing the ticket, and ensuring there's enough information to reproduce the bug for anyone who wants to try to fix it.

We've created an [issues guide](https://github.com/bundler/bundler/blob/master/ISSUES.md) to walk Bundler users through the process of troubleshooting issues and reporting bugs.

If you'd like to help, awesome! You can [report a new bug](https://github.com/bundler/bundler/issues/new) or browse our [existing open tickets](https://github.com/bundler/bundler/issues).

Not every ticket will point to a bug in Bundler's code, but open tickets usually mean that there is something we could improve to help that user. Sometimes that means writing additional documentation, sometimes that means making error messages clearer, and sometimes that means explaining to a user that they need to install git to use git gems.

When you're looking at a ticket, here are the main questions to ask:

  * Can I reproduce this bug myself?
  * Are the steps to reproduce clearly stated in the ticket?
  * Which versions of Bundler (1.1.x, 1.2.x, git, etc.) manifest this bug?
  * Which operating systems (OS X, Windows, Ubuntu, CentOS, etc.) manifest this bug?
  * Which rubies (MRI, JRuby, Rubinius, etc.) and which versions (1.8.7, 1.9.3, etc.) have this bug?

If you can't reproduce an issue, chances are good that the bug has been fixed (hurrah!). That's a good time to post to the ticket explaining what you did and how it worked.

If you can reproduce an issue, you're well on your way to fixing it. :) Fixing issues is similar to adding new features:

  1. Discuss the fix on the existing issue. Coordinating with everyone else saves duplicate work and serves as a great way to get suggestions and ideas if you need any.
  2. Base your commits on the correct branch. Bugfixes for 1.x versions of Bundler should be based on the matching 1-x-stable branch.
  3. Commit the code and at least one test covering your changes to a named branch in your fork.
  4. Put a line in the [CHANGELOG](https://github.com/bundler/bundler/blob/master/CHANGELOG.md) summarizing your changes under the next release under the “Bugfixes” heading.
  5. Send us a [pull request](https://help.github.com/articles/using-pull-requests) from your bugfix branch.

Finally, the ticket may be a duplicate of another older ticket. If you notice a ticket is a duplicate, simply comment on the ticket noting the original ticket’s number. For example, you could say “This is a duplicate of issue #42, and can be closed”.


# Adding New Features

If you would like to add a new feature to Bundler, please follow these steps:

  1. [Create an issue](https://github.com/bundler/bundler/issues/new) with the [`feature-request` label](https://github.com/bundler/bundler/labels/type:%20feature-request) to discuss your feature.
  2. Base your commits on the master branch, since we follow [SemVer](http://semver.org) and don't add new features to old releases.
  3. Commit the code and at least one test covering your changes to a feature branch in your fork.
  4. Send us a [pull request](https://help.github.com/articles/using-pull-requests) from your feature branch.

If you don't hear back immediately, don’t get discouraged! We all have day jobs, but we respond to most tickets within a day or two.


# Beta testing

Early releases require heavy testing, especially across various system setups. We :heart: testers, and are big fans of anyone who can run `gem install bundler --pre` and try out upcoming releases in their development and staging environments.

There may not always be prereleases or beta versions of Bundler. The Bundler team will tweet from the [@bundlerio account](http://twitter.com/bundlerio) when a prerelease or beta version becomes available. You are also always welcome to try checking out master and building a gem yourself if you want to try out the latest changes.


# Translations

We don't currently have any translations, but please reach out to us if you would like to help get this going.


# Documentation

Code needs explanation, and sometimes those who know the code well have trouble explaining it to someone just getting into it. Because of that, we welcome documentation suggestions and patches from everyone, especially if they are brand new to using Bundler.

Bundler has two main sources of documentation: the built-in help (including usage information and man pages) and the [Bundler documentation site](http://bundler.io).

If you’d like to submit a patch to the man pages, follow the steps for submitting a pull request above. All of the man pages are located in the `man` directory. Just use the “Documentation” heading when you describe what you did in the changelog.

If you have a suggestion or proposed change for [bundler.io](http://bundler.io), please open an issue or send a pull request to the [bundler-site](https://github.com/bundler/bundler-site) repository.


# Community

Community is an important part of all we do. If you’d like to be part of the Bundler community, you can jump right in and start helping make Bundler better for everyone who uses it.

It would be tremendously helpful to have more people answering questions about Bundler (and often simply about [Rubygems](https://github.com/rubygems/rubygems) or Ruby itself) in our [issue tracker](https://github.com/bundler/bundler/issues) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/bundler).

Additional documentation and explanation is always helpful, too. If you have any suggestions for the Bundler website [bundler.io](http://bundler.io), we would absolutely love it if you opened an issue or pull request on the [bundler-site](https://github.com/bundler/bundler-site) repository.

Finally, sharing your experiences and discoveries by writing them up is a valuable way to help others who have similar problems or experiences in the future. You can write a blog post, create an example and commit it to Github, take screenshots, or make videos.

Publishing examples of how Bundler is used helps everyone, and we’ve discovered that people already use it in ways that we never imagined when we were writing it. If you’re still not sure what to write about, there are also several projects doing interesting things based on Bundler. They could probably use publicity too.

Finally, all contributors to the Bundler project must agree to the contributor [code of conduct](http://bundler.io/conduct.html). By participating in this project you agree to abide by its terms.
