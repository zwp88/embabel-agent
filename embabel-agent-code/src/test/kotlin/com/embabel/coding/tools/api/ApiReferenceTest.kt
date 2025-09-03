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
package com.embabel.coding.tools.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiReferenceTest {

    private lateinit var smallApi: Api
    private lateinit var largeApi: Api
    private lateinit var emptyApi: Api

    @BeforeAll
    fun setup() {
        // Create test data
        val testMethod1 = ApiMethod(
            name = "getName",
            parameters = emptyList(),
            returnType = "String",
            annotations = listOf("org.springframework.web.bind.annotation.GetMapping"),
            comment = "Gets the name of the user"
        )

        val testMethod2 = ApiMethod(
            name = "setName",
            parameters = listOf("String name"),
            returnType = "void",
            annotations = emptyList(),
            comment = null
        )

        val testMethod3 = ApiMethod(
            name = "process",
            parameters = listOf("Object input", "int timeout"),
            returnType = "Result<String>",
            annotations = listOf(
                "org.springframework.transaction.annotation.Transactional",
                "org.springframework.validation.annotation.Validated"
            ),
            comment = "Processes input with timeout"
        )

        val testClass1 = ApiClass(
            name = "UserService",
            packageName = "com.example.service",
            type = "class",
            methods = listOf(testMethod1, testMethod2),
            annotations = listOf("org.springframework.stereotype.Service"),
            superTypes = listOf("BaseService", "UserOperations"),
            comment = "Service for managing users"
        )

        val testClass2 = ApiClass(
            name = "UserController",
            packageName = "com.example.controller",
            type = "class",
            methods = listOf(testMethod3),
            annotations = listOf("org.springframework.web.bind.annotation.RestController"),
            superTypes = emptyList(),
            comment = null
        )

        val testInterface = ApiClass(
            name = "UserRepository",
            packageName = "com.example.repository",
            type = "interface",
            methods = listOf(testMethod1),
            annotations = emptyList(),
            superTypes = listOf("JpaRepository<User, Long>"),
            comment = "Repository for user data"
        )

        val testEnum = ApiClass(
            name = "UserStatus",
            packageName = "com.example.model",
            type = "enum",
            methods = emptyList(),
            annotations = emptyList(),
            superTypes = emptyList(),
            comment = "User status enumeration"
        )

        // Small API (under limit)
        smallApi = Api(
            name = "TestAPI",
            classes = listOf(testClass1, testClass2, testInterface, testEnum),
            totalClasses = 4,
            totalMethods = 4
        )

        // Large API (over default limit of 100)
        val largeClassList = (1..150).map { index ->
            ApiClass(
                name = "GeneratedClass$index",
                packageName = "com.generated.package$index",
                type = "class",
                methods = listOf(
                    ApiMethod(
                        name = "method$index",
                        parameters = listOf("String param"),
                        returnType = "void"
                    )
                )
            )
        }

        largeApi = Api(
            name = "LargeTestAPI",
            classes = largeClassList,
            totalClasses = 150,
            totalMethods = 150
        )

        // Empty API
        emptyApi = Api(
            name = "EmptyAPI",
            classes = emptyList(),
            totalClasses = 0,
            totalMethods = 0
        )
    }

    @Nested
    inner class ConstructorAndPropertiesTests {

        @Test
        fun `should initialize with default class limit`() {
            val apiRef = ApiReference(smallApi)

            assertEquals("TestAPI", apiRef.name)
            assertEquals("API reference for TestAPI with 4 classes and 4 methods.", apiRef.description)
        }

        @Test
        fun `should initialize with custom class limit`() {
            val apiRef = ApiReference(smallApi, classLimit = 50)

            assertEquals("TestAPI", apiRef.name)
            assertEquals("API reference for TestAPI with 4 classes and 4 methods.", apiRef.description)
        }

        @Test
        fun `should handle empty API`() {
            val apiRef = ApiReference(emptyApi)

            assertEquals("EmptyAPI", apiRef.name)
            assertEquals("API reference for EmptyAPI with 0 classes and 0 methods.", apiRef.description)
        }
    }

    @Nested
    inner class ContributionTests {

        @Test
        fun `should return full contribution for small API`() {
            val apiRef = ApiReference(smallApi)
            val contribution = apiRef.contribution()

            assertTrue(contribution.contains("The following is an API reference for 4 classes and 4 methods"))
            assertTrue(contribution.contains("Use this reference to answer questions about the API"))
            assertTrue(contribution.contains("findClassSignature"))
            assertTrue(contribution.contains("findPackageSignature"))
            assertTrue(contribution.contains("com.example.service.UserService"))
            assertTrue(contribution.contains("com.example.controller.UserController"))
        }

        @Test
        fun `should return limited contribution for large API`() {
            val apiRef = ApiReference(largeApi)
            val contribution = apiRef.contribution()

            assertTrue(contribution.contains("API reference is too large to include here"))
            assertTrue(contribution.contains("contains 150 classes and 150 methods"))
            assertTrue(contribution.contains("Use the tools `findClassSignature` and `findPackageSignature`"))
            assertFalse(contribution.contains("com.generated.package"))
        }

        @Test
        fun `should respect custom class limit`() {
            val apiRef = ApiReference(smallApi, classLimit = 2)
            val contribution = apiRef.contribution()

            assertTrue(contribution.contains("API reference is too large to include here"))
            assertTrue(contribution.contains("contains 4 classes and 4 methods"))
        }

        @Test
        fun `should handle empty API contribution`() {
            val apiRef = ApiReference(emptyApi)
            val contribution = apiRef.contribution()

            assertTrue(contribution.contains("The following is an API reference for 0 classes and 0 methods"))
            assertTrue(contribution.contains("API Reference - 0 classes, 0 methods"))
        }
    }

    @Nested
    inner class FormatAsTextTests {

        @Test
        fun `should format API as text with header`() {
            val apiRef = ApiReference(smallApi)
            val formatted = apiRef.formatAsText()

            assertTrue(formatted.startsWith("API Reference - 4 classes, 4 methods"))
            assertTrue(formatted.contains("com.example.service.UserService"))
            assertTrue(formatted.contains("com.example.controller.UserController"))
            assertTrue(formatted.contains("com.example.repository.UserRepository (interface)"))
            assertTrue(formatted.contains("com.example.model.UserStatus (enum)"))
        }

        @Test
        fun `should sort classes by fully qualified name`() {
            val apiRef = ApiReference(smallApi)
            val formatted = apiRef.formatAsText()

            val lines = formatted.split("\n")
            val classHeaders = lines.filter { it.matches(Regex("com\\.example\\.[a-z]+\\.[A-Z][a-zA-Z]+.*")) }

            // Should be sorted: controller, model, repository, service
            assertTrue(classHeaders[0].contains("com.example.controller.UserController"))
            assertTrue(classHeaders[1].contains("com.example.model.UserStatus"))
            assertTrue(classHeaders[2].contains("com.example.repository.UserRepository"))
            assertTrue(classHeaders[3].contains("com.example.service.UserService"))
        }

        @Test
        fun `should handle empty API formatting`() {
            val apiRef = ApiReference(emptyApi)
            val formatted = apiRef.formatAsText()

            assertEquals("API Reference - 0 classes, 0 methods\n\n", formatted)
        }
    }

    @Nested
    inner class FindClassSignatureTests {

        @Test
        fun `should find existing class by FQN`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("com.example.service.UserService")

            assertTrue(signature.contains("com.example.service.UserService"))
            assertTrue(signature.contains("Service for managing users"))
            assertTrue(signature.contains("extends/implements: BaseService, UserOperations"))
            assertTrue(signature.contains("getName(): String @GetMapping"))
            assertTrue(signature.contains("setName(String name): void"))
            assertTrue(signature.contains("Gets the name of the user"))
        }

        @Test
        fun `should find interface class correctly`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("com.example.repository.UserRepository")

            assertTrue(signature.contains("com.example.repository.UserRepository (interface)"))
            assertTrue(signature.contains("Repository for user data"))
            assertTrue(signature.contains("extends/implements: JpaRepository<User, Long>"))
        }

        @Test
        fun `should find enum class correctly`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("com.example.model.UserStatus")

            assertTrue(signature.contains("com.example.model.UserStatus (enum)"))
            assertTrue(signature.contains("User status enumeration"))
            assertFalse(signature.contains("extends/implements"))
        }

        @Test
        fun `should return not found message for non-existent class`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("com.example.NonExistentClass")

            assertEquals("Class not found: com.example.NonExistentClass", signature)
        }

        @Test
        fun `should handle empty FQN`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("")

            assertEquals("Class not found: ", signature)
        }

        @Test
        fun `should handle null-like input`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureByFqn("null")

            assertEquals("Class not found: null", signature)
        }
    }

    @Nested
    inner class FindClassSignatureBySimpleNameTests {

        @Test
        fun `should find unique class by simple name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("UserService")

            assertTrue(signature.contains("com.example.service.UserService"))
            assertTrue(signature.contains("Service for managing users"))
            assertTrue(signature.contains("extends/implements: BaseService, UserOperations"))
            assertTrue(signature.contains("getName(): String @GetMapping"))
        }

        @Test
        fun `should find interface by simple name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("UserRepository")

            assertTrue(signature.contains("com.example.repository.UserRepository (interface)"))
            assertTrue(signature.contains("Repository for user data"))
        }

        @Test
        fun `should find enum by simple name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("UserStatus")

            assertTrue(signature.contains("com.example.model.UserStatus (enum)"))
            assertTrue(signature.contains("User status enumeration"))
        }

        @Test
        fun `should return not found for non-existent simple name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("NonExistentClass")

            assertEquals("Class not found: NonExistentClass", signature)
        }

        @Test
        fun `should handle empty simple name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("")

            assertEquals("Class not found: ", signature)
        }

        @Test
        fun `should handle multiple classes with same simple name`() {
            // Create two classes with same simple name but different packages
            val userModel = ApiClass(
                name = "User",
                packageName = "com.example.model",
                type = "class"
            )
            val userDto = ApiClass(
                name = "User",
                packageName = "com.example.dto",
                type = "class"
            )

            val api = Api("TestAPI", listOf(userModel, userDto), 2, 0)
            val apiRef = ApiReference(api)
            val signature = apiRef.findClassSignatureBySimpleName("User")

            assertTrue(signature.contains("Multiple classes found with name 'User':"))
            assertTrue(signature.contains("com.example.model.User"))
            assertTrue(signature.contains("com.example.dto.User"))
        }

        @Test
        fun `should work with large API`() {
            val apiRef = ApiReference(largeApi)
            val signature = apiRef.findClassSignatureBySimpleName("GeneratedClass1")

            assertTrue(signature.contains("com.generated.package1.GeneratedClass1"))
        }

        @Test
        fun `should be case sensitive`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findClassSignatureBySimpleName("userservice")

            assertEquals("Class not found: userservice", signature)
        }
    }

    @Nested
    inner class FindPackageSignatureTests {

        @Test
        fun `should find existing package with multiple classes`() {
            // Add another class to the same package
            val extraClass = ApiClass(
                name = "UserDto",
                packageName = "com.example.service",
                type = "class"
            )
            val apiWithExtraClass = Api(
                name = "TestAPI",
                classes = smallApi.classes + extraClass,
                totalClasses = 5,
                totalMethods = 4
            )

            val apiRef = ApiReference(apiWithExtraClass)
            val signature = apiRef.findPackageSignature("com.example.service")

            assertTrue(signature.contains("Package: com.example.service"))
            assertTrue(signature.contains("Classes:"))
            assertTrue(signature.contains("UserDto (class)"))
            assertTrue(signature.contains("UserService (class)"))

            // Check sorting
            val lines = signature.split("\n")
            val classLines = lines.filter { it.trim().startsWith("- ") }
            assertTrue(classLines[0].contains("UserDto"))
            assertTrue(classLines[1].contains("UserService"))
        }

        @Test
        fun `should find package with single class`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findPackageSignature("com.example.controller")

            assertTrue(signature.contains("Package: com.example.controller"))
            assertTrue(signature.contains("Classes:"))
            assertTrue(signature.contains("UserController (class)"))
        }

        @Test
        fun `should return not found message for non-existent package`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findPackageSignature("com.nonexistent.package")

            assertEquals("Package not found: com.nonexistent.package", signature)
        }

        @Test
        fun `should handle empty package name`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findPackageSignature("")

            assertEquals("Package not found: ", signature)
        }

        @Test
        fun `should handle partial package matches correctly`() {
            val apiRef = ApiReference(smallApi)
            val signature = apiRef.findPackageSignature("com.example")

            assertEquals("Package not found: com.example", signature)
        }
    }

    @Nested
    inner class FormatClassTests {

        @Test
        fun `should format class with all features`() {
            val apiRef = ApiReference(smallApi)
            val userServiceClass = smallApi.classes.find { it.name == "UserService" }!!
            val formatted = apiRef.formatClass(userServiceClass)

            assertTrue(formatted.contains("com.example.service.UserService"))
            assertTrue(formatted.contains("// Service for managing users"))
            assertTrue(formatted.contains("extends/implements: BaseService, UserOperations"))
            assertTrue(formatted.contains("// Gets the name of the user"))
            assertTrue(formatted.contains("getName(): String @GetMapping"))
            assertTrue(formatted.contains("setName(String name): void"))
        }

        @Test
        fun `should format interface with type indicator`() {
            val apiRef = ApiReference(smallApi)
            val repositoryClass = smallApi.classes.find { it.name == "UserRepository" }!!
            val formatted = apiRef.formatClass(repositoryClass)

            assertTrue(formatted.contains("com.example.repository.UserRepository (interface)"))
            assertTrue(formatted.contains("// Repository for user data"))
            assertTrue(formatted.contains("extends/implements: JpaRepository<User, Long>"))
        }

        @Test
        fun `should format enum without extends implements when empty`() {
            val apiRef = ApiReference(smallApi)
            val enumClass = smallApi.classes.find { it.name == "UserStatus" }!!
            val formatted = apiRef.formatClass(enumClass)

            assertTrue(formatted.contains("com.example.model.UserStatus (enum)"))
            assertTrue(formatted.contains("// User status enumeration"))
            assertFalse(formatted.contains("extends/implements"))
        }

        @Test
        fun `should format class without comment`() {
            // Create a class with no comment and no method comments
            val classWithoutComments = ApiClass(
                name = "EmptyController",
                packageName = "com.example.controller",
                type = "class",
                methods = listOf(
                    ApiMethod(
                        name = "simpleMethod",
                        parameters = listOf("String input"),
                        returnType = "String",
                        annotations = emptyList(),
                        comment = null
                    )
                ),
                annotations = emptyList(),
                superTypes = emptyList(),
                comment = null
            )

            val apiRef = ApiReference(Api("Test", listOf(classWithoutComments), 1, 1))
            val formatted = apiRef.formatClass(classWithoutComments)

            assertTrue(formatted.contains("com.example.controller.EmptyController"))
            assertFalse(formatted.contains("//"))
        }

        @Test
        fun `should format method with multiple annotations`() {
            val method = ApiMethod(
                name = "complexMethod",
                parameters = listOf("String input", "int count"),
                returnType = "ResponseEntity<String>",
                annotations = listOf(
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.security.access.prepost.PreAuthorize",
                    "org.springframework.validation.annotation.Validated"
                ),
                comment = "A complex method with multiple annotations"
            )

            val testClass = ApiClass(
                name = "TestClass",
                packageName = "com.test",
                type = "class",
                methods = listOf(method)
            )

            val apiRef = ApiReference(Api("Test", listOf(testClass), 1, 1))
            val formatted = apiRef.formatClass(testClass)

            assertTrue(formatted.contains("// A complex method with multiple annotations"))
            assertTrue(formatted.contains("complexMethod(String input, int count): ResponseEntity<String> @PostMapping @PreAuthorize @Validated"))
        }

        @Test
        fun `should format method without parameters`() {
            val method = ApiMethod(
                name = "simpleMethod",
                parameters = emptyList(),
                returnType = "void"
            )

            val testClass = ApiClass(
                name = "SimpleClass",
                packageName = "com.simple",
                type = "class",
                methods = listOf(method)
            )

            val apiRef = ApiReference(Api("Simple", listOf(testClass), 1, 1))
            val formatted = apiRef.formatClass(testClass)

            assertTrue(formatted.contains("simpleMethod(): void"))
        }

        @Test
        fun `should handle class with no methods`() {
            val testClass = ApiClass(
                name = "EmptyClass",
                packageName = "com.empty",
                type = "class",
                methods = emptyList()
            )

            val apiRef = ApiReference(Api("Empty", listOf(testClass), 1, 0))
            val formatted = apiRef.formatClass(testClass)

            assertTrue(formatted.contains("com.empty.EmptyClass"))
            // Should not have method lines
            val lines = formatted.split("\n")
            assertFalse(lines.any { it.trim().contains("():") || it.trim().contains("(") && it.trim().contains("):") })
        }
    }

    @Nested
    inner class EdgeCasesAndIntegrationTests {

        @Test
        fun `should handle API with classes having same name in different packages`() {
            val class1 = ApiClass(
                name = "User",
                packageName = "com.example.model",
                type = "class"
            )

            val class2 = ApiClass(
                name = "User",
                packageName = "com.example.dto",
                type = "class"
            )

            val api = Api("DuplicateNames", listOf(class1, class2), 2, 0)
            val apiRef = ApiReference(api)

            // Both should be findable by FQN
            val modelUser = apiRef.findClassSignatureByFqn("com.example.model.User")
            val dtoUser = apiRef.findClassSignatureByFqn("com.example.dto.User")

            assertTrue(modelUser.contains("com.example.model.User"))
            assertTrue(dtoUser.contains("com.example.dto.User"))
            assertNotEquals(modelUser, dtoUser)
        }

        @Test
        fun `should maintain consistent behavior with large API operations`() {
            val apiRef = ApiReference(largeApi)

            // Should still be able to find specific classes
            val classSignature = apiRef.findClassSignatureByFqn("com.generated.package1.GeneratedClass1")
            assertTrue(classSignature.contains("com.generated.package1.GeneratedClass1"))

            // Should still be able to find packages
            val packageSignature = apiRef.findPackageSignature("com.generated.package1")
            assertTrue(packageSignature.contains("Package: com.generated.package1"))
            assertTrue(packageSignature.contains("GeneratedClass1"))
        }

        @Test
        fun `should handle special characters in names and comments`() {
            val method = ApiMethod(
                name = "processXML",
                parameters = listOf("String xmlData", "Map<String, Object> options"),
                returnType = "Result<XML>",
                comment = "Processes XML data with special chars: <>&\"'"
            )

            val testClass = ApiClass(
                name = "XMLProcessor",
                packageName = "com.xml.util",
                type = "class",
                methods = listOf(method),
                comment = "Handles XML processing & validation"
            )

            val api = Api("XMLTest", listOf(testClass), 1, 1)
            val apiRef = ApiReference(api)

            val formatted = apiRef.formatClass(testClass)
            assertTrue(formatted.contains("XMLProcessor"))
            assertTrue(formatted.contains("Handles XML processing & validation"))
            assertTrue(formatted.contains("Processes XML data with special chars: <>&\"'"))
            assertTrue(formatted.contains("processXML(String xmlData, Map<String, Object> options): Result<XML>"))
        }

        @Test
        fun `should handle deeply nested generic types`() {
            val method = ApiMethod(
                name = "complexGeneric",
                parameters = listOf("Map<String, List<Optional<CompletableFuture<ResponseEntity<List<User>>>>>>"),
                returnType = "CompletableFuture<ResponseEntity<Map<String, List<Optional<Result<String>>>>>>"
            )

            val testClass = ApiClass(
                name = "GenericClass",
                packageName = "com.generic",
                type = "class",
                methods = listOf(method)
            )

            val api = Api("GenericTest", listOf(testClass), 1, 1)
            val apiRef = ApiReference(api)

            val formatted = apiRef.formatClass(testClass)
            assertTrue(formatted.contains("complexGeneric"))
            assertTrue(formatted.contains("Map<String, List<Optional<CompletableFuture<ResponseEntity<List<User>>>>>>"))
            assertTrue(formatted.contains("CompletableFuture<ResponseEntity<Map<String, List<Optional<Result<String>>>>>>"))
        }
    }
}
