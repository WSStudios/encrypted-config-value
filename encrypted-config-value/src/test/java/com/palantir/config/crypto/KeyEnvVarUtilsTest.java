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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.config.crypto.util.SystemProxy;
import org.junit.Before;
import org.junit.Test;


public class KeyEnvVarUtilsTest {
    private final String asymmetricallyEncryptedString =
            "enc:eyJjaXBoZXJ0ZXh0IjoialR4bjM0SHJkNWM5b1BuYXhQV3VIZ0MrWm5XUG41Tzk4RXo4"
            + "VGhqNmwxeDB1Qmh5Nkk3d0d1aG04ZmZ6dnZKaFgxK2s4cWcxNEhUVTBISW1LY2ZjazhlbGVO"
                    + "QW4zZHhjVnhoaElpRlJaL09uY09wV280a0RKVkN0K0JIYjJzZ1Jsc2dKdHg0ZUpX"
                    + "T2RaYmhmUG5hOEZGZmo0VHBXc01uNExxanJLa1lWd29RMFQwR1hGOEppVlRTbFBO"
                    + "b3hKVDJSNUpxZW1ua3dZRisrWUtJVEkxa0RVZStTcDhzdXRTSUtFc3l5RHAwYmNX"
                    + "YmxJNHpGYmRjM3FiU3Z4bjNtVFZSYUdBRjNwZWlQcktiQVhCZjVUNm9VaU9qb3pt"
                    + "WHJMWENJYXZBaEtoVjVLWlRxazJxMDhSYlc2d2kyUVFUUGZRVWgvUWQweXN1elcr"
                    + "Ymhyb2kyMFZOc0V3PT0iLCJtb2RlIjoiT0FFUCIsInR5cGUiOiJSU0EiLCJtZGYx"
                    + "LWFsZyI6IlNIQS0yNTYiLCJvYWVwLWFsZyI6IlNIQS0yNTYifQ==";
    private final String symmetricallyEncryptedString = "enc:INNv4cGkVF45MLWZhgVZdIsg"
            + "Q4zKvbMoJ978Es3MIKgrtz5eeTuOCLM1vPbQm97ejz2EK6M=";
    private final EncryptedValue asymmetricallyEncryptedValue =
            EncryptedValue.fromString(asymmetricallyEncryptedString);
    private final EncryptedValue symmetricallyEncryptedValue =
            EncryptedValue.fromString(symmetricallyEncryptedString);
    private final String encryptionKey = "RSA-PUB:MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMII"
            + "BCgKCAQEAsX/Vj9sRUtPB+SSBnUYI3gqDNQrUyXbqrg4pYO6fwEO/FIlLouxhv8xYbXZq0Ye"
            + "Dmon12xW2pPnJzWVkOj+wp3GAwAGTBRn+JB1Sgnp8jZ/Ct5N+KrJ5Pcl9l7avJhqWeAgQgfL"
            + "11n+ODmyUnpmYDPI4X+bsPcKwIfNEdvMhV3hYoyIK5alfAYFwVkijvmL0lSTEpmN+KA1WDQj"
            + "PPoDFjfNxj7TXd6/bQkaRrq2e0i0I8FhfkF4uHmWdKQcW0MGog3jMdF/nfZJZV5Bv7cLlBF1"
            + "nbe1GJl/J/2i2++T/gg1WzmFpsVFQmK2YYwViv1+7hJcnl31EaKE9a+ChahKL4QIDAQAB";
    private final String decryptionKey = "RSA-PRIV:MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBK"
            + "gwggSkAgEAAoIBAQCxf9WP2xFS08H5JIGdRgjeCoM1CtTJduquDilg7p/AQ78UiUui7GG/zF"
            + "htdmrRh4OaifXbFbak+cnNZWQ6P7CncYDAAZMFGf4kHVKCenyNn8K3k34qsnk9yX2Xtq8mGp"
            + "Z4CBCB8vXWf44ObJSemZgM8jhf5uw9wrAh80R28yFXeFijIgrlqV8BgXBWSKO+YvSVJMSmY3"
            + "4oDVYNCM8+gMWN83GPtNd3r9tCRpGurZ7SLQjwWF+QXi4eZZ0pBxbQwaiDeMx0X+d9kllXkG"
            + "/twuUEXWdt7UYmX8n/aLb75P+CDVbOYWmxUVCYrZhjBWK/X7uElyeXfURooT1r4KFqEovhAg"
            + "MBAAECggEATMQ04Rqly2S6J76aMCLyAtYZGP+uN9Oue8i0LLAHd2fVZFRc2gHR5NnEBNOKL0"
            + "SkrlmscyxY6dD+bk7Dok2ZYVG9lU0ZAMPVHllwLe47oCTQWgT4NA8sUISsRMlFxv0IxrD2Mr"
            + "2ZhUN1dNeRD2buU+sOZqvA0JU7B/PmUGump+UyGSMCeIsUofijUiC/whj6RgaXKFSQDUTbDP"
            + "n7sZmqua6o3nQ2or0dpkFak8O5XgFHNJ/SDK94YVlTzjIpZAe1slHfQVfn900uhRYrpIk3sJ"
            + "z5hnBvBktu5tt1SGWtdeDb3RJvraI5tvpybrnhiVww/8MuTXFkqiK2Wgc6rSZiYQKBgQD4x7"
            + "26+VBuHbgswxxf4VTaaRwai857JqKut2I/l85gPfPYvkPIufA2ivBVft2PigGyN4yqI0y5z6"
            + "abwiSwABRVG0cxroLjDO4ofTxisOqB6ilhC3lanPlj35SZ/WaNYAhVv7TFO8ZeilVccRp+YD"
            + "24VFTgIctP5py5Wn4ey/gaiwKBgQC2pob2S3US4Fe0DRpUKt4DWS99rTn3gBuah2OTFkhbvp"
            + "vKH0lqcUTOYH7DQfbQegBfRFRIbLF0J9t/7MqiJygSW+oOyR9V83LuuAl6UYnhPknmIMFUB3"
            + "aJgjgBNpwEfV2APr1KWzmFFZrXNfFkheHUHVzQcytAoghEzUUh1BF8wwKBgQCKylRNBV4bsL"
            + "TZNBiWXQ2Ls+wb2zdceRd1RZWoSLa39pgdqTgDucgLhcPCzr4ooCOGG3t8R6k8WF5oswzoq4"
            + "KsYEV9sBARP1t2dzfDD1QlGk/vdglDxiNT1p3+suINqS+9NvBQwKJQh/hLVEYljuef+FxJll"
            + "DoyI1S0utdnwlRNQKBgDjRdtx5PrMfjMja5pzWNPgvr0FWONkQRgX9JfGkld/MXKQ0tV7iW3"
            + "gjtmtBQuk5epIoLFvXTCCJIZQa8jIdIi8L3rS4xgGz2MYABBrD8LNb8Bshh2J/a9V857Ug6s"
            + "nOwd5aJgJSfIM05FUcV7pgl46nj67clNDnVtEoVeAfG02HAoGBAJZlWT5hRRFsU8HIXx+ekP"
            + "CdOEQy4qbej/v9p38kn9eSsqrLEnnrZIX/cyO9DC/5ATAA7mGZ0DMSrlvM48TzsKu/7HXjYR"
            + "AYez/aTzNvd5Ez/sxlnHtiZ14A6bUWvT1Cy/stI0g2cS2WYpC05zQt57eBuvzfIu5Sy9kAwo"
            + "0RTa/P";
    private static final String symmetricKey = "AES:vgwWG0UUo39Hhfru2dD7Nw==";
    private static final String ENCRYPTION_KEY_NAME = "config.encryption.key";
    private static final String DECRYPTION_KEY_NAME = "config.decryption.key";
    private SystemProxy systemProxy;

    @Before
    public void before() {
        systemProxy = mock(SystemProxy.class);
        KeyEnvVarUtils.setSystemProxy(systemProxy);
    }

    @Test
    public void decryptUsingEnvironmentKeysAsymmetric() {
        when(systemProxy.getenv(ENCRYPTION_KEY_NAME)).thenReturn(encryptionKey);
        when(systemProxy.getenv(DECRYPTION_KEY_NAME)).thenReturn(decryptionKey);

        String result = KeyEnvVarUtils.decryptUsingEnvironmentKeys(asymmetricallyEncryptedValue);

        String expected = "foo";
        assertEquals(expected, result);
    }

    @Test
    public void decryptUsingEnvironmentKeysSymmetric() {
        when(systemProxy.getenv(ENCRYPTION_KEY_NAME)).thenReturn(symmetricKey);
        when(systemProxy.getenv(DECRYPTION_KEY_NAME)).thenReturn("");

        String result = KeyEnvVarUtils.decryptUsingEnvironmentKeys(symmetricallyEncryptedValue);

        String expected = "value";
        assertEquals(expected, result);
    }

    @Test
    public void retrieveKeyPairFromEnvVar() {
        when(systemProxy.getenv(ENCRYPTION_KEY_NAME)).thenReturn(encryptionKey);
        when(systemProxy.getenv(DECRYPTION_KEY_NAME)).thenReturn(decryptionKey);

        KeyPair result = KeyEnvVarUtils.retrieveKeyPairFromEnvVar();

        assertEquals(result.encryptionKey().toString(), encryptionKey);
        assertEquals(result.decryptionKey().toString(), decryptionKey);
    }

    @Test(expected = RuntimeException.class)
    public void decryptFailsWhenKeyIsntPresent() {
        when(systemProxy.getenv(ENCRYPTION_KEY_NAME)).thenReturn("");
        when(systemProxy.getenv(DECRYPTION_KEY_NAME)).thenReturn("");

        KeyEnvVarUtils.decryptUsingEnvironmentKeys(symmetricallyEncryptedValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decryptUsingEnvironmentKeysAsymmetricFailsWhenDecryptionKeyMissing() {
        when(systemProxy.getenv(ENCRYPTION_KEY_NAME)).thenReturn(encryptionKey);
        when(systemProxy.getenv(DECRYPTION_KEY_NAME)).thenReturn("");

        KeyEnvVarUtils.decryptUsingEnvironmentKeys(asymmetricallyEncryptedValue);
    }
}
