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
