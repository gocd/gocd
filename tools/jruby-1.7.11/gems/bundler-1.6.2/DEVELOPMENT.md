Great to have you here! Here are a few ways you can help out with [Bundler](http://github.com/bundler/bundler).

# Where should I start?

You can start learning about Bundler by reading [the documentation](http://bundler.io). If you want, you can also read a (lengthy) explanation of [why Bundler exists and what it does](http://bundler.io/v1.5/rationale.html). You can also check out discussions about Bundler on the [Bundler mailing list](https://groups.google.com/group/ruby-bundler) and in the [Bundler IRC channel](http://webchat.freenode.net/?channels=%23bundler), which is #bundler on Freenode.

## Your first commits

If you’re interested in contributing to Bundler, that’s awesome! We’d love your help.

If you have any questions after reading this page, please feel free to contact either [@indirect](http://github.com/indirect) or [@hone](http://github.com/hone). They are both happy to provide help working through your first bugfix or thinking through the problem you’re trying to resolve.

## Tackle some small problems

We track [small
bugs](https://github.com/bundler/bundler/issues?labels=small&state=open) and [small features](https://github.com/bundler/bundler-features/issues?labels=small&state=open) so that anyone who wants to help can start with something that's not too overwhelming. If nothing on those lists looks good, though, just talk to us.


# Development setup

Bundler doesn't use a Gemfile to list development dependencies, because when we tried it we couldn't tell if we were awake or it was just another level of dreams. To work on Bundler, you'll probably want to do a couple of things.

  1. Install Bundler's development dependencies

        $ rake spec:deps

  2. Run the test suite, to make sure things are working

        $ rake spec

  3. Set up a shell alias to run Bundler from your clone, e.g. a Bash alias:

        $ alias dbundle='ruby -I /path/to/bundler/lib /path/to/bundler/bin/bundle'

     With that set up, you can test changes you've made to Bundler by running `dbundle`, without interfering with the regular `bundle` command.


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


# Adding new features

If you would like to add a new feature to Bundler, please follow these steps:

  1. [Create an issue](https://github.com/bundler/bundler-features/issues/new) to discuss your feature.
  2. Base your commits on the master branch, since we follow [SemVer](http://semver.org) and don't add new features to old releases.
  3. Commit the code and at least one test covering your changes to a feature branch in your fork.
  4. Put a line in the [CHANGELOG](https://github.com/bundler/bundler/blob/master/CHANGELOG.md) summarizing your changes under the next release under the "Features" heading.
  5. Send us a [pull request](https://help.github.com/articles/using-pull-requests) from your feature branch.

If you don't hear back immediately, don’t get discouraged! We all have day jobs, but we respond to most tickets within a day or two.


# Beta testing

Early releases require heavy testing, especially across various system setups. We :heart: testers, and are big fans of anyone who can run `gem install bundler --pre` and try out upcoming releases in their development and staging environments.

There may not always be prereleases or beta versions of Bundler. That said, you are always welcome to try checking out master and building a gem yourself if you want to try out the latest changes.


# Translations

We don't currently have any translations, but please reach out to us if you would like to help get this going.


# Documentation

Code needs explanation, and sometimes those who know the code well have trouble explaining it to someone just getting into it. Because of that, we welcome documentation suggestions and patches from everyone, especially if they are brand new to using Bundler.

Bundler has two main sources of documentation: the built-in help (including usage information and man pages) and the [Bundler documentation site](http://bundler.io).

If you’d like to submit a patch to the man pages, follow the steps for adding a feature above. All of the man pages are located in the `man` directory. Just use the “Documentation” heading when you describe what you did in the changelog.

If you have a suggestion or proposed change for [bundler.io](http://bundler.io), please open an issue or send a pull request to the [bundler-site](https://github.com/bundler/bundler-site) repository.


# Community

Community is an important part of all we do. If you’d like to be part of the Bundler community, you can jump right in and start helping make Bundler better for everyone who uses it.

It would be tremendously helpful to have more people answering questions about Bundler (and often simply about Rubygems or Ruby itself) in our [issue tracker](https://github.com/bundler/bundler/issues) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/bundler).

Additional documentation and explanation is always helpful, too. If you have any suggestions for the Bundler website [bundler.io](http://bundler.io), we would absolutely love it if you opened an issue or pull request on the [bundler-site](https://github.com/bundler/bundler-site) repository.

Finally, sharing your experiences and discoveries by writing them up is a valuable way to help others who have similar problems or experiences in the future. You can write a blog post, create an example and commit it to Github, take screenshots, or make videos.

Examples of how Bundler is used help everyone, and we’ve discovered that people already use it in ways that we never imagined when we were writing it. If you’re still not sure what to write about, there are also several projects doing interesting things based on Bundler. They could probably use publicity too.

If you let someone on the core team know you wrote about Bundler, we will add your post to the list of Bundler resources on the Github project wiki.
