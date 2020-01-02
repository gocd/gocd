/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

public class GoConfigMigrationResult {
    enum Validity {FAILED_UPGRADE, SUCCESS, UNEXPECTED_FAILURE }

    private Validity validity = Validity.UNEXPECTED_FAILURE;
    private final String message;

    public GoConfigMigrationResult(Validity validity, String message) {
        this.validity = validity;
        this.message = message;
    }

    public static GoConfigMigrationResult failedToUpgrade(String message) {
        return new GoConfigMigrationResult(Validity.FAILED_UPGRADE, message);
    }

    public static GoConfigMigrationResult success() {
        return new GoConfigMigrationResult(Validity.SUCCESS, null);
    }

    public static GoConfigMigrationResult unexpectedFailure(String message) {
        return new GoConfigMigrationResult(Validity.UNEXPECTED_FAILURE, message);
    }

    public boolean isUpgradeFailure() {
        return Validity.FAILED_UPGRADE.equals(validity);
    }

    public String message() {
        return message;
    }
}
