# Rack::Test
[<img src="https://travis-ci.org/rack-test/rack-test.svg?branch=master" />](https://travis-ci.org/rack-test/rack-test)
[<img src="https://codeclimate.com/github/rack-test/rack-test.png" />](https://codeclimate.com/github/rack-test/rack-test)
[<img src="https://codeclimate.com/github/rack-test/rack-test/coverage.png" />](https://codeclimate.com/github/rack-test/rack-test)

Code: https://github.com/rack-test/rack-test

## Description

Rack::Test is a small, simple testing API for Rack apps. It can be used on its
own or as a reusable starting point for Web frameworks and testing libraries
to build on.

## Features

* Maintains a cookie jar across requests
* Easily follow redirects when desired
* Set request headers to be used by all subsequent requests
* Small footprint. Approximately 200 LOC

## Examples
```ruby
require "test/unit"
require "rack/test"

class HomepageTest < Test::Unit::TestCase
  include Rack::Test::Methods

  def app
    app = lambda { |env| [200, {'Content-Type' => 'text/plain'}, ['All responses are OK']] }
    builder = Rack::Builder.new
    builder.run app
  end

  def test_response_is_ok
    get "/"

    assert last_response.ok?
    assert_equal last_response.body, "All responses are OK"
  end
  
  def set_request_headers
    headers 'Accept-Charset', 'utf-8'
    get "/"

    assert last_response.ok?
    assert_equal last_response.body, "All responses are OK"
  end

  def test_response_is_ok_for_other_paths
    get "/other_paths"

    assert last_response.ok?
    assert_equal last_response.body, "All responses are OK"
  end
end
```

If you want to test one app in isolation, you just return that app as shown above. But if you want to test the entire app stack, including middlewares, cascades etc. you need to parse the app defined in config.ru.

```ruby
OUTER_APP = Rack::Builder.parse_file("config.ru").first

class TestApp < Test::Unit::TestCase
  include Rack::Test::Methods

  def app
    OUTER_APP
  end

  def test_root
    get "/"
    assert last_response.ok?
  end
end
```


## Install

To install the latest release as a gem:

`gem install rack-test`

Or via Bundler:

`gem "rack-test", require: "rack/test"`

## Authors

- Contributions from Bryan Helmkamp, Simon Rozet, Pat Nakajima and others
- Much of the original code was extracted from Merb 1.0's request helper

## License
`rack-test` is released under the [MIT License](MIT-LICENSE.txt).

## Contribution

Contributions are welcome. Please make sure to:

* Use a regular forking workflow
* Write tests for the new or changed behaviour
* Provide an explanation/motivation in your commit message / PR message
* Ensure History.txt is updated

## Releasing

* Ensure History.txt is up-to-date
* Bump VERSION in lib/rack/test/version.rb
* bundle exec thor :release
