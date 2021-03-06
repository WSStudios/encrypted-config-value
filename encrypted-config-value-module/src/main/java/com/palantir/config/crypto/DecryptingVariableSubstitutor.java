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
import com.palantir.config.crypto.util.StringSubstitutionException;
import com.palantir.config.crypto.util.SystemProxy;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

public final class DecryptingVariableSubstitutor extends StrSubstitutor {
    public DecryptingVariableSubstitutor() {
        super(new DecryptingStringLookup(new SystemProxy()));
    }

    public DecryptingVariableSubstitutor(SystemProxy systemProxy) {
        super(new DecryptingStringLookup(systemProxy));
    }

    private static final class DecryptingStringLookup extends StrLookup<String> {
        private final SystemProxy systemProxy;

        DecryptingStringLookup(SystemProxy systemProxy) {
            this.systemProxy = systemProxy;
        }

        @Override
        public String lookup(String encryptedValue) {
            if (!EncryptedValue.isEncryptedValue(encryptedValue)) {
                return null;
            }

            try {
                if (Strings.isNullOrEmpty(systemProxy.getenv(KeyEnvVarUtils.ENCRYPTION_KEY_NAME))) {
                    return KeyFileUtils.decryptUsingDefaultKeys(EncryptedValue.fromString(encryptedValue));
                } else {
                    return KeyEnvVarUtils.decryptUsingEnvironmentKeys(EncryptedValue.fromString(encryptedValue));
                }
            } catch (RuntimeException e) {
                throw new StringSubstitutionException(e, encryptedValue);
            }
        }
    }
}
