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

import com.embabel.ux.form.Text;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.Objects;

@JsonClassDescription("Astrological details for a person")
public class Starry {
    @Text(label = "Star sign")
    private final String sign;

    public Starry(String sign) {
        this.sign = sign;
    }

    public String getSign() {
        return sign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Starry starry = (Starry) o;
        return Objects.equals(sign, starry.sign);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sign);
    }

    @Override
    public String toString() {
        return "Starry{" +
                "sign='" + sign + '\'' +
                '}';
    }
}