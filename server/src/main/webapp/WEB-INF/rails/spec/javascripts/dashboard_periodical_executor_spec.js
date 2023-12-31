/*
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe("dashboard_periodical_executor", function () {
  var dashboard_periodical_executor = new DashboardPeriodicalExecutor('pipelineStatus.json');

  beforeEach(function () {
    contextPath = "/go";
    setFixtures(`
      <div id="project1_profile">
        <div id="control_panel">
          <a id="project1_forcebuild"></a>
          <a id="project1_config_panel"></a>
          <a id="project1_all_builds"></a>
          <a id="project1_all_successful_builds"></a>
        </div>
      </div>`);
    dashboard_periodical_executor.clean();
  });

  afterEach(function () {
    contextPath = null;
  });

  it("test_should_call_observer_notify_when_success", function () {
    spyOn($, "ajax").and.callFake(function (options) {
      options.success([1, 2, 3]);
    });

    var invoked = false;
    var fakeObserver = {
      notify: function () {
        invoked = true;
      }
    };
    dashboard_periodical_executor.clean();
    dashboard_periodical_executor.register(fakeObserver);
    expect(dashboard_periodical_executor.observers.length).toBe(1);
    dashboard_periodical_executor.start();
    expect(invoked).toBe(true);
  });

  it("test_should_call_redirect_to_login_when_response_header_is_401", function () {
    var invoked = false;
    spyOn($, "ajax").and.callFake(function (options) {
      options.statusCode[401]();
    });

    dashboard_periodical_executor.redirectToLoginPage = function () {
      invoked = true;
    };

    dashboard_periodical_executor.start();
    dashboard_periodical_executor.fireNow();

    expect(invoked).toBe(true);
  });

  it("test_should_not_call_observer_when_executor_is_paused", function () {
    spyOn($, "ajax").and.callFake(function (options) {
      options.success([1, 2, 3.4]);
    });
    var invoked = false;
    var fakeOb = {
      notify: function () {
        invoked = true;
      }
    };
    dashboard_periodical_executor.clean();
    dashboard_periodical_executor.pause();
    dashboard_periodical_executor.register(fakeOb);
    expect(dashboard_periodical_executor.observers.length).toBe(1);
    dashboard_periodical_executor.start();
    dashboard_periodical_executor.fireNow();

    expect(invoked).toBe(false);
    dashboard_periodical_executor.resume();
    dashboard_periodical_executor.fireNow();
    expect(invoked).toBe(true);
  });

  it("test_should_have_default_path_when_there_is_no_context_path", function () {
    dashboard_periodical_executor.setUrl('json.json');
    expect(dashboard_periodical_executor.url).toBe('/go/json.json');
  });

  it("test_sn_should_be_expired_after_call_generate_method_2_times", function () {
    var sn = dashboard_periodical_executor.generateSequenceNumber();
    dashboard_periodical_executor.generateSequenceNumber();
    expect(dashboard_periodical_executor.isSequenceNumberValid(sn)).toBe(false);
  });

  it("test_should_remove_all_observers_after_clean", function () {
    var invoked = false;
    var fakeOb = {
      notify: function () {
        invoked = true;
      }
    };
    dashboard_periodical_executor.clean();
    expect(dashboard_periodical_executor.observers.length).toBe(0);

    dashboard_periodical_executor.register(fakeOb);
    dashboard_periodical_executor.register(fakeOb);
    dashboard_periodical_executor.register(fakeOb);
    dashboard_periodical_executor.register(fakeOb);

    expect(dashboard_periodical_executor.observers.length).toBe(4);
    dashboard_periodical_executor.clean();
    expect(dashboard_periodical_executor.observers.length).toBe(0);
  });

  it("test_should_remove_observer_when_unregister", function () {
    var invoked = false;
    var fakeOb = {
      notify: function () {
        invoked = true;
      }
    };
    dashboard_periodical_executor.clean();
    expect(dashboard_periodical_executor.observers.length).toBe(0);

    dashboard_periodical_executor.register(fakeOb);
    dashboard_periodical_executor.register(fakeOb);

    expect(dashboard_periodical_executor.observers.length).toBe(2);
    dashboard_periodical_executor.unregister(fakeOb);
    expect(dashboard_periodical_executor.observers.length).toBe(1);
  });

  it("test_should_register_one_observer", function () {
    expect(dashboard_periodical_executor.observers.length).toBe(0);
    dashboard_periodical_executor.register("fake_observer");
    expect(dashboard_periodical_executor.observers.length).toBe(1);
  });

  it("test_should_register_multiple_observer", function () {
    expect(dashboard_periodical_executor.observers.length).toBe(0);
    dashboard_periodical_executor.register("fake_observer", "fake_observer2");
    expect(dashboard_periodical_executor.observers.length).toBe(2);
  });

  it("test_executor_should_pause_when_pause_condition_is_true", function () {
    spyOn($, "ajax").and.callFake(function (options) {
      options.success({pause: true});
    });

    var invoked = false;

    var fakeOb = {
      notify: function () {
        invoked = true;
      }
    };

    var pausable_dashboard_periodical_executor = new DashboardPeriodicalExecutor('pipelineStatus.json', function (data) {
      return data.pause;
    });
    pausable_dashboard_periodical_executor.start();

    pausable_dashboard_periodical_executor.fireNow();

    pausable_dashboard_periodical_executor.register(fakeOb);
    pausable_dashboard_periodical_executor.start();
    pausable_dashboard_periodical_executor.fireNow();

    expect(true).toBe(pausable_dashboard_periodical_executor.is_paused);
    expect(invoked).toBe(false);
  });

  it("test_should_invoke_notify_method_on_observer", function () {
    var is_invoked = false;
    var observer = {
      notify: function () {
        is_invoked = true;
      }
    };
    dashboard_periodical_executor.register(observer);
    dashboard_periodical_executor._loop_observers({responseText: "{bla:'bla'}"}, 1);
    expect(is_invoked).toBe(true);
  });
});
