/*
 * Copyright 2018 Rakuten Inc.
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

package com.rakuten.tech.mobile.manifestconfig.processor

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.tools.JavaFileObject

@RunWith(Parameterized::class)
class ManifestConfigGeneratorHappyPathSpec(private val interfaceName: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should generate config implementation for {0}")
        fun data() = listOf("Minimal", "CustomKeys", "CustomValues").map { arrayOf(it) }
    }

    @Test
    fun shouldGenerateManifestConfig() {
        val configInterface = javaFile(interfaceName)

        val compilation = javac()
            .withProcessors(ManifestConfigProcessor())
            .compile(configInterface)

        assertThat(compilation).succeeded()

        val configImplementation = javaFile("${interfaceName}ManifestConfig")

        assertThat(compilation)
            .generatedSourceFile("com.rakuten.tech.mobile.manifestconfig.${interfaceName}ManifestConfig")
            .hasSourceEquivalentTo(configImplementation)
    }
}

@RunWith(Parameterized::class)
class ManifestConfigGeneratorFailureSpec(
    private val interfaceName: String,
    private val expectedErrorMessage: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "Should fail to generate config implementation for {0} with message \"{1}\"")
        fun data() = listOf(
            arrayOf("AnnotationOnClass", "Only interfaces can be annotated with @ManifestConfig"),
            arrayOf("UnsupportedType", "Type java.lang.Number not supported as manifest config type."),
            arrayOf("InvalidValueInt", "Cannot convert \"abc\" into type int"),
            arrayOf("InvalidValueBoolean", "Cannot convert \"abc\" into type boolean"),
            arrayOf("InvalidValueFloat", "Cannot convert \"abc\" into type float"),
            arrayOf("InvalidKeyEmpty", "Cannot use empty meta key"),
            arrayOf("InvalidKeyTrailingWhitespace", "Cannot use whitespace in meta key"),
            arrayOf("InvalidKeyLeadingWhitespace", "Cannot use whitespace in meta key"),
            arrayOf("InvalidKeyContainingWhitespace", "Cannot use whitespace in meta key")
        )
    }

    @Test
    fun shouldFailToGenerate() {
        val configInterface = javaFile(interfaceName)

        val compilation = javac()
            .withProcessors(ManifestConfigProcessor())
            .compile(configInterface)

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining(expectedErrorMessage)
    }
}

class ResourceLoader

fun resourceFile(name: String) :String {
    return ResourceLoader::class.java.classLoader.getResource(name).readText()
}

fun javaFile(name: String): JavaFileObject = JavaFileObjects.forSourceString(
    "com.rakuten.tech.mobile.manifestconfig.$name",
    resourceFile("$name.java")
)

@Suppress("unused")
fun javaSource(java: JavaFileObject): String {
    return java.openInputStream().bufferedReader().use { it.readText() }
}
