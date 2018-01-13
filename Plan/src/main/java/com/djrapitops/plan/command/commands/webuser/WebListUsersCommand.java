package com.djrapitops.plan.command.commands.webuser;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.locale.Msg;
import com.djrapitops.plan.system.settings.Permissions;
import com.djrapitops.plan.utilities.comparators.WebUserComparator;
import com.djrapitops.plugin.api.utility.log.Log;
import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.ISender;
import com.djrapitops.plugin.command.SubCommand;
import com.djrapitops.plugin.settings.ColorScheme;
import com.djrapitops.plugin.task.AbsRunnable;
import com.djrapitops.plugin.task.RunnableFactory;

import java.util.List;

/**
 * Subcommand for checking WebUser list.
 *
 * @author Rsl1122
 * @since 3.5.2
 */
public class WebListUsersCommand extends SubCommand {

    private final PlanPlugin plugin;

    public WebListUsersCommand(PlanPlugin plugin) {
        super("list", CommandType.CONSOLE, Permissions.MANAGE_WEB.getPerm(), "List registered web users & permission levels.");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(ISender sender, String commandLabel, String[] args) {
        RunnableFactory.createNew(new AbsRunnable("Webuser List Task") {
            @Override
            public void run() {
                try {
                    ColorScheme cs = plugin.getColorScheme();
                    String mCol = cs.getMainColor();
                    List<WebUser> users = plugin.getDB().getSecurityTable().getUsers();
                    users.sort(new WebUserComparator());
                    sender.sendMessage(Locale.get(Msg.CMD_CONSTANT_FOOTER).parse() + mCol + " WebUsers (" + users.size() + ")");
                    for (WebUser user : users) {
                        sender.sendMessage("  " + user.getPermLevel() + " : " + user.getName());
                    }
                    sender.sendMessage(Locale.get(Msg.CMD_CONSTANT_FOOTER).parse());
                } catch (Exception ex) {
                    Log.toLog(this.getClass().getName(), ex);
                    sender.sendMessage(Locale.get(Msg.MANAGE_INFO_FAIL).parse());
                } finally {
                    this.cancel();
                }
            }
        }).runTaskAsynchronously();
        return true;
    }

}
