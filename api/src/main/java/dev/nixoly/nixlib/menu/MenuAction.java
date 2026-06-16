package dev.nixoly.nixlib.menu;

@FunctionalInterface
public interface MenuAction {

    void run(ActionContext context, String argument);
}
