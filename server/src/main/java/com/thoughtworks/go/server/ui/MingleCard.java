/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.MingleConfig;

/**
 * @understands a mingle card reference
 */
public class MingleCard {
    private final MingleConfig mingleConfig;
    private final String cardNumber;

    public MingleCard(MingleConfig mingleConfig, String cardNumber) {
        this.mingleConfig = mingleConfig;
        this.cardNumber = cardNumber;
    }

    public MingleConfig getMingleConfig() {
        return mingleConfig;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MingleCard that = (MingleCard) o;

        if (cardNumber != null ? !cardNumber.equals(that.cardNumber) : that.cardNumber != null) {
            return false;
        }
        if (mingleConfig != null ? !mingleConfig.equals(that.mingleConfig) : that.mingleConfig != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mingleConfig != null ? mingleConfig.hashCode() : 0;
        result = 31 * result + (cardNumber != null ? cardNumber.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "MingleCard{" +
                "mingleConfig=" + mingleConfig +
                ", cardNumber='" + cardNumber + '\'' +
                '}';
    }
}
