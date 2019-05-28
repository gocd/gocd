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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.lang.String.join;

/**
 * @understands an environment variable value that will be passed to a job when it is run
 */
@ConfigTag("variable")
public class EnvironmentVariableConfig implements Serializable, Validatable, ParamsAttributeAware, PasswordEncrypter, ConfigOriginTraceable, SecretParamAware {
    public static final String NAME = "name";
    public static final String VALUE = "valueForDisplay";
    public static final String ENCRYPTEDVALUE = "encryptedValue";
    public static final String SECURE = "secure";
    public static final String ISCHANGED = "isChanged";

    @ConfigAttribute(value = "name", optional = false)
    private String name;
    @ConfigAttribute(value = "secure", optional = true)
    private boolean isSecure = false;
    @ConfigSubtag
    private VariableValueConfig value;
    @ConfigSubtag
    private EncryptedVariableValueConfig encryptedValue;

    private final ConfigErrors configErrors = new ConfigErrors();
    private final GoCipher goCipher;
    private ConfigOrigin origin;

    public EnvironmentVariableConfig() {
        this.goCipher = new GoCipher();
    }

    public EnvironmentVariableConfig(String name, String value) {
        this(new GoCipher(), name, value, false);
    }

    public EnvironmentVariableConfig(GoCipher goCipher, String name, String value, boolean isSecure) {
        this(goCipher);
        this.name = name;
        this.isSecure = isSecure;
        setValue(value);
    }

    public EnvironmentVariableConfig(GoCipher goCipher, String name, String encryptedValue) {
        this(goCipher);
        this.name = name;
        this.isSecure = true;
        this.setEncryptedValue(new EncryptedVariableValueConfig(encryptedValue));
    }

    public EnvironmentVariableConfig(EnvironmentVariableConfig variable) {
        this(variable.goCipher, variable.name, variable.getValue(), variable.isSecure);
    }

    private String encrypt(String value) {
        try {
            return goCipher.encrypt(value);
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    public EnvironmentVariableConfig(GoCipher goCipher) {
        this.goCipher = goCipher;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnvironmentVariableConfig that = (EnvironmentVariableConfig) o;

        if (isSecure != that.isSecure) {
            return false;
        }
        if (!goCipher.passwordEquals(encryptedValue, that.encryptedValue)) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isSecure ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + goCipher.passwordHashcode(encryptedValue);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void addTo(EnvironmentVariableContext context) {
        context.setProperty(name, getValue(), isSecure());
    }

    public void addToIfExists(EnvironmentVariableContext context) {
        if (context.hasProperty(name)) {
            addTo(context);
        }
    }

    public void validateName(Map<String, EnvironmentVariableConfig> variableConfigMap, ValidationContext validationContext) {
        String currentVariableName = name.toLowerCase();
        String parentDisplayName = validationContext.getParentDisplayName();
        CaseInsensitiveString parentName = getParentNameFrom(validationContext);
        if (!currentVariableName.trim().equals(currentVariableName)) {
            configErrors.add(NAME, String.format("Environment Variable cannot start or end with spaces for %s '%s'.", parentDisplayName, parentName));
            return;
        }
        if (StringUtils.isBlank(currentVariableName)) {
            configErrors.add(NAME, String.format("Environment Variable cannot have an empty name for %s '%s'.", parentDisplayName, parentName));
            return;
        }
        EnvironmentVariableConfig configWithSameName = variableConfigMap.get(currentVariableName);
        if (configWithSameName == null) {
            variableConfigMap.put(currentVariableName, this);
        } else {
            configWithSameName.addNameConflictError(parentDisplayName, parentName);
            this.addNameConflictError(parentDisplayName, parentName);
        }
    }

    private void addNameConflictError(String parentDisplayName, Object parentName) {
        configErrors.add(NAME, String.format("Environment Variable name '%s' is not unique for %s '%s'.", this.name, parentDisplayName, parentName));
    }


    private CaseInsensitiveString getParentNameFrom(ValidationContext validationContext) {
        EnvironmentVariableScope parent = (EnvironmentVariableScope) validationContext.getParent();
        return parent.name();
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        try {
            SecretParams secretParams = SecretParams.parse(getValue());

            if (!secretParams.hasSecretParams()) {
                return;
            }

            final Set<String> missingSecretConfigs = new HashSet<>();
            final Set<String> canNotReferSecretConfigs = new HashSet<>();

            SecretConfigs secretConfigs = validationContext.getCruiseConfig().getSecretConfigs();
            Parent parent = Parent.from(validationContext);

            secretParams.forEach(secretParam -> {
                final SecretConfig secretConfig = secretConfigs.find(secretParam.getSecretConfigId());
                if (secretConfig == null) {
                    missingSecretConfigs.add(secretParam.getSecretConfigId());
                } else {
                    if (parent != null && !secretConfig.canRefer(parent.getType(), parent.getIdentifier())) {
                        canNotReferSecretConfigs.add(secretParam.getSecretConfigId());
                    }
                }
            });

            if (!missingSecretConfigs.isEmpty()) {
                addError(VALUE, String.format("Secret config with ids '%s' does not exist.", String.join(", ", missingSecretConfigs)));
            }

            if (!canNotReferSecretConfigs.isEmpty()) {
                addError(VALUE, format("Secret config with ids '%s' is not allowed to use in %s '%s'.", join(", ", canNotReferSecretConfigs), parent.getTag(), parent.getIdentifier()));
            }

        } catch (Exception e) {
            errors().add(VALUE, String.format("Encrypted value for variable named '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.", getName()));
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setIsSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    public boolean isPlain() {
        return !isSecure();
    }

    @Deprecated
    // prefer using deserialize instead
    public void setValue(String value) {
        if (isSecure) {
            encryptedValue = new EncryptedVariableValueConfig(encrypt(value));
        } else {
            this.value = new VariableValueConfig(value);
        }
    }

    public void setEncryptedValue(String encrypted) {
        this.encryptedValue = new EncryptedVariableValueConfig(encrypted);
    }

    public void setValue(VariableValueConfig value) {
        this.value = value;
    }

    public void setEncryptedValue(EncryptedVariableValueConfig encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public String getValue() {
        if (isSecure) {
            try {
                return goCipher.decrypt(encryptedValue.getValue());
            } catch (CryptoException e) {
                throw new RuntimeException(format("Could not decrypt secure environment variable value for name %s", getName()), e);
            }
        } else {
            return value.getValue();
        }
    }

    public String getDisplayValue() {
        if (isSecure()) return "****";
        return getValue();
    }

    public String getEncryptedValue() {
        return encryptedValue.getValue();
    }

    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        this.name = (String) attributeMap.get(EnvironmentVariableConfig.NAME);
        String value = (String) attributeMap.get(EnvironmentVariableConfig.VALUE);
        if (StringUtils.isBlank(name) && StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(String.format("Need not null/empty name & value %s:%s", this.name, value));
        }
        this.isSecure = BooleanUtils.toBoolean((String) attributeMap.get(EnvironmentVariableConfig.SECURE));
        Boolean isChanged = BooleanUtils.toBoolean((String) attributeMap.get(EnvironmentVariableConfig.ISCHANGED));
        if (isSecure) {
            this.encryptedValue = isChanged ? new EncryptedVariableValueConfig(encrypt(value)) : new EncryptedVariableValueConfig(value);
        } else {
            this.value = new VariableValueConfig(value);
        }
    }

    @PostConstruct
    public void ensureEncrypted() {
        if (isSecure && value != null) {
            encryptedValue = new EncryptedVariableValueConfig(encrypt(value.getValue()));
            value = null;
        }

        if (isSecure && encryptedValue != null) {
            String encryptedValue = this.encryptedValue.getValue();
            setEncryptedValue(goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedValue));
        }
    }

    public void deserialize(String name, String value, boolean isSecure, String encryptedValue) throws CryptoException {
        setName(name);
        setIsSecure(isSecure);

        if (!isSecure && encryptedValue != null) {
            errors().add(ENCRYPTEDVALUE, "You may specify encrypted value only when option 'secure' is true.");
        }

        if (value != null && encryptedValue != null) {
            addError("value", "You may only specify `value` or `encrypted_value`, not both!");
            addError(ENCRYPTEDVALUE, "You may only specify `value` or `encrypted_value`, not both!");
        }

        if (encryptedValue != null) {
            setEncryptedValue(goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedValue));
        }

        if (isSecure) {
            if (value != null) {
                setEncryptedValue(new EncryptedVariableValueConfig(new GoCipher().encrypt(value)));
            }
        } else {
            setValue(new VariableValueConfig(value));
        }
    }

    public String getValueForDisplay() {
        if (isSecure) {
            return getEncryptedValue();
        }
        return value.getValue();
    }

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    public boolean isRemote() {
        return origin != null && !origin.isLocal();
    }

    public void setOrigins(ConfigOrigin origins) {
        this.origin = origins;
    }

    @Override
    public boolean hasSecretParams() {
        return !SecretParams.parse(getValue()).isEmpty();
    }

    @Override
    public SecretParams getSecretParams() {
        return SecretParams.parse(getValue());
    }

    static abstract class Parent<T extends Validatable> {
        private T parent;

        Parent(T parent) {
            this.parent = parent;
        }

        public static Parent from(ValidationContext validationContext) {
            if (validationContext.isWithinPipelines()) {
                return new PipelineParent(validationContext.getPipelineGroup());
            }

            if (validationContext.isWithinEnvironment()) {
                return new EnvironmentParent(validationContext.getEnvironment());
            }

            return null;
        }

        public String getTag() {
            ConfigTag configTag = parent.getClass().getAnnotation(ConfigTag.class);
            if (StringUtils.isNotBlank(configTag.label())) {
                return configTag.label();
            }
            return configTag.value();
        }

        public Class<? extends Validatable> getType() {
            return parent.getClass();
        }

        public T getParent() {
            return parent;
        }

        public abstract String getIdentifier();
    }

    static class PipelineParent extends Parent<PipelineConfigs> {
        PipelineParent(PipelineConfigs pipelineConfigs) {
            super(pipelineConfigs);
        }

        @Override
        public String getIdentifier() {
            return getParent().getGroup();
        }
    }

    static class TemplateParent extends Parent<PipelineTemplateConfig> {
        TemplateParent(PipelineTemplateConfig template) {
            super(template);
        }

        @Override
        public String getIdentifier() {
            return getParent().name().toString();
        }
    }

    static class EnvironmentParent extends Parent<EnvironmentConfig> {
        EnvironmentParent(EnvironmentConfig environment) {
            super(environment);
        }

        @Override
        public String getIdentifier() {
            return getParent().name().toString();
        }
    }
}
