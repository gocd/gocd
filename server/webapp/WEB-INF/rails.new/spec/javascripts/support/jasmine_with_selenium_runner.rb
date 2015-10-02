# Heavily inspired by https://github.com/jasmine/jasmine_selenium_runner.
# Couldn't use jasmine_selenium_runner directly as it depends on sauce-connect which internally depends on cmdparse.
# cmdparse has a dependency on ruby 2.0+ :(
# Unlike maven, bundler does not let you exclude unwanted internal dependencies of a gem as yet (https://github.com/bundler/bundler/issues/2827)
# Hence, resorting to a local version of runner.


class JasmineWithSeleniumRunner
  def initialize(formatter, jasmine_server_url, runner_config)
    @web_driver = Selenium::WebDriver.for((runner_config['browser'] || 'firefox').to_sym, {})
    @formatter = formatter
    @jasmine_server_url = jasmine_server_url
    @result_batch_size = runner_config['batch_size'] || 50
  end

  def run
    web_driver.navigate.to jasmine_server_url
    wait_for_jasmine_to_start
    wait_for_suites_to_finish_running
    formatter.format(get_results)
    formatter.done
  ensure
    web_driver.quit
  end

  private
  attr_reader :formatter, :config, :web_driver, :jasmine_server_url, :result_batch_size

  def jasmine_testing_started?
    web_driver.execute_script "return jsApiReporter && jsApiReporter.started"
  end

  def jasmine_testing_finished?
    web_driver.execute_script "return jsApiReporter && jsApiReporter.finished"
  end

  def wait_for_jasmine_to_start
    started = Time.now
    until jasmine_testing_started? do
      raise "couldn't connect to Jasmine after 120 seconds" if (started + 120 < Time.now)
      sleep 1
    end
  end

  def wait_for_suites_to_finish_running
    puts "Waiting for suite to finish in browser ..."
    until jasmine_testing_finished? do
      sleep 1
    end
  end

  def get_results
    index                = 0
    spec_results         = []
    failed_suite_results = []

    loop do
      slice = results('spec', index)
      spec_results << slice
      index += result_batch_size
      break if slice.size < result_batch_size
    end

    index = 0
    loop do
      slice = results('suite', index)
      failed_suite_results << slice.select(&:failed?)
      index += result_batch_size
      break if slice.size < result_batch_size
    end

    spec_results.flatten + failed_suite_results.flatten
  end

  def results(result_type, starting_index)
    slice = web_driver.execute_script(<<-JS)
          var results = jsApiReporter.#{result_type}Results(#{starting_index}, #{result_batch_size})
          for (var i = 0; i < results.length; i++) {
            var expectations = results[i].failedExpectations;
            if (results[i].passedExpectations) {
              expectations = expectations.concat(results[i].passedExpectations);
            }
            for (var j = 0; j < expectations.length; j++) {
              var expectation = expectations[j];
              try { JSON.stringify(expectation.expected); } catch (e) { expectation.expected = '<circular expected>'; }
              try { JSON.stringify(expectation.actual); } catch (e) { expectation.actual = '<circular actual>'; }
            }
          }
          return results;
    JS
    Jasmine::Result.map_raw_results(slice)
  end
end
