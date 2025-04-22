package com.embabel.agent.experimental.hitl

object Forms {
    
    fun singleField(
        title: String,
        label: String,
        placeholder: String = ""
    ): Form {
        return Form(
            title = title,
            controls = listOf(
                TextField(
                    label = label,
                    placeholder = placeholder,
                    required = true,
                ),
            ),
        )
    }
}