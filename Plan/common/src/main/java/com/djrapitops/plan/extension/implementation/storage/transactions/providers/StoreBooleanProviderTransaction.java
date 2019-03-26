/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.extension.implementation.storage.transactions.providers;

import com.djrapitops.plan.db.access.ExecStatement;
import com.djrapitops.plan.db.access.Executable;
import com.djrapitops.plan.db.access.transactions.Transaction;
import com.djrapitops.plan.db.sql.tables.ExtensionIconTable;
import com.djrapitops.plan.db.sql.tables.ExtensionPluginTable;
import com.djrapitops.plan.db.sql.tables.ExtensionTabTable;
import com.djrapitops.plan.extension.implementation.ProviderInformation;
import com.djrapitops.plan.extension.implementation.providers.DataProvider;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import static com.djrapitops.plan.db.sql.parsing.Sql.AND;
import static com.djrapitops.plan.db.sql.parsing.Sql.WHERE;
import static com.djrapitops.plan.db.sql.tables.ExtensionProviderTable.*;

/**
 * Transaction to store information about a {@link com.djrapitops.plan.extension.implementation.providers.BooleanDataProvider}.
 *
 * @author Rsl1122
 */
public class StoreBooleanProviderTransaction extends Transaction {

    private final String providedCondition;
    private final boolean hidden;
    private final UUID serverUUID;
    private final ProviderInformation providerInformation;

    public StoreBooleanProviderTransaction(DataProvider<Boolean> booleanProvider, String providedCondition, boolean hidden, UUID serverUUID) {
        this.providedCondition = providedCondition;
        this.hidden = hidden;
        this.serverUUID = serverUUID;
        this.providerInformation = booleanProvider.getProviderInformation();
    }

    @Override
    protected void performOperations() {
        execute(storeProvider());
    }

    private Executable storeProvider() {
        return connection -> {
            if (!updateProvider().execute(connection)) {
                return insertProvider().execute(connection);
            }
            return false;
        };
    }

    private Executable updateProvider() {
        String sql = "UPDATE " + TABLE_NAME +
                " SET " +
                TEXT + "=?," +
                DESCRIPTION + "=?," +
                PRIORITY + "=?," +
                CONDITION + "=?," +
                PROVIDED_CONDITION + "=?," +
                TAB_ID + "=" + ExtensionTabTable.STATEMENT_SELECT_TAB_ID + "," +
                ICON_ID + "=" + ExtensionIconTable.STATEMENT_SELECT_ICON_ID +
                WHERE + PLUGIN_ID + "=" + ExtensionPluginTable.STATEMENT_SELECT_PLUGIN_ID +
                AND + PROVIDER_NAME + "=?," +
                HIDDEN + "=?";

        return new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, providerInformation.getText());
                if (providerInformation.getDescription().isPresent()) {
                    statement.setString(2, providerInformation.getDescription().get());
                } else {
                    statement.setNull(2, Types.VARCHAR);
                }
                statement.setInt(3, providerInformation.getPriority());
                if (providerInformation.getCondition().isPresent()) {
                    statement.setString(4, providerInformation.getCondition().orElse(null));
                } else {
                    statement.setNull(4, Types.VARCHAR);
                }
                if (providedCondition != null) {
                    statement.setString(5, providedCondition);
                } else {
                    statement.setNull(5, Types.VARCHAR);
                }
                ExtensionTabTable.set3TabValuesToStatement(statement, 6, providerInformation.getTab().orElse("No Tab"), providerInformation.getPluginName(), serverUUID);
                ExtensionIconTable.set3IconValuesToStatement(statement, 9, providerInformation.getIcon());
                ExtensionPluginTable.set2PluginValuesToStatement(statement, 12, providerInformation.getPluginName(), serverUUID);
                statement.setString(14, providerInformation.getName());
                statement.setBoolean(15, hidden);
            }
        };
    }

    private Executable insertProvider() {
        String sql = "INSERT INTO " + TABLE_NAME + "(" +
                PROVIDER_NAME + "," +
                TEXT + "," +
                DESCRIPTION + "," +
                PRIORITY + "," +
                CONDITION + "," +
                PROVIDED_CONDITION + "," +
                TAB_ID + "," +
                ICON_ID + "," +
                PLUGIN_ID +
                ") VALUES (?,?,?,?,?,?," +
                ExtensionTabTable.STATEMENT_SELECT_TAB_ID + "," +
                ExtensionIconTable.STATEMENT_SELECT_ICON_ID + "," +
                ExtensionPluginTable.STATEMENT_SELECT_PLUGIN_ID + ", ?)";
        return new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, providerInformation.getName());
                statement.setString(2, providerInformation.getText());
                if (providerInformation.getDescription().isPresent()) {
                    statement.setString(3, providerInformation.getDescription().get());
                } else {
                    statement.setNull(3, Types.VARCHAR);
                }
                statement.setInt(4, providerInformation.getPriority());
                if (providerInformation.getCondition().isPresent()) {
                    statement.setString(5, providerInformation.getCondition().orElse(null));
                } else {
                    statement.setNull(5, Types.VARCHAR);
                }
                if (providedCondition != null) {
                    statement.setString(6, providedCondition);
                } else {
                    statement.setNull(6, Types.VARCHAR);
                }
                ExtensionTabTable.set3TabValuesToStatement(statement, 7, providerInformation.getTab().orElse("No Tab"), providerInformation.getPluginName(), serverUUID);
                ExtensionIconTable.set3IconValuesToStatement(statement, 10, providerInformation.getIcon());
                ExtensionPluginTable.set2PluginValuesToStatement(statement, 13, providerInformation.getPluginName(), serverUUID);
                statement.setBoolean(15, hidden);
            }
        };
    }
}