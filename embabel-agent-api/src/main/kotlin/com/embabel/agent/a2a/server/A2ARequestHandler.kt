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
package com.embabel.agent.a2a.server

import com.embabel.agent.a2a.spec.JSONRPCRequest
import com.embabel.agent.a2a.spec.JSONRPCResponse

/**
 * Handles JSON-RPC requests according to the A2A protocol.
 */
interface A2ARequestHandler {

    /**
     * Handle a JSON-RPC request according to the A2A protocol.
     * @param request the JSON-RPC request
     * @return the JSON-RPC response
     */
    fun handleJsonRpc(request: JSONRPCRequest): JSONRPCResponse
}
