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
package com.thoughtworks.go.listener;

import com.thoughtworks.go.config.CruiseConfig;

import java.lang.reflect.ParameterizedType;

public abstract class EntityConfigChangedListener<T> implements ConfigChangedListener {
    private Class<?> clazz;

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
    }

    public abstract void onEntityConfigChange(T entity);

    private Class<?> getParameterizedClass() {
        if (clazz == null) {
            ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
            clazz = (Class<?>) pt.getActualTypeArguments()[0];
        }
        return clazz;
    }

    public boolean shouldCareAbout(Object entity) {
        if (entity != null) return getParameterizedClass().isAssignableFrom(entity.getClass());
        return false;
    }
}

