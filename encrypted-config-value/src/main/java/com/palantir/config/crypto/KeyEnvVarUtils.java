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

import com.google.common.base.Strings;
import com.palantir.config.crypto.util.SystemProxy;

public final class KeyEnvVarUtils {
    public static final String KEY_VALUE_PROPERTY = "palantir.config.key_value";
    private static SystemProxy systemProxy = new SystemProxy();

    public static void setSystemProxy(SystemProxy systemProxy) {
        KeyEnvVarUtils.systemProxy = systemProxy;
    }

    public static String decryptUsingDefaultKeys(EncryptedValue encryptedValue) {
        KeyPair keyPair = retrieveKeyPairFromEnvVar();
        return encryptedValue.decrypt(keyPair.decryptionKey());
    }

    public static KeyPair retrieveKeyPairFromEnvVar() {
        String encryptionKey = systemProxy.getenv(KEY_VALUE_PROPERTY);
        if (Strings.isNullOrEmpty(encryptionKey)) {
            throw new RuntimeException("Failed to read key");
        }
        KeyWithType encryptionKeyWithType = KeyWithType.fromString(encryptionKey);

        String decryptionKey = systemProxy.getenv(KEY_VALUE_PROPERTY + ".private");
        return grabKeyWithCorrectSymmetry(encryptionKeyWithType, decryptionKey);
    }

    private static KeyPair grabKeyWithCorrectSymmetry(KeyWithType encryptionKeyWithType, String decryptionKey) {
        KeyPair keyPair;
        if (Strings.isNullOrEmpty(decryptionKey)) {
            keyPair = KeyPair.symmetric(encryptionKeyWithType);
        } else {
            KeyWithType decryptionKeyWithType = KeyWithType.fromString(decryptionKey);
            keyPair = KeyPair.of(encryptionKeyWithType, decryptionKeyWithType);
        }
        return keyPair;
    }

    private KeyEnvVarUtils() {
    }
}
