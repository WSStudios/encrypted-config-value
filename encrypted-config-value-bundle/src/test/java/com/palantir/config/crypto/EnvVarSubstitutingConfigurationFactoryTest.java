/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.config.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.google.common.collect.Maps;
import com.palantir.config.crypto.jackson.JsonNodeStringReplacer;
import com.palantir.config.crypto.util.Person;
import com.palantir.config.crypto.util.TestConfig;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnvVarSubstitutingConfigurationFactoryTest {
    private static SubstitutingConfigurationFactory<TestConfig> factory;
    private static Map<String, String> previousEnv;

    // https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    @SuppressWarnings("unchecked")
    protected static void updateEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

    @BeforeClass
    public static void before() throws Exception {
        previousEnv = System.getenv();
        Map<String, String> env = Maps.newHashMap(previousEnv);
        env.put(KeyEnvVarUtils.KEY_VALUE_PROPERTY, "AES:vgwWG0UUo39Hhfru2dD7Nw==");
        EnvVarSubstitutingConfigurationFactoryTest.updateEnv(env);

        factory = new SubstitutingConfigurationFactory(
                TestConfig.class,
                Validators.newValidator(),
                Jackson.newObjectMapper(),
                "",
                new JsonNodeStringReplacer(new DecryptingVariableSubstitutor()));
    }

    @AfterClass
    public static void after() throws Exception {
        if (previousEnv != null) {
            EnvVarSubstitutingConfigurationFactoryTest.updateEnv(previousEnv);
        }
    }

    @Test
    public final void decryptionSucceeds() throws IOException, ConfigurationException {
        TestConfig config = factory.build(new File("src/test/resources/testConfig.yml"));

        assertThat(config.getEncrypted()).isEqualTo("value");
        assertThat(config.getUnencrypted()).isEqualTo("value");
        assertThat(config.getEncryptedWithSingleQuote()).isEqualTo("don't use quotes");
        assertThat(config.getEncryptedWithDoubleQuote()).isEqualTo("double quote is \"");
        assertThat(config.getEncryptedMalformedYaml()).isEqualTo("[oh dear");
        assertThat(config.getArrayWithSomeEncryptedValues())
                .containsExactly("value", "value", "other value", "[oh dear");
        assertThat(config.getPojoWithEncryptedValues()).isEqualTo(Person.of("some-user", "value"));
    }

    @Test
    public final void decryptionFailsWithNiceMessage() throws IOException, ConfigurationException {
        try {
            factory.build(new File("src/test/resources/testConfigWithError.yml"));
            failBecauseExceptionWasNotThrown(ConfigurationDecryptionException.class);
        } catch (ConfigurationDecryptionException e) {
            assertThat(e.getMessage())
                    .contains("src/test/resources/testConfigWithError.yml has the following errors"
                                      .replaceAll("/", Matcher.quoteReplacement(File.separator)));
            assertThat(e.getMessage()).contains(
                    "The value 'enc:ERROR' for field 'arrayWithSomeEncryptedValues[3]' could not be replaced "
                    + "with its unencrypted value");
            assertThat(e.getMessage()).contains(
                    "Underlying error - ");
        }
    }
}
