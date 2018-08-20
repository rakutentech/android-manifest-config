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

import com.google.auto.service.AutoService
import com.rakuten.tech.mobile.manifestconfig.annotations.ManifestConfig
import com.rakuten.tech.mobile.manifestconfig.annotations.MetaData
import com.squareup.javapoet.*
import com.sun.tools.javac.code.Type
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
@AutoService(Processor::class)
class ManifestConfigProcessor : AbstractProcessor() {

    private lateinit var typeUtils: Types
    private lateinit var codeGenerator: ManifestConfigGenerator
    private lateinit var messager: Messager
    private val annotatedInterfaces: MutableList<TypeElement> = mutableListOf()

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        typeUtils = processingEnv!!.typeUtils
        codeGenerator = ManifestConfigGenerator(processingEnv.elementUtils, processingEnv.filer)
        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (elem  in roundEnv.getElementsAnnotatedWith(ManifestConfig::class.java)) {
            if (elem.kind != ElementKind.INTERFACE) {
                error(
                    elem,
                    "Only interfaces can be annotated with @%s",
                    ManifestConfig::class.simpleName!!)
                return true
            }

            annotatedInterfaces.add(elem as TypeElement)
        }

        for (configToGenerate in annotatedInterfaces) {
            try {
                codeGenerator.generateClass(configToGenerate)
            } catch (e: ManifestConfigGeneratorException) {
                error(configToGenerate,
                    e.message ?:
                    "Failed to generate implementation for ${configToGenerate.simpleName}"
                )
            }
        }

        annotatedInterfaces.clear()

        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ManifestConfig::class.java.name)
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            String.format(msg, *args),
            e
        )
    }
}

class ManifestConfigInterface(val element: TypeElement)

val CONTEXT: ClassName = ClassName.get("android.content", "Context")
val PACKAGE_MANAGER: ClassName = ClassName.get("android.content.pm", "PackageManager")
val BUNDLE: ClassName = ClassName.get("android.os", "Bundle")
val NAME_NOT_FOUND_EXCEPTION: ClassName = ClassName.get("android.content.pm", "PackageManager.NameNotFoundException")

class ManifestConfigGenerator(
    private val elementUtils: Elements,
    private val filer: Filer
) {

    fun generateClass(config: TypeElement) {

        val constructor = constructor()

        val classBuilder = TypeSpec.classBuilder("${config.simpleName}ManifestConfig")
            .addSuperinterface(TypeName.get(config.asType()))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(BUNDLE, "metaData", Modifier.PRIVATE)
            .addMethod(constructor)

        for (member in config.enclosedElements) {
            if(member.kind == ElementKind.METHOD) {
                classBuilder.addMethod(configMethod(member))
            }
        }

        val javaFile = JavaFile.builder(
            elementUtils.getPackageOf(config).qualifiedName.toString(),
            classBuilder.build()
        ).build()

        javaFile.writeTo(filer)
    }

    private fun configMethod(member: Element): MethodSpec {

        val type = member.asType() as Type.MethodType
        val constName = constantName(member)
        val returnType = TypeName.get(type.returnType)
        val returnFallbackValue = returnFallbackValue(returnType, member)

        return MethodSpec.methodBuilder(member.simpleName.toString())
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addStatement(readFromBundle(returnType), "metaData", constName, returnFallbackValue)
            .returns(returnType)
            .build()
    }

    private fun constantName(member: Element): String {
        val methodName = member.simpleName.toString()
        val annotation = member.getAnnotation(MetaData::class.java)
        val key = annotation?.key
        val constantName = if (key.isNullOrEmpty()) methodName.capitalize() else key!!
        return when {
            constantName.isBlank() -> throw InvalidKeyException("Cannot use empty meta key")
            constantName.any { it.isWhitespace() } -> throw InvalidKeyException("Cannot use whitespace in meta key")
            else -> constantName
        }
    }

    private fun returnFallbackValue(returnType: TypeName, member: Element): Any {
        val annotation = member.getAnnotation(MetaData::class.java)
        val value = annotation?.value?.toValue(returnType)

        return when (returnType.toString()) {
            "int", "java.lang.Integer"     -> value ?: -1
            "boolean", "java.lang.Boolean" -> value ?: false
            "float", "java.lang.Float"     -> value ?: -1f
            "java.lang.String"             -> value ?: ""
            else -> throw UnsupportedTypeException(returnType)
        }
    }

    private fun readFromBundle(returnType: TypeName): String {
        return when(returnType.toString()) {
            "int", "java.lang.Integer"      -> "return \$N.getInt(\$S, \$L)"
            "boolean", "java.lang.Boolean"  -> "return \$N.getBoolean(\$S, \$L)"
            "float", "java.lang.Float"      -> "return \$N.getFloat(\$S, \$Lf)"
            "java.lang.String"              -> "return \$N.getString(\$S, \$S)"
            else -> throw UnsupportedTypeException(returnType)
        }
    }

    private fun constructor(): MethodSpec {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(CONTEXT, "context")
            .addStatement("this.\$N = new \$T()", "metaData", BUNDLE)
            .beginControlFlow("try")
            .addStatement("\$T \$N = context.getPackageManager()", PACKAGE_MANAGER, "pm")
            .addStatement(
                "\$T \$N = \$N.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData",
                BUNDLE, "appMeta", "pm"
            )
            .beginControlFlow("if (\$N != null)", "appMeta")
            .addStatement("this.\$N = \$N", "metaData", "appMeta")
            .endControlFlow()
            .nextControlFlow("catch (\$T ignored)", NAME_NOT_FOUND_EXCEPTION)
            .addComment("if we can't get metadata we'll use default config")
            .endControlFlow()
            .build()
    }
}

open class ManifestConfigGeneratorException(msg: String) : Exception(msg)

class UnsupportedTypeException(
    type: TypeName
) : ManifestConfigGeneratorException("Type $type not supported as manifest config type.")

class InvalidValueException(
    type: TypeName,
    value: String
) : ManifestConfigGeneratorException("Cannot convert \"$value\" into type $type")

class InvalidKeyException(msg: String) : ManifestConfigGeneratorException(msg)

fun String.toValue(type: TypeName): Any? {
    return try {
        when (type.toString()) {
            "int", "java.lang.Integer" -> if (isEmpty()) null else toInt()
            "boolean", "java.lang.Boolean" -> when {
                this.equals("true", ignoreCase = true) -> true
                this.equals("false", ignoreCase = true) -> false
                this.isEmpty() -> null // default value of annotation property
                else -> throw Exception()
            // parse booleans strictly, only "true" or "false allowed (ignoring case)
            }
            "float", "java.lang.Float" -> if (isEmpty()) null else toFloat()
            "java.lang.String" -> this
            else -> throw UnsupportedTypeException(type)
        }
    } catch (e: Exception) {
        throw e as? ManifestConfigGeneratorException ?: InvalidValueException(type, this)
    }
}