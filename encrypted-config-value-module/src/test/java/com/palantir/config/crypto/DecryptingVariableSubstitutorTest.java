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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.config.crypto.algorithm.Algorithm;
import com.palantir.config.crypto.util.StringSubstitutionException;
import com.palantir.config.crypto.util.SystemProxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DecryptingVariableSubstitutorTest {

    private static final Algorithm ALGORITHM = Algorithm.RSA;
    private static final KeyPair KEY_PAIR = ALGORITHM.newKeyPair();
    private static String previousProperty;

    private SystemProxy systemProxy;
    private DecryptingVariableSubstitutor substitutor;

    @BeforeClass
    public static void beforeClass() throws IOException {
        previousProperty = System.getProperty(KeyFileUtils.KEY_PATH_PROPERTY);

        Path tempFilePath = Files.createTempDirectory("temp-key-directory").resolve("test.key");
        KeyFileUtils.keyPairToFile(KEY_PAIR, tempFilePath);
        System.setProperty(KeyFileUtils.KEY_PATH_PROPERTY, tempFilePath.toAbsolutePath().toString());
    }

    @Before
    public void before() {
        systemProxy = mock(SystemProxy.class);
        when(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME)).thenReturn("");
        substitutor = new DecryptingVariableSubstitutor(systemProxy);
    }

    @AfterClass
    public static void afterClass() {
        if (previousProperty != null) {
            System.setProperty(KeyFileUtils.KEY_PATH_PROPERTY, previousProperty);
        }
    }

    @Test
    public final void constantsAreNotModified() {
        assertThat(substitutor.replace("abc"), is("abc"));
    }

    @Test
    public final void invalidEncryptedVariablesThrowStringSubstitutionException() {
        try {
            substitutor.replace("${enc:invalid-contents}");
            fail();
        } catch (StringSubstitutionException e) {
            assertThat(e.getValue(), is("enc:invalid-contents"));
            assertThat(e.getField(), is(""));
        }
    }

    @Test
    public final void nonEncryptedVariablesAreNotModified() {
        assertThat(substitutor.replace("${abc}"), is("${abc}"));
    }

    @Test
    public final void variableIsDecryptedWithEnvVar() throws Exception {
        systemProxy = mock(SystemProxy.class);
        String decryptionKey = KEY_PAIR.decryptionKey().toString();
        String encryptionKey = KEY_PAIR.encryptionKey().toString();
        when(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME)).thenReturn(encryptionKey);
        when(systemProxy.getenv(KeyEnvVarUtils.DECRYPTION_KEY_NAME)).thenReturn(decryptionKey);
        substitutor = new DecryptingVariableSubstitutor(systemProxy);
        KeyEnvVarUtils.setSystemProxy(systemProxy);

        assertThat(substitutor.replace("${" + encrypt("abc") + "}"), is("abc"));
    }

    @Test
    public final void variableIsDecrypted() throws Exception {
        assertThat(substitutor.replace("${" + encrypt("abc") + "}"), is("abc"));
    }

    private String encrypt(String value) {
        return ALGORITHM.newEncrypter().encrypt(KEY_PAIR.encryptionKey(), value).toString();
    }
}
