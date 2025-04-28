/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.api.annotation;

import com.embabel.agent.core.hitl.Awaitable;

/**
 * Java syntax sugar for HITL
 *
 * @see com.embabel.agent.api.annotation.WaitKt
 */
public class WaitFor {

    private WaitFor() {
        // Prevent instantiation
    }

    /**
     * @see com.embabel.agent.api.annotation.WaitKt#fromForm(String, Class)
     */
    public static <T> T formSubmission(String title, Class<T> clazz) {
        return com.embabel.agent.api.annotation.WaitKt.fromForm(title, clazz);
    }

    public static <P> P confirmation(P what, String description) {
        return com.embabel.agent.api.annotation.WaitKt.confirm(what, description);
    }

    public static <P> P awaitable(Awaitable<P, ?> awaitable) {
        return com.embabel.agent.api.annotation.WaitKt.waitFor(awaitable);
    }


}
