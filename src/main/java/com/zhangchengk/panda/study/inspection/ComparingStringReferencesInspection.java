package com.zhangchengk.panda.study.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 实现一个检查，用于检测使用 'a==b' 或 'a!=b' 比较字符串引用的情况。
 * 快速修复会将这些比较转换为使用 'a.equals(b)' 或 '!a.equals(b)'。
 */
final class ComparingStringReferencesInspection extends AbstractBaseJavaLocalInspectionTool {

    private final ReplaceWithEqualsQuickFix myQuickFix = new ReplaceWithEqualsQuickFix();

    /**
     * 重写此方法以提供一个自定义访问者，
     * 该访问者用于检查使用关系运算符 '==' 和 '!=' 的表达式。
     * 访问者必须是非递归的，并且必须是线程安全的。
     *
     * @param holder     用于访问者注册发现的问题的对象
     * @param isOnTheFly 如果检查是在非批处理模式下运行，则为 true
     * @return 此检查的非空访问者
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            /**
             * 评估二进制 PSI 表达式，以确定它们是否包含关系运算符 '==' 和 '!='，
             * 并且它们是字符串类型。
             * 评估会忽略将对象与 null 进行比较的表达式。
             * 如果满足这些条件，则在 ProblemsHolder 中注册问题。
             *
             * @param expression 要评估的二进制表达式。
             */
            @Override
            public void visitBinaryExpression(@NotNull PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                IElementType opSign = expression.getOperationTokenType();
                if (opSign == JavaTokenType.EQEQ || opSign == JavaTokenType.NE) {
                    // The binary expression is the correct type for this inspection
                    PsiExpression lOperand = expression.getLOperand();
                    PsiExpression rOperand = expression.getROperand();
                    if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
                        return;
                    }
                    // Nothing is compared to null, now check the types being compared
                    if (isStringType(lOperand) || isStringType(rOperand)) {
                        // Identified an expression with potential problems, register problem with the quick fix object
                        holder.registerProblem(expression,
                                InspectionBundle.message("inspection.comparing.string.references.problem.descriptor"),
                                myQuickFix);
                    }
                }
            }

            private boolean isStringType(PsiExpression operand) {
                PsiClass psiClass = PsiTypesUtil.getPsiClass(operand.getType());
                if (psiClass == null) {
                    return false;
                }

                return "java.lang.String" == psiClass.getQualifiedName();
            }

            private static boolean isNullLiteral(PsiExpression expression) {
                return expression instanceof PsiLiteralExpression &&
                        ((PsiLiteralExpression) expression).getValue() == null;
            }
        };
    }

    /**
     * 该类通过操作 PSI 树，将检查问题中的表达式从使用 '==' 或 '!=' 替换为 'a.equals(b)' 来提供解决方案。
     */
    private static class ReplaceWithEqualsQuickFix implements LocalQuickFix {

        /**
         * 返回一个部分本地化的字符串，表示快速修复的意图。
         * 该字符串用于此插件的测试代码。
         *
         * @return 快速修复的简短名称。
         */
        @NotNull
        @Override
        public String getName() {
            return InspectionBundle.message("inspection.comparing.string.references.use.quickfix");
        }

        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // binaryExpression 包含一个形式为 "x == y" 的 PSI 表达式，
            // 需要将其替换为 "x.equals(y)"
            PsiBinaryExpression binaryExpression = (PsiBinaryExpression) descriptor.getPsiElement();
            IElementType opSign = binaryExpression.getOperationTokenType();
            PsiExpression lExpr = binaryExpression.getLOperand();
            PsiExpression rExpr = binaryExpression.getROperand();
            if (rExpr == null) {
                return;
            }
            // 步骤 1：从文本创建一个替换片段，其中 "a" 和 "b" 作为占位符
            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
            PsiMethodCallExpression equalsCall =
                    (PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);
            // 步骤 2：用原始文件中的元素替换 "a" 和 "b"
            PsiExpression qualifierExpression =
                    equalsCall.getMethodExpression().getQualifierExpression();
            assert qualifierExpression != null;
            qualifierExpression.replace(lExpr);
            equalsCall.getArgumentList().getExpressions()[0].replace(rExpr);
           // 步骤 3：用替换树替换原始文件中的较大元素
            PsiExpression result = (PsiExpression) binaryExpression.replace(equalsCall);

            // Steps 4-6 needed only for negation
            if (opSign == JavaTokenType.NE) {
                // 步骤 4：创建一个带有否定和被否定的操作数占位符的替换片段
                PsiPrefixExpression negation =
                        (PsiPrefixExpression) factory.createExpressionFromText("!a", null);
                PsiExpression operand = negation.getOperand();
                assert operand != null;
                // 步骤 5：用实际表达式替换操作数占位符
                operand.replace(result);
                // 步骤 6：用否定表达式替换结果
                result.replace(negation);
            }
        }

        @NotNull
        public String getFamilyName() {
            return getName();
        }

    }

    @Override
    public @Nullable String getDescriptionFileName() {
        return "ComparingStringReferences";
    }
}

