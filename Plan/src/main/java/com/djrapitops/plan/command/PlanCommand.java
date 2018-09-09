package com.djrapitops.plan.command;

import com.djrapitops.plan.command.commands.*;
import com.djrapitops.plan.system.locale.Locale;
import com.djrapitops.plan.system.locale.lang.DeepHelpLang;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plugin.command.ColorScheme;
import com.djrapitops.plugin.command.CommandNode;
import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.TreeCmdNode;
import dagger.Lazy;

import javax.inject.Inject;

/**
 * TreeCommand for the /plan command, and all SubCommands.
 * <p>
 * Uses the Abstract Plugin Framework for easier command management.
 *
 * @author Rsl1122
 * @since 1.0.0
 */
public class PlanCommand extends TreeCmdNode {

    private final PlanConfig config;
    private final InspectCommand inspectCommand;
    private final QInspectCommand qInspectCommand;
    private final SearchCommand searchCommand;
    private final ListPlayersCommand listPlayersCommand;
    private final AnalyzeCommand analyzeCommand;
    private final NetworkCommand networkCommand;
    private final ListServersCommand listServersCommand;
    private final Lazy<WebUserCommand> webUserCommand;
    private final RegisterCommand registerCommand;
    private final InfoCommand infoCommand;
    private final ReloadCommand reloadCommand;
    private final Lazy<ManageCommand> manageCommand;
    private final DevCommand devCommand;

    private boolean commandsRegistered;

    @Inject
    public PlanCommand(
            ColorScheme colorScheme,
            Locale locale,
            PlanConfig config,
            // Group 1
            InspectCommand inspectCommand,
            QInspectCommand qInspectCommand,
            SearchCommand searchCommand,
            ListPlayersCommand listPlayersCommand,
            AnalyzeCommand analyzeCommand,
            NetworkCommand networkCommand,
            ListServersCommand listServersCommand,
            // Group 2
            Lazy<WebUserCommand> webUserCommand,
            RegisterCommand registerCommand,
            // Group 3
            InfoCommand infoCommand,
            ReloadCommand reloadCommand,
            Lazy<ManageCommand> manageCommand,
            DevCommand devCommand
    ) {
        super("plan", "", CommandType.CONSOLE, null);

        commandsRegistered = false;

        this.config = config;
        this.inspectCommand = inspectCommand;
        this.qInspectCommand = qInspectCommand;
        this.searchCommand = searchCommand;
        this.listPlayersCommand = listPlayersCommand;
        this.analyzeCommand = analyzeCommand;
        this.networkCommand = networkCommand;
        this.listServersCommand = listServersCommand;
        this.webUserCommand = webUserCommand;
        this.registerCommand = registerCommand;
        this.infoCommand = infoCommand;
        this.reloadCommand = reloadCommand;
        this.manageCommand = manageCommand;
        this.devCommand = devCommand;

        setDefaultCommand("inspect");
        setColorScheme(colorScheme);
        setInDepthHelp(locale.getArray(DeepHelpLang.PLAN));
    }

    public void registerCommands() {
        if (commandsRegistered) {
            return;
        }

        CommandNode[] analyticsGroup = {
                inspectCommand,
                qInspectCommand,
                searchCommand,
                listPlayersCommand,
                analyzeCommand,
                networkCommand,
                listServersCommand
        };
        CommandNode[] webGroup = {
                webUserCommand.get(),
                registerCommand
        };
        CommandNode[] manageGroup = {
                infoCommand,
                reloadCommand,
                manageCommand.get(),
                config.isTrue(Settings.DEV_MODE) ? devCommand : null
        };
        setNodeGroups(analyticsGroup, webGroup, manageGroup);
        commandsRegistered = true;
    }
}
