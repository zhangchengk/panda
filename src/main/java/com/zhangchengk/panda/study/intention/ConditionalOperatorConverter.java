// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.zhangchengk.panda.study.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 实现一个意图操作，将三元表达式替换为 if-then-else 语句。
 * 将光标放到 ? 触发动作
 */
@NonNls
final class ConditionalOperatorConverter extends PsiElementBaseIntentionAction implements IntentionAction {

    /**
     * 检查此意图是否在文件中的光标偏移位置可用——光标必须位于三元表达式的 "?" 字符之前。如果满足此条件，此意图的条目将显示在可用意图列表中。
     *
     * <p>注意：此方法必须快速完成检查并返回。</p>
     *
     * @param project 对正在编辑的 Project 对象的引用。
     * @param editor  对编辑项目源代码的对象的引用。
     * @param element 对当前光标所在位置的 PSI 元素的引用。
     * @return 如果光标位于字符串字面量元素中，则返回 {@code true}，表示应将此功能添加到意图菜单；对于其他类型的光标位置，返回 {@code false}。
     */
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        // Quick sanity check
        if (element == null) {
            return false;
        }

        // Is this a token of type representing a "?" character?
        if (element instanceof PsiJavaToken token) {
            if (token.getTokenType() != JavaTokenType.QUEST) {
                return false;
            }
            // Is this token part of a fully formed conditional, i.e. a ternary?
            if (token.getParent() instanceof PsiConditionalExpression conditionalExpression) {
                // Satisfies all criteria; call back invoke method
                return conditionalExpression.getThenExpression() != null && conditionalExpression.getElseExpression() != null;
            }
            return false;
        }
        return false;
    }

    /**
     * 修改 PSI 以将三元表达式转换为 if-then-else 语句。
     * 如果三元表达式是声明的一部分，则将声明分离并移动到 if-then-else 语句的上方。
     * 当用户从可用意图列表中选择此意图操作时调用。
     *
     * @param project 对正在编辑的 Project 对象的引用。
     * @param editor  对编辑项目源代码的对象的引用。
     * @param element 对当前光标所在位置的 PSI 元素的引用。
     * @throws IncorrectOperationException 当 PSI 树的操纵失败时，由底层（PSI 模型）写操作上下文抛出。
     */
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        // Get the factory for making new PsiElements, and the code style manager to format new statements
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        CodeStyleManager codeStylist = CodeStyleManager.getInstance(project);

        // Get the parent of the "?" element in the ternary statement to find the conditional expression that contains it
        PsiConditionalExpression conditionalExpression =
                PsiTreeUtil.getParentOfType(element, PsiConditionalExpression.class, false);
        if (conditionalExpression == null) {
            return;
        }
        // Verify the conditional expression exists and has two outcomes in the ternary statement.
        PsiExpression thenExpression = conditionalExpression.getThenExpression();
        PsiExpression elseExpression = conditionalExpression.getElseExpression();
        if (thenExpression == null || elseExpression == null) {
            return;
        }

        // Keep searching up the PSI Tree in case the ternary is part of a FOR statement.
        PsiElement originalStatement = PsiTreeUtil.getParentOfType(conditionalExpression, PsiStatement.class, false);
        while (originalStatement instanceof PsiForStatement) {
            originalStatement = PsiTreeUtil.getParentOfType(originalStatement, PsiStatement.class, true);
        }
        if (originalStatement == null) {
            return;
        }

        // If the original statement is a declaration based on a ternary operator,
        // split the declaration and assignment
        if (originalStatement instanceof PsiDeclarationStatement declaration) {
            // Find the local variable within the declaration statement
            PsiElement[] declaredElements = declaration.getDeclaredElements();
            PsiLocalVariable variable = null;
            for (PsiElement declaredElement : declaredElements) {
                if (declaredElement instanceof PsiLocalVariable &&
                        PsiTreeUtil.isAncestor(declaredElement, conditionalExpression, true)) {
                    variable = (PsiLocalVariable) declaredElement;
                    break;
                }
            }
            if (variable == null) {
                return;
            }

            // Ensure that the variable declaration is not combined with other declarations, and add a mark
            variable.normalizeDeclaration();
            Object marker = new Object();
            PsiTreeUtil.mark(conditionalExpression, marker);

            // Create a new expression to declare the local variable
            PsiExpressionStatement statement =
                    (PsiExpressionStatement) factory.createStatementFromText(variable.getName() + " = 0;", null);
            statement = (PsiExpressionStatement) codeStylist.reformat(statement);

            // Replace initializer with the ternary expression, making an assignment statement using the ternary
            PsiExpression rExpression = ((PsiAssignmentExpression) statement.getExpression()).getRExpression();
            PsiExpression variableInitializer = variable.getInitializer();
            if (rExpression == null || variableInitializer == null) {
                return;
            }
            rExpression.replace(variableInitializer);

            // Remove the initializer portion of the local variable statement,
            // making it a declaration statement with no initializer
            variableInitializer.delete();

            // Get the grandparent of the local var declaration, and add the new declaration just beneath it
            PsiElement variableParent = variable.getParent();
            originalStatement = variableParent.getParent().addAfter(statement, variableParent);
            conditionalExpression = (PsiConditionalExpression) PsiTreeUtil.releaseMark(originalStatement, marker);
        }
        if (conditionalExpression == null) {
            return;
        }

        // Create an IF statement from a string with placeholder elements.
        // This will replace the ternary statement
        PsiIfStatement newIfStmt = (PsiIfStatement) factory.createStatementFromText("if (true) {a=b;} else {c=d;}", null);
        newIfStmt = (PsiIfStatement) codeStylist.reformat(newIfStmt);

        // Replace the conditional expression with the one from the original ternary expression
        PsiReferenceExpression condition = (PsiReferenceExpression) conditionalExpression.getCondition().copy();
        PsiExpression newIfStmtCondition = newIfStmt.getCondition();
        if (newIfStmtCondition == null) {
            return;
        }
        newIfStmtCondition.replace(condition);

        // Begin building the assignment string for the THEN and ELSE clauses using the
        // parent of the ternary conditional expression
        PsiAssignmentExpression assignmentExpression =
                PsiTreeUtil.getParentOfType(conditionalExpression, PsiAssignmentExpression.class, false);
        if (assignmentExpression == null) {
            return;
        }
        // Get the contents of the assignment expression up to the start of the ternary expression
        String exprFrag = assignmentExpression.getLExpression().getText()
                + assignmentExpression.getOperationSign().getText();

        // Build the THEN statement string for the new IF statement,
        // make a PsiExpressionStatement from the string, and switch the placeholder
        String thenStr = exprFrag + thenExpression.getText() + ";";
        PsiExpressionStatement thenStmt = (PsiExpressionStatement) factory.createStatementFromText(thenStr, null);
        PsiBlockStatement thenBranch = (PsiBlockStatement) newIfStmt.getThenBranch();
        if (thenBranch == null) {
            return;
        }
        thenBranch.getCodeBlock().getStatements()[0].replace(thenStmt);

        // Build the ELSE statement string for the new IF statement,
        // make a PsiExpressionStatement from the string, and switch the placeholder
        String elseStr = exprFrag + elseExpression.getText() + ";";
        PsiExpressionStatement elseStmt = (PsiExpressionStatement) factory.createStatementFromText(elseStr, null);
        PsiBlockStatement elseBranch = (PsiBlockStatement) newIfStmt.getElseBranch();
        if (elseBranch == null) {
            return;
        }
        elseBranch.getCodeBlock().getStatements()[0].replace(elseStmt);

        // Replace the entire original statement with the new IF
        originalStatement.replace(newIfStmt);
    }

    /**
     * 如果此操作适用，返回将在可用意图操作列表中显示的文本。
     */
    @NotNull
    public String getText() {
        return getFamilyName();
    }

    /**
     * 返回此意图家族的名称文本。
     * 用于外部化意图的“自动显示”状态。
     * 也是描述文件的目录名称。
     *
     * @return 意图家族的名称。
     */
    @NotNull
    public String getFamilyName() {
        return "SDK: Convert ternary operator to if statement";
    }

}
