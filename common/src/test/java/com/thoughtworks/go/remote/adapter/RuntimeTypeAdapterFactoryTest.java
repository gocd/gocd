/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Originally from Gson project. Modifications copyright Thoughtworks and respective GoCD contributors.
 * This class has been copied from https://github.com/google/gson/blob/main/extras/src/test/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactoryTest.java
 * since the gson-extras library is not officially released or maintained, refer - https://github.com/google/gson/issues/845
 */

package com.thoughtworks.go.remote.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public final class RuntimeTypeAdapterFactoryTest {

    @Test
    public void testRuntimeTypeAdapter() {
        RuntimeTypeAdapterFactory<BillingInstrument> rta = RuntimeTypeAdapterFactory.of(
                BillingInstrument.class)
            .registerSubtype(CreditCard.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(rta)
            .create();

        CreditCard original = new CreditCard("Jesse", 234);
        assertEquals("{\"type\":\"CreditCard\",\"cvv\":234,\"ownerName\":\"Jesse\"}",
            gson.toJson(original, BillingInstrument.class));
        BillingInstrument deserialized = gson.fromJson(
            "{type:'CreditCard',cvv:234,ownerName:'Jesse'}", BillingInstrument.class);
        assertEquals("Jesse", deserialized.ownerName);
        assertTrue(deserialized instanceof CreditCard);
    }

    @Test
    public void testRuntimeTypeIsBaseType() {
        TypeAdapterFactory rta = RuntimeTypeAdapterFactory.of(
                BillingInstrument.class)
            .registerSubtype(BillingInstrument.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(rta)
            .create();

        BillingInstrument original = new BillingInstrument("Jesse");
        assertEquals("{\"type\":\"BillingInstrument\",\"ownerName\":\"Jesse\"}",
            gson.toJson(original, BillingInstrument.class));
        BillingInstrument deserialized = gson.fromJson(
            "{type:'BillingInstrument',ownerName:'Jesse'}", BillingInstrument.class);
        assertEquals("Jesse", deserialized.ownerName);
    }

    @Test
    public void testNullBaseType() {
        assertThatThrownBy(() -> RuntimeTypeAdapterFactory.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNullTypeFieldName() {
        assertThatThrownBy(() -> RuntimeTypeAdapterFactory.of(BillingInstrument.class, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNullSubtype() {
        RuntimeTypeAdapterFactory<BillingInstrument> rta = RuntimeTypeAdapterFactory.of(
            BillingInstrument.class);
        assertThatThrownBy(() -> rta.registerSubtype(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNullLabel() {
        RuntimeTypeAdapterFactory<BillingInstrument> rta = RuntimeTypeAdapterFactory.of(
            BillingInstrument.class);
        assertThatThrownBy(() -> rta.registerSubtype(CreditCard.class, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testDuplicateSubtype() {
        RuntimeTypeAdapterFactory<BillingInstrument> rta = RuntimeTypeAdapterFactory.of(
            BillingInstrument.class);
        rta.registerSubtype(CreditCard.class, "CC");
        assertThatThrownBy(() -> rta.registerSubtype(CreditCard.class, "Visa"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("types and labels must be unique");
    }

    @Test
    public void testDuplicateLabel() {
        RuntimeTypeAdapterFactory<BillingInstrument> rta = RuntimeTypeAdapterFactory.of(
            BillingInstrument.class);
        rta.registerSubtype(CreditCard.class, "CC");
        assertThatThrownBy(() -> rta.registerSubtype(BankTransfer.class, "CC"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("types and labels must be unique");
    }

    @Test
    public void testDeserializeMissingTypeField() {
        TypeAdapterFactory billingAdapter = RuntimeTypeAdapterFactory.of(BillingInstrument.class)
            .registerSubtype(CreditCard.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(billingAdapter)
            .create();
        assertThatThrownBy(() -> gson.fromJson("{ownerName:'Jesse'}", BillingInstrument.class))
            .isInstanceOf(JsonParseException.class)
            .hasMessageContaining("type");
    }

    @Test
    public void testDeserializeMissingSubtype() {
        TypeAdapterFactory billingAdapter = RuntimeTypeAdapterFactory.of(BillingInstrument.class)
            .registerSubtype(BankTransfer.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(billingAdapter)
            .create();
        assertThatThrownBy(() -> gson.fromJson("{type:'CreditCard',ownerName:'Jesse'}", BillingInstrument.class))
            .isInstanceOf(JsonParseException.class)
            .hasMessageContaining("CreditCard");
    }

    @Test
    public void testSerializeMissingSubtype() {
        TypeAdapterFactory billingAdapter = RuntimeTypeAdapterFactory.of(BillingInstrument.class)
            .registerSubtype(BankTransfer.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(billingAdapter)
            .create();
        assertThatThrownBy(() -> gson.toJson(new CreditCard("Jesse", 456), BillingInstrument.class))
            .isInstanceOf(JsonParseException.class)
            .hasMessageContaining("type");
    }

    @Test
    public void testSerializeCollidingTypeFieldName() {
        TypeAdapterFactory billingAdapter = RuntimeTypeAdapterFactory.of(BillingInstrument.class, "cvv")
            .registerSubtype(CreditCard.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(billingAdapter)
            .create();
        assertThatThrownBy(() -> gson.toJson(new CreditCard("Jesse", 456), BillingInstrument.class))
            .isInstanceOf(JsonParseException.class)
            .hasMessageContaining("already defines a field named cvv");
    }

    @Test
    public void testSerializeWrappedNullValue() {
        TypeAdapterFactory billingAdapter = RuntimeTypeAdapterFactory.of(BillingInstrument.class)
            .registerSubtype(CreditCard.class)
            .registerSubtype(BankTransfer.class);
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(billingAdapter)
            .create();
        String serialized = gson.toJson(new BillingInstrumentWrapper(null), BillingInstrumentWrapper.class);
        BillingInstrumentWrapper deserialized = gson.fromJson(serialized, BillingInstrumentWrapper.class);
        assertNull(deserialized.instrument);
    }

    static class BillingInstrumentWrapper {
        BillingInstrument instrument;

        BillingInstrumentWrapper(BillingInstrument instrument) {
            this.instrument = instrument;
        }
    }

    static class BillingInstrument {
        private final String ownerName;

        BillingInstrument(String ownerName) {
            this.ownerName = ownerName;
        }
    }

    static class CreditCard extends BillingInstrument {
        int cvv;

        CreditCard(String ownerName, int cvv) {
            super(ownerName);
            this.cvv = cvv;
        }
    }

    static class BankTransfer extends BillingInstrument {
        int bankAccount;

        BankTransfer(String ownerName, int bankAccount) {
            super(ownerName);
            this.bankAccount = bankAccount;
        }
    }
}