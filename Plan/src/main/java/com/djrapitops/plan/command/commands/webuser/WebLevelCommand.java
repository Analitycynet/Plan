package com.djrapitops.plan.command.commands.webuser;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.locale.Msg;
import com.djrapitops.plan.system.settings.Permissions;
import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.ISender;
import com.djrapitops.plugin.command.SubCommand;
import com.djrapitops.plugin.settings.ColorScheme;

/**
 * Subcommand for info about permission levels.
 *
 * @author Rsl1122
 * @since 3.5.2
 */
public class WebLevelCommand extends SubCommand {

    private final PlanPlugin plugin;

    public WebLevelCommand(PlanPlugin plugin) {
        super("level",
                CommandType.CONSOLE,
                Permissions.MANAGE_WEB.getPerm(),
                Locale.get(Msg.CMD_USG_WEB_LEVEL).toString());
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(ISender sender, String commandLabel, String[] args) {
        ColorScheme cs = plugin.getColorScheme();
        String sCol = cs.getSecondaryColor();
        String cmdBall = Locale.get(Msg.CMD_CONSTANT_LIST_BALL).parse();
        String cmdFooter = Locale.get(Msg.CMD_CONSTANT_FOOTER).parse();

        String[] messages = new String[]{
                cmdFooter,
                cmdBall + sCol + "0: Access all pages",
                cmdBall + sCol + "1: Access '/players' and all inspect pages",
                cmdBall + sCol + "2: Access inspect page with the same username as the webuser",
                cmdBall + sCol + "3+: No permissions",
                cmdFooter
        };

        sender.sendMessage(messages);
        return true;
    }

}
