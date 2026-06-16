package dev.nixoly.nixlib.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NixCommand extends Command {

    private final CommandSpec spec;

    public NixCommand(@NotNull CommandSpec spec) {
        super(spec.label(), spec.description(), "/" + spec.label(), spec.aliases());
        this.spec = spec;
    }

    public @NotNull CommandSpec spec() {
        return spec;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        CommandContext context = new CommandContext(sender, label, args);

        if (!allowed(sender, spec.permission())) {
            deny(context);
            return true;
        }

        if (args.length == 0) {
            if (spec.rootAction() != null) {
                spec.rootAction().run(context);
            }
            return true;
        }

        CommandSpec.Sub sub = spec.findSub(args[0]);
        if (sub == null) {
            CommandAction fallback = spec.fallback() != null ? spec.fallback() : spec.rootAction();
            if (fallback != null) {
                fallback.run(context);
            }
            return true;
        }

        if (!allowed(sender, sub.permission())) {
            deny(context);
            return true;
        }

        if (sub.action() != null) {
            sub.action().run(context);
        } else if (spec.rootAction() != null) {
            spec.rootAction().run(context);
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                             @NotNull String[] args) {
        if (args.length == 0 || !allowed(sender, spec.permission())) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (CommandSpec.Sub sub : spec.subs()) {
                if (!allowed(sender, sub.permission())) {
                    continue;
                }
                String name = sub.primary();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(name);
                }
            }
            return out;
        }

        CommandSpec.Sub sub = spec.findSub(args[0]);
        if (sub == null || !allowed(sender, sub.permission())) {
            return new ArrayList<>();
        }
        int position = args.length - 2;
        if (position < 0 || position >= sub.argSuggestions().size()) {
            return new ArrayList<>();
        }
        List<String> options = sub.argSuggestions().get(position).suggest(new CommandContext(sender, alias, args));
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option != null && option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
        return out;
    }

    private boolean allowed(@NotNull CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }

    private void deny(@NotNull CommandContext context) {
        if (spec.permissionDenied() != null) {
            spec.permissionDenied().run(context);
        }
    }
}
