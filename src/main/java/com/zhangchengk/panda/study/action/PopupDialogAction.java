
package com.zhangchengk.panda.study.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 该类的主要功能是在与 IntelliJ 平台交互时为用户提供一个弹出对话框作为反馈。
 * <p>
 * - 功能：这个类展示了如何与 IntelliJ 平台进行交互。
 * - 行为：类的主要行为是向用户显示一个弹出对话框。
 * - 实例化方式：
 * -- 通常情况下，这个类是由 IntelliJ 平台框架根据 `plugin.xml` 文件中的声明来实例化的。
 * -- 如果是在运行时动态添加的，这个类则由一个动作组（action group）来实例化。
 */
public class PopupDialogAction extends AnAction {

    public PopupDialogAction() {
        super();
    }

    /**
     * 该构造函数用于支持动态添加的菜单动作。
     * <p>
     * - 构造函数的作用：这个构造函数用于支持动态添加的菜单动作。
     * - 设置内容：它设置了菜单项的文本、描述和图标。
     * - 默认构造函数：如果不使用这个构造函数，IntelliJ 平台将使用默认的 `AnAction` 构造函数。
     *
     * @param text：菜单项上显示的文本。
     * @param description：菜单项的描述。
     * @param icon：菜单项使用的图标。
     */
    public PopupDialogAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    /**
     * 这段代码确保了 PopupDialogAction 的状态更新操作在后台线程中执行，以防止 UI 线程被阻塞。
     * ActionUpdateThread.BGT：BGT 是 Background Thread 的缩写，表示状态更新操作将在后台线程中执行，而不是在事件调度线程（Event Dispatch Thread, EDT）中执行。
     * */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 这是 AnAction 接口中的abstract方法，当用户触发该动作时，该方法会被调用。
     *
     * @param event 包含了触发动作时的各种信息。
     * */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // 使用事件创建并显示一个对话框
        Project currentProject = event.getProject(); // 获取当前打开的项目
        StringBuilder message =
                new StringBuilder(event.getPresentation().getText() + " Selected!");// 获取动作的显示文本，并将其添加到 StringBuilder 中，形成初始消息。
        // 如果编辑器中选择了元素，添加关于它的信息
        Navigatable selectedElement = event.getData(CommonDataKeys.NAVIGATABLE); // 获取当前选中的可导航元素（如文件、类、方法等）。
        if (selectedElement != null) {
            message.append("\nSelected Element: ").append(selectedElement);
        }
        String title = event.getPresentation().getDescription();
        Messages.showMessageDialog( // 显示一个消息对话框
                currentProject,
                message.toString(),
                title,
                Messages.getInformationIcon());
    }

    /**
     *  AnAction 接口中的方法，用于在每次动作的状态需要更新时被调用。
     *  AnActionEvent 对象包含了触发动作时的各种信息。
     * */
    @Override
    public void update(AnActionEvent e) {
        // 根据是否有项目打开来设置动作的可用性和可见性
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

}
