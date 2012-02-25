/*
 * Copyright 2010 Ronnie Kolehmainen
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

package com.googlecode.cssxfire;

import com.googlecode.cssxfire.resolve.CssRulesetReference;
import com.intellij.lang.Language;
import com.intellij.lang.css.CSSLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.css.*;
import com.intellij.psi.css.impl.util.CssUtil;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ronnie
 */
public class CssUtils
{
    public static CssDeclaration createDeclaration(Project project, String selector, String property, String value, boolean important)
    {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        String text = selector + " {" + property + ":" + value + (important ? " !important" : "") + ";}\n";
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, text);
        return findFirstChildOfType(dummyFile, CssDeclaration.class);
    }

    public static CssRuleset createRuleset(Project project, String selector)
    {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, selector + " {\n\n}\n");
        return findFirstChildOfType(dummyFile, CssRuleset.class);
    }

    public static CssTermList createTermList(Project project, String value)
    {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, ".foo { color: " + value + " }");
        return findFirstChildOfType(dummyFile, CssTermList.class);
    }

    private static <T extends PsiElement> T findFirstChildOfType(@NotNull PsiElement element, Class<T> type)
    {
        PsiElement[] children = element.getChildren();
        for (PsiElement child : children)
        {
            if (type.isAssignableFrom(child.getClass()))
            {
                return (T) child;
            }
            T t = findFirstChildOfType(child, type);
            if (t != null)
            {
                return (T) t;
            }
        }
        return null;
    }

    @Nullable
    public static CssRulesetList findFirstCssRulesetList(@NotNull PsiFile file)
    {
        final Ref<CssRulesetList> ref = new Ref<CssRulesetList>();
        PsiTreeUtil.processElements(file, new PsiElementProcessor()
        {
            public boolean execute(PsiElement element)
            {
                if (element instanceof CssRulesetList)
                {
                    ref.set((CssRulesetList) element);
                    return false;
                }
                return true;
            }
        });
        return ref.get();
    }

    public static PsiSearchHelper getPsiSearchHelper(Project project)
    {
        return ServiceManager.getService(project, PsiSearchHelper.class);
    }

    public static boolean processParents(@NotNull PsiElement element, @NotNull PsiElementProcessor<PsiElement> processor)
    {
        PsiElement parent = element.getParent();
        while (parent != null)
        {
            if (parent instanceof PsiFile)
            {
                break;
            }
            if (!processor.execute(parent))
            {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }

    /** Recursion guard */
    private static ThreadLocal<Set<PsiElement>> processed = new ThreadLocal<Set<PsiElement>>()
    {
        @Override
        protected Set<PsiElement> initialValue()
        {
            return new HashSet<PsiElement>();
        }
    };

    @Nullable
    public static CssTermList resolveVariableAssignment(@NotNull CssDeclaration cssDeclaration)
    {
        CssTermList termList = PsiTreeUtil.getChildOfType(cssDeclaration, CssTermList.class);
        try
        {
            CssTermList result = null;
            while (termList != null)
            {
                if (!processed.get().add(termList))
                {
                    return null; // infinite recursion detected
                }
                CssTerm[] terms = PsiTreeUtil.getChildrenOfType(termList, CssTerm.class);
                if (terms == null || terms.length != 1)
                {
                    return null; // not an explicit variable reference
                }
                termList = resolveTermList(termList);
                if (termList == null)
                {
                    return result;
                }
                result = termList;
            }
            return result;
        }
        finally
        {
            processed.get().clear();
        }
    }

    @Nullable
    private static CssTermList resolveTermList(@NotNull CssTermList termList)
    {
        final Ref<CssTermList> result = new Ref<CssTermList>(null);
        PsiTreeUtil.processElements(termList, new PsiElementProcessor()
        {
            public boolean execute(@NotNull PsiElement psiElement)
            {
                if (psiElement instanceof CssTerm)
                {
                    return true; // CssIdentifierReference - skip
                }
                for (PsiReference reference : psiElement.getReferences())
                {
                    PsiElement referenced = reference.resolve();
                    if (referenced != null)
                    {
                        for (PsiElement child : referenced.getChildren())
                        {
                            if (child instanceof CssTermList)
                            {
                                result.set((CssTermList) child);
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        });

        return result.get();
    }

    public static boolean processCssDeclarations(@Nullable CssBlock block, final PsiElementProcessor<CssDeclaration> declarationProcessor)
    {
        if (block == null)
        {
            return false;
        }
        CssDeclaration[] declarations = PsiTreeUtil.getChildrenOfType(block, CssDeclaration.class);
        if (declarations != null)
        {
            for (CssDeclaration declaration : declarations)
            {
                if (!declarationProcessor.execute(declaration))
                {
                    return false;
                }
            }
        }
        if (ProjectSettings.getInstance(block.getProject()).isResolveMixins())
        {
            return PsiTreeUtil.processElements(block, new PsiElementProcessor()
            {
                public boolean execute(@NotNull PsiElement element)
                {
                    PsiReference[] references = element.getReferences();
                    for (PsiReference reference : references)
                    {
                        if (reference instanceof CssRulesetReference)
                        {
                            CssRulesetReference rulesetReference = (CssRulesetReference) reference;
                            PsiElement resolved = rulesetReference.resolve();
                            if (resolved instanceof CssRuleset)
                            {
                                CssRuleset ruleset = (CssRuleset) resolved;
                                if (!processCssDeclarations(ruleset.getBlock(), declarationProcessor))
                                {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            });
        }
        return true;
    }
}
