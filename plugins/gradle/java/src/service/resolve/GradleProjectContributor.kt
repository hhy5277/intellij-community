// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.processReceiverType

/**
 * @author Vladislav.Soroka
 */
class GradleProjectContributor : GradleMethodContextContributor {
  companion object {
    val projectClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT,
                                                                                  "project", "configure", "subprojects", "allprojects"))
    val copySpecClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "copy", "copySpec"))
    val fileTreeClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "fileTree"))
    val filesClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "files"))
    val taskClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "task"))
    val execClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "exec"))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (projectClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_PROJECT, closure), Closure.DELEGATE_FIRST)
    }
    if (copySpecClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_COPY_SPEC, closure), Closure.DELEGATE_FIRST)
    }
    if (fileTreeClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_CONFIGURABLE_FILE_TREE, closure), Closure.DELEGATE_FIRST)
    }
    if (filesClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_CONFIGURABLE_FILE_COLLECTION, closure), Closure.DELEGATE_FIRST)
    }
    if (taskClosure.accepts(closure)) {
      var taskType: PsiType? = null
      val parent = closure.parent
      if (parent is GrMethodCallExpression) {
        val typeTakArgument = parent.namedArguments.find { "type" == it.labelName }?.expression?.type
        if (typeTakArgument is PsiClassType && "Class" == typeTakArgument.className) {
          taskType = typeTakArgument.parameters.firstOrNull()
        }
      }
      if (taskType == null) {
        taskType = createType(GRADLE_API_TASK, closure)
      }
      return DelegatesToInfo(taskType, Closure.DELEGATE_FIRST)
    }
    if (execClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_PROCESS_EXEC_SPEC, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }

  override fun process(methodCallInfo: List<String>, processor: PsiScopeProcessor, state: ResolveState, place: PsiElement): Boolean {
    return GradleProjectAwareType(GRADLE_API_PROJECT, place.containingFile).processReceiverType(processor, state, place)
  }
}
