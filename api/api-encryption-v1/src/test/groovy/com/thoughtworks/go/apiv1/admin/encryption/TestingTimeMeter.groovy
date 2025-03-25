/*
 * Copyright Thoughtworks, Inc.
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

import io.github.bucket4j.TimeMeter

import java.util.concurrent.TimeUnit

class TestingTimeMeter implements TimeMeter {

  long time
  boolean useSystemClock

  TestingTimeMeter(long time) {
    this.time = time
  }

  TestingTimeMeter() {
    this(System.nanoTime())
    freeze()
  }

  @Override
  boolean isWallClockBased() {
    false
  }

  @Override
  long currentTimeNanos() {
    useSystemClock ? System.nanoTime() : this.time
  }

  TimeMeter useSystemClock() {
    this.useSystemClock = true
    return this
  }


  TimeMeter freeze() {
    this.useSystemClock = false
    return this
  }

  TimeMeter forward(long duration, TimeUnit unit) {
    time += unit.toNanos(duration)
    return this
  }
}
