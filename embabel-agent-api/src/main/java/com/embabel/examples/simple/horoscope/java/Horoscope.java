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
package com.embabel.examples.simple.horoscope.java;

import java.util.Objects;

public class Horoscope {
    private final String summary;

    public Horoscope(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Horoscope horoscope = (Horoscope) o;
        return Objects.equals(summary, horoscope.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary);
    }

    @Override
    public String toString() {
        return "Horoscope{" +
                "summary='" + summary + '\'' +
                '}';
    }
}