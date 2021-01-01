/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.admin.encryption

import com.google.common.base.Ticker

import java.util.concurrent.TimeUnit

class TestingTicker extends Ticker {

  long time
  boolean useSystemClock

  TestingTicker(long time) {
    this.time = time
  }

  TestingTicker() {
    this(System.nanoTime())
    freeze()
  }


  @Override
  long read() {
    useSystemClock ? System.nanoTime() : this.time
  }

  Ticker useSystemClock() {
    this.useSystemClock = true
    return this
  }


  Ticker freeze() {
    this.useSystemClock = false
    return this
  }

  Ticker forward(long duration, TimeUnit unit) {
    time += unit.toNanos(duration)
    return this
  }
}
