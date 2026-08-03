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
package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The set of path patterns to restrict a git working copy to, one per line, in the same
 * gitignore-style syntax accepted by {@code git sparse-checkout set --no-cone}. This is the git
 * analogue of a Perforce client view, and is modelled the same way as {@link
 * com.thoughtworks.go.config.materials.perforce.P4MaterialViewConfig}.
 */
@ConfigTag("sparseCheckout")
public class GitSparseCheckoutConfig implements Serializable, Validatable {
    @ConfigValue(requireCdata = true)
    @ValidationErrorKey(value = GitMaterialConfig.SPARSE_CHECKOUT)
    private String value;

    private final ConfigErrors configErrors = new ConfigErrors();

    public GitSparseCheckoutConfig() {
    }

    public GitSparseCheckoutConfig(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * @return the configured patterns, one per line, with blank lines and surrounding whitespace
     * discarded; empty when nothing meaningful is configured.
     */
    public List<String> patterns() {
        if (isBlank(value)) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(value, ((GitSparseCheckoutConfig) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
