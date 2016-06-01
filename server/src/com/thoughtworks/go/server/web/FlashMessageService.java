/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.thoughtworks.go.presentation.FlashMessageModel;
import org.springframework.stereotype.Service;

/**
 * @understands persisting and retrieving messages from flash(session store, but deleted on read)
 */
@Service
public class FlashMessageService {
    private static ThreadLocal<Flash> requestFlash = new ThreadLocal<>();

    public String add(FlashMessageModel message) {
        String key = UUID.randomUUID().toString();
        getFlash().put(key, message);
        return key;
    }

    private Flash getFlash() {
        Flash flash = requestFlash.get();
        if (flash == null) {
            throw new NullPointerException("No flash context found, this call should only be made within a request.");
        }
        return flash;
    }

    public FlashMessageModel get(String key) {
        return getFlash().get(key);
    }

    public static void useFlash(Flash newFlash) {
        requestFlash.set(newFlash);
    }

    public static final class Flash {
        Map<String, FlashMessageModel> messages = new HashMap<>();

        public void put(String key, FlashMessageModel message) {
            messages.put(key, message);
        }

        public FlashMessageModel get(String key) {
            return messages.remove(key);
        }
    }
}
