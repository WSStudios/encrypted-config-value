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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.palantir.config.crypto.algorithm.Algorithm;
import com.palantir.config.crypto.util.SystemProxy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import net.sourceforge.argparse4j.inf.Namespace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class EncryptConfigValueCommandTest {
    private static final String CHARSET = "UTF8";
    private static final String plaintext = "this is a secret message";
    private SystemProxy systemProxy;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private EncryptConfigValueCommand command;

    private PrintStream originalSystemOut;

    @Before
    public void setUpStreams() throws UnsupportedEncodingException {
        systemProxy = mock(SystemProxy.class);
        command = new EncryptConfigValueCommand(systemProxy);
        originalSystemOut = System.out;
        System.setOut(new PrintStream(outContent, false, CHARSET));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(originalSystemOut);
    }

    private void weEncryptAndPrintAValue(Algorithm algorithm) throws Exception {
        when(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME)).thenReturn("");
        Path tempFilePath = Files.createTempDirectory("temp-key-directory").resolve("test.key");

        KeyPair keyPair = algorithm.newKeyPair();
        KeyFileUtils.keyPairToFile(keyPair, tempFilePath);

        Namespace namespace = new Namespace(ImmutableMap.of(
                EncryptConfigValueCommand.KEYFILE, tempFilePath.toString(),
                EncryptConfigValueCommand.VALUE, plaintext));

        command.run(null, namespace);

        assertOutputEqualsDecryptedValue(keyPair);
    }

    private void weEncryptAndPrintAValueUsingEnvVar(Algorithm algorithm) throws Exception {
        KeyPair keyPair = algorithm.newKeyPair();

        when(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME))
                .thenReturn(keyPair.encryptionKey().toString());
        KeyEnvVarUtils.setSystemProxy(systemProxy);
        if (keyPair.encryptionKey() != keyPair.decryptionKey()) {
            when(systemProxy.getenv(KeyEnvVarUtils.DECRYPTION_KEY_NAME))
                    .thenReturn(keyPair.decryptionKey().toString());
        }

        Namespace namespace = new Namespace(ImmutableMap.of(
                EncryptConfigValueCommand.VALUE, plaintext));

        command.run(null, namespace);

        assertOutputEqualsDecryptedValue(keyPair);
    }

    private void assertOutputEqualsDecryptedValue(KeyPair keyPair) throws UnsupportedEncodingException {
        String output = outContent.toString(CHARSET).trim();

        EncryptedValue configValue = EncryptedValue.fromString(output);
        KeyWithType decryptionKey = keyPair.decryptionKey();
        String decryptedValue = configValue.decrypt(decryptionKey);

        assertThat(decryptedValue, is(plaintext));
    }

    @Test
    public void weEncryptAndPrintAValueUsingAes() throws Exception {
        weEncryptAndPrintAValue(Algorithm.AES);
    }

    @Test
    public void weEncryptAndPrintAValueUsingRsa() throws Exception {
        weEncryptAndPrintAValue(Algorithm.RSA);
    }

    @Test(expected = NoSuchFileException.class)
    public void weFailIfTheKeyfileDoesNotExist() throws Exception {
        Path tempFilePath = Files.createTempDirectory("temp-key-directory").resolve("test.key");

        Namespace namespace = new Namespace(ImmutableMap.of(
                EncryptConfigValueCommand.KEYFILE, tempFilePath.toString(),
                EncryptConfigValueCommand.VALUE, plaintext));

        command.run(null, namespace);
    }

    @Test
    public void weEncryptAndPrintAValueUsingAesFromEnvVar() throws Exception {
        weEncryptAndPrintAValueUsingEnvVar(Algorithm.AES);
    }

    @Test
    public void weEncryptAndPrintAValueUsingRsaFromEnvVar() throws Exception {
        weEncryptAndPrintAValueUsingEnvVar(Algorithm.RSA);
    }

    @Test(expected = NullPointerException.class)
    public void weFailIfNeitherVariableNorKeyfileExists() throws Exception {
        when(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME)).thenReturn(null);

        Namespace namespace = new Namespace(ImmutableMap.of(
                EncryptConfigValueCommand.VALUE, plaintext));

        command.run(null, namespace);
    }
}
