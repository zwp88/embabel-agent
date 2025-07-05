package com.embabel.common.util

/**
 * Convenient utility interface for transforming strings.
 */
fun interface StringTransformer {

    fun transform(raw: String): String

    companion object {

        /**
         * Apply transformers in order
         */
        fun transform(raw: String, transformers: List<StringTransformer>): String {
            var transformedContent = raw

            // Run all transformers
            for (transformer in transformers) {
                transformedContent = transformer.transform(transformedContent)
            }
            return transformedContent
        }
    }
}