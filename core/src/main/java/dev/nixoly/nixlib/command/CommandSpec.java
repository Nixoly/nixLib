package dev.nixoly.nixlib.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CommandSpec {

    private final String label;
    private final List<String> aliases;
    private final String description;
    private final String permission;
    private final CommandAction rootAction;
    private final CommandAction fallback;
    private final CommandAction permissionDenied;
    private final List<Sub> subs;

    private CommandSpec(Builder builder) {
        this.label = builder.label;
        this.aliases = List.copyOf(builder.aliases);
        this.description = builder.description;
        this.permission = builder.permission;
        this.rootAction = builder.rootAction;
        this.fallback = builder.fallback;
        this.permissionDenied = builder.permissionDenied;
        this.subs = List.copyOf(builder.subs);
    }

    public static Builder builder(@NotNull String label) {
        return new Builder(label);
    }

    public static Sub.Builder sub(@NotNull String name) {
        return new Sub.Builder(name);
    }

    public @NotNull String label() {
        return label;
    }

    public @NotNull List<String> aliases() {
        return aliases;
    }

    public @NotNull String description() {
        return description;
    }

    public @Nullable String permission() {
        return permission;
    }

    public @Nullable CommandAction rootAction() {
        return rootAction;
    }

    public @Nullable CommandAction fallback() {
        return fallback;
    }

    public @Nullable CommandAction permissionDenied() {
        return permissionDenied;
    }

    public @NotNull List<Sub> subs() {
        return subs;
    }

    public @Nullable Sub findSub(@NotNull String token) {
        for (Sub sub : subs) {
            for (String name : sub.names()) {
                if (name.equalsIgnoreCase(token)) {
                    return sub;
                }
            }
        }
        return null;
    }

    public static final class Sub {

        private final List<String> names;
        private final String permission;
        private final CommandAction action;
        private final List<SuggestionProvider> argSuggestions;

        private Sub(Builder builder) {
            this.names = List.copyOf(builder.names);
            this.permission = builder.permission;
            this.action = builder.action;
            this.argSuggestions = List.copyOf(builder.argSuggestions);
        }

        public @NotNull List<String> names() {
            return names;
        }

        public @NotNull String primary() {
            return names.get(0);
        }

        public @Nullable String permission() {
            return permission;
        }

        public @Nullable CommandAction action() {
            return action;
        }

        public @NotNull List<SuggestionProvider> argSuggestions() {
            return argSuggestions;
        }

        public static final class Builder {

            private final List<String> names = new ArrayList<>();
            private String permission;
            private CommandAction action;
            private final List<SuggestionProvider> argSuggestions = new ArrayList<>();

            Builder(@NotNull String name) {
                this.names.add(name);
            }

            public Builder aliases(@Nullable Collection<String> aliases) {
                if (aliases != null) {
                    for (String alias : aliases) {
                        if (alias != null && !alias.isBlank()) {
                            names.add(alias);
                        }
                    }
                }
                return this;
            }

            public Builder permission(@Nullable String permission) {
                this.permission = permission;
                return this;
            }

            public Builder action(@Nullable CommandAction action) {
                this.action = action;
                return this;
            }

            public Builder arg(@NotNull SuggestionProvider suggestions) {
                this.argSuggestions.add(suggestions);
                return this;
            }

            public Builder arg(@NotNull Collection<String> options) {
                List<String> snapshot = List.copyOf(options);
                this.argSuggestions.add(context -> snapshot);
                return this;
            }

            public Builder argNone() {
                this.argSuggestions.add(context -> Collections.emptyList());
                return this;
            }

            public Sub build() {
                return new Sub(this);
            }
        }
    }

    public static final class Builder {

        private final String label;
        private final List<String> aliases = new ArrayList<>();
        private String description = "";
        private String permission;
        private CommandAction rootAction;
        private CommandAction fallback;
        private CommandAction permissionDenied;
        private final List<Sub> subs = new ArrayList<>();

        Builder(@NotNull String label) {
            this.label = label;
        }

        public Builder aliases(@Nullable Collection<String> aliases) {
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias != null && !alias.isBlank()) {
                        this.aliases.add(alias);
                    }
                }
            }
            return this;
        }

        public Builder description(@NotNull String description) {
            this.description = description;
            return this;
        }

        public Builder permission(@Nullable String permission) {
            this.permission = permission;
            return this;
        }

        public Builder onRoot(@Nullable CommandAction action) {
            this.rootAction = action;
            return this;
        }

        public Builder onUnknown(@Nullable CommandAction action) {
            this.fallback = action;
            return this;
        }

        public Builder onPermissionDenied(@Nullable CommandAction action) {
            this.permissionDenied = action;
            return this;
        }

        public Builder sub(@NotNull Sub sub) {
            this.subs.add(sub);
            return this;
        }

        public Builder sub(@NotNull Sub.Builder sub) {
            return sub(sub.build());
        }

        public CommandSpec build() {
            return new CommandSpec(this);
        }
    }
}
