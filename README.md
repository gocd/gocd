# GoCD

[![Google Groups](https://img.shields.io/badge/Google_Groups-user_help-purple)](https://groups.google.com/g/go-cd)
[![GitHub Discussions](https://img.shields.io/badge/GitHub_discussions-user_&amp;_dev_chat-green)](https://github.com/gocd/gocd/discussions)
[![GitHub License](https://img.shields.io/github/license/gocd/gocd?color=yellow)](LICENSE)
[![Server Docker Pulls](https://img.shields.io/docker/pulls/gocd/gocd-server?label=Server%20Docker%20pulls)](https://hub.docker.com/r/gocd/gocd-server/)


This is the main repository for [GoCD](https://gocd.org) - a continuous delivery server. GoCD helps you automate and streamline the build-test-release cycle for worry-free, continuous delivery of your product.

- To quickly build your first pipeline while learning key GoCD concepts, visit our [Test Drive GoCD](https://www.gocd.org/test-drive-gocd.html).
- To download GoCD, visit the [downloads page](https://www.gocd.org/download/).

## Security

Please see [the security policy](SECURITY.md) for details on GoCD's security status, and how to responsibly disclose issues.

## Development

GoCD is predominantly a Java & TypeScript project utilising [Spring Framework](https://spring.io/projects/spring-framework/), [SparkJava](https://sparkjava.com/) & [MithrilJS](https://mithril.js.org/) as key frameworks, built using [Gradle](https://gradle.org/) & [Webpack](https://webpack.js.org/) and running within [Eclipse Jetty](https://eclipse.dev/jetty/).

There are a small number of older parts of GoCD rendered server-side within [JRuby](https://www.jruby.org/) on [Rails](https://rubyonrails.org/) which utilise some legacy plain JavaScript with [JQuery](https://jquery.com/). GoCD itself is used to [build GoCD](https://build.gocd.org).

Here is the guide to [setup your development environment](https://developer.gocd.org/current/).

## Contributing

We'd love it if you contributed to GoCD. For information on contributing to this project, please see our [contributor's guide](https://gocd.org/contribute).
A lot of useful information like links to user documentation, design documentation, mailing lists etc. can be found in the [resources](https://gocd.org/community/resources.html) section.

## License

GoCD is an open source project, sponsored by [Thoughtworks, Inc.](https://www.thoughtworks.com) under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
