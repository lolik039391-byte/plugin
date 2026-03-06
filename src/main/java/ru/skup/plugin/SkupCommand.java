package ru.skup.plugin;

/**
 * Legacy wrapper for backward compatibility in source history.
 * Runtime command is now /buyer (alias: /skup) via BuyerCommand.
 */
@Deprecated
public class SkupCommand {
    private SkupCommand() {
    }
}
