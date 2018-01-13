package com.djrapitops.plan.data;

import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.container.StickyData;
import com.djrapitops.plan.data.container.TPS;
import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.HealthNotes;
import com.djrapitops.plan.data.plugin.PluginData;
import com.djrapitops.plan.data.time.WorldTimes;
import com.djrapitops.plan.settings.theme.Theme;
import com.djrapitops.plan.settings.theme.ThemeVal;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.utilities.FormatUtils;
import com.djrapitops.plan.utilities.MiscUtils;
import com.djrapitops.plan.utilities.analysis.AnalysisUtils;
import com.djrapitops.plan.utilities.analysis.MathUtils;
import com.djrapitops.plan.utilities.comparators.SessionStartComparator;
import com.djrapitops.plan.utilities.html.Html;
import com.djrapitops.plan.utilities.html.HtmlUtils;
import com.djrapitops.plan.utilities.html.graphs.ActivityStackGraph;
import com.djrapitops.plan.utilities.html.graphs.PunchCardGraph;
import com.djrapitops.plan.utilities.html.graphs.WorldMap;
import com.djrapitops.plan.utilities.html.graphs.line.*;
import com.djrapitops.plan.utilities.html.graphs.pie.ActivityPie;
import com.djrapitops.plan.utilities.html.graphs.pie.WorldPie;
import com.djrapitops.plan.utilities.html.structure.AnalysisPluginsTabContentCreator;
import com.djrapitops.plan.utilities.html.structure.SessionTabStructureCreator;
import com.djrapitops.plan.utilities.html.tables.CommandUseTableCreator;
import com.djrapitops.plan.utilities.html.tables.SessionsTableCreator;
import com.djrapitops.plugin.api.TimeAmount;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Big container object for Data.
 * <p>
 * Contains parts that can be analysed. Each part has their own purpose.
 * <p>
 * Parts contain variables that can be added to. These variables are then
 * analysed using the analysis method.
 * <p>
 * After being analysed the ReplaceMap can be retrieved for replacing
 * placeholders on the server.html file.
 *
 * @author Rsl1122
 * @since 3.5.2
 */
public class AnalysisData extends RawData {

    private long refreshDate;

    private Map<String, Long> analyzedValues;
    private Set<StickyData> stickyMonthData;
    private List<PlayerProfile> players;

    public AnalysisData() {
        analyzedValues = new HashMap<>();
        stickyMonthData = new HashSet<>();
    }

    public void parsePluginsSection(Map<PluginData, AnalysisContainer> containers) {
        String[] navAndTabs = AnalysisPluginsTabContentCreator.createContent(containers);
        addValue("navPluginsTabs", navAndTabs[0]);
        addValue("tabsPlugins", navAndTabs[1]);
    }

    private void addConstants() {
        addValue("version", PlanPlugin.getInstance().getVersion());
        addValue("worldPieColors", Theme.getValue(ThemeVal.GRAPH_WORLD_PIE));
        addValue("gmPieColors", Theme.getValue(ThemeVal.GRAPH_GM_PIE));
        addValue("serverName", Settings.SERVER_NAME.toString().replaceAll("[^a-zA-Z0-9_\\s]", "_"));
        addValue("timeZone", MiscUtils.getTimeZoneOffsetHours());
        addValue("refresh", FormatUtils.formatTimeStamp(refreshDate));

        addValue("activityPieColors", Theme.getValue(ThemeVal.GRAPH_ACTIVITY_PIE));
        addValue("playersGraphColor", Theme.getValue(ThemeVal.GRAPH_PLAYERS_ONLINE));
        addValue("tpsHighColor", Theme.getValue(ThemeVal.GRAPH_TPS_HIGH));
        addValue("tpsMediumColor", Theme.getValue(ThemeVal.GRAPH_TPS_MED));
        addValue("tpsLowColor", Theme.getValue(ThemeVal.GRAPH_TPS_LOW));
        addValue("worldMapColLow", Theme.getValue(ThemeVal.WORLD_MAP_LOW));
        addValue("worldMapColHigh", Theme.getValue(ThemeVal.WORLD_MAP_HIGH));
        addValue("tpsMedium", Settings.THEME_GRAPH_TPS_THRESHOLD_MED.getNumber());
        addValue("tpsHigh", Settings.THEME_GRAPH_TPS_THRESHOLD_HIGH.getNumber());

        addValue("playersMax", ServerProfile.getPlayersMax());
        addValue("playersOnline", ServerProfile.getPlayersOnline());
    }

    public long getRefreshDate() {
        return refreshDate;
    }

    public void analyze(ServerProfile profile) {
        long now = MiscUtils.getTime();
        refreshDate = now;
        long dayAgo = now - TimeAmount.DAY.ms();
        long weekAgo = now - TimeAmount.WEEK.ms();
        long monthAgo = now - TimeAmount.MONTH.ms();

        addConstants();

        got("now", now);
        got("dayAgo", dayAgo);
        got("weekAgo", weekAgo);
        got("monthAgo", monthAgo);

        Map<UUID, List<Session>> sessions = profile.getSessions();
        List<Session> allSessions = profile.getAllSessions();
        allSessions.sort(new SessionStartComparator());

        players = profile.getPlayers();
        List<PlayerProfile> ops = profile.getOps().collect(Collectors.toList());
        long playersTotal = got("playersTotal", players.size());

        List<TPS> tpsData = profile.getTPSData(0, now).collect(Collectors.toList());
        List<TPS> tpsDataDay = profile.getTPSData(dayAgo, now).collect(Collectors.toList());
        List<TPS> tpsDataWeek = profile.getTPSData(weekAgo, now).collect(Collectors.toList());
        List<TPS> tpsDataMonth = profile.getTPSData(monthAgo, now).collect(Collectors.toList());

        List<String> geoLocations = profile.getGeoLocations();
        Map<String, Integer> commandUsage = profile.getCommandUsage();

        directProfileVariables(profile);
        performanceTab(tpsData, tpsDataDay, tpsDataWeek, tpsDataMonth);
        sessionData(monthAgo, sessions, allSessions);
        onlineActivityNumbers(profile, sessions, players);
        geolocationsTab(geoLocations);
        commandUsage(commandUsage);

        addValue("ops", ops.size());
        addValue("playersTotal", playersTotal);

        healthTab(now, players, tpsDataMonth);

        long totalPlaytime = profile.getTotalPlaytime();
        addValue("playtimeTotal", playersTotal != 0 ? FormatUtils.formatTimeAmount(totalPlaytime) : "No Players");
        addValue("playtimeAverage", playersTotal != 0 ? FormatUtils.formatTimeAmount(MathUtils.averageLong(totalPlaytime, playersTotal)) : "-");
    }

    private void healthTab(long now, List<PlayerProfile> players, List<TPS> tpsDataMonth) {
        TreeMap<Long, Map<String, Set<UUID>>> activityData = AnalysisUtils.turnToActivityDataMap(now, players);

        Map<String, Set<UUID>> activityNow = activityData.getOrDefault(now, new HashMap<>());

        String[] activityStackSeries = ActivityStackGraph.createSeries(activityData);
        String activityPieSeries = ActivityPie.createSeries(activityNow);

        addValue("activityStackCategories", activityStackSeries[0]);
        addValue("activityStackSeries", activityStackSeries[1]);
        addValue("activityPieSeries", activityPieSeries);

        Set<UUID> veryActiveNow = activityNow.getOrDefault("Very Active", new HashSet<>());
        Set<UUID> activeNow = activityNow.getOrDefault("Active", new HashSet<>());
        Set<UUID> regularNow = activityNow.getOrDefault("Regular", new HashSet<>());

        addValue("playersRegular", (veryActiveNow.size() + activeNow.size() + regularNow.size()));

        HealthNotes healthNotes = new HealthNotes(this, activityData, tpsDataMonth, now);
        healthNotes.analyzeHealth();

        addValue("healthNotes", healthNotes.parse());
        addValue("healthIndex", healthNotes.getServerHealth());
    }

    private void commandUsage(Map<String, Integer> commandUsage) {
        addValue("commandUniqueCount", String.valueOf(commandUsage.size()));
        addValue("commandCount", MathUtils.sumInt(commandUsage.values().stream().map(i -> (int) i)));
        addValue("tableBodyCommands", HtmlUtils.removeXSS(CommandUseTableCreator.createTable(commandUsage)));
    }

    private void geolocationsTab(List<String> geoLocations) {
        addValue("geoMapSeries", WorldMap.createSeries(geoLocations));
    }

    private void onlineActivityNumbers(ServerProfile profile, Map<UUID, List<Session>> sessions, List<PlayerProfile> players) {
        long now = value("now");
        long dayAgo = value("dayAgo");
        long weekAgo = value("weekAgo");
        long monthAgo = value("monthAgo");

        List<PlayerProfile> newDay = profile.getPlayersWhoRegistered(dayAgo, now).collect(Collectors.toList());
        List<PlayerProfile> newWeek = profile.getPlayersWhoRegistered(weekAgo, now).collect(Collectors.toList());
        List<PlayerProfile> newMonth = profile.getPlayersWhoRegistered(monthAgo, now).collect(Collectors.toList());
        List<PlayerProfile> uniqueDay = profile.getPlayersWhoPlayedBetween(dayAgo, now).collect(Collectors.toList());
        List<PlayerProfile> uniqueWeek = profile.getPlayersWhoPlayedBetween(weekAgo, now).collect(Collectors.toList());
        List<PlayerProfile> uniqueMonth = profile.getPlayersWhoPlayedBetween(monthAgo, now).collect(Collectors.toList());

        int uniqD = uniqueDay.size();
        int uniqW = uniqueWeek.size();
        int uniqM = uniqueMonth.size();
        long newD = got("newD", newDay.size());
        long newW = got("newW", newWeek.size());
        long newM = got("newM", newMonth.size());
        long playersTotal = value("playersTotal");

        addValue("playersDay", uniqD);
        addValue("playersWeek", uniqW);
        addValue("playersMonth", uniqM);
        addValue("playersNewDay", newD);
        addValue("playersNewWeek", newW);
        addValue("playersNewMonth", newM);

        addValue("playersAverage", AnalysisUtils.getUniqueJoinsPerDay(sessions, -1));
        addValue("playersAverageDay", AnalysisUtils.getUniqueJoinsPerDay(sessions, dayAgo));
        addValue("playersAverageWeek", AnalysisUtils.getUniqueJoinsPerDay(sessions, weekAgo));
        addValue("playersAverageMonth", AnalysisUtils.getUniqueJoinsPerDay(sessions, monthAgo));
        addValue("playersNewAverage", AnalysisUtils.getNewUsersPerDay(toRegistered(players), -1, playersTotal));
        addValue("playersNewAverageDay", AnalysisUtils.getNewUsersPerDay(toRegistered(newDay), -1, newD));
        addValue("playersNewAverageWeek", AnalysisUtils.getNewUsersPerDay(toRegistered(newWeek), -1, newW));
        addValue("playersNewAverageMonth", AnalysisUtils.getNewUsersPerDay(toRegistered(newMonth), -1, newM));

        stickiness(now, weekAgo, monthAgo, newDay, newWeek, newMonth);
    }

    private void stickiness(long now, long weekAgo, long monthAgo,
                            List<PlayerProfile> newDay, List<PlayerProfile> newWeek, List<PlayerProfile> newMonth) {
        long newD = value("newD");
        long newW = value("newW");
        long newM = value("newM");

        long fourDaysAgo = now - TimeAmount.DAY.ms() * 4L;
        long twoWeeksAgo = now - TimeAmount.WEEK.ms() * 2L;

        List<PlayerProfile> playersStuckPerMonth = newMonth.stream()
                .filter(p -> p.playedBetween(monthAgo, twoWeeksAgo) && p.playedBetween(twoWeeksAgo, now))
                .collect(Collectors.toList());
        List<PlayerProfile> playersStuckPerWeek = newWeek.stream()
                .filter(p -> p.playedBetween(weekAgo, fourDaysAgo) && p.playedBetween(fourDaysAgo, now))
                .collect(Collectors.toList());

        int stuckPerM = playersStuckPerMonth.size();
        int stuckPerW = playersStuckPerWeek.size();
        got("stuckPerM", stuckPerM);
        got("stuckPerW", stuckPerW);

        addValue("playersStuckMonth", stuckPerM);
        addValue("playersStuckWeek", stuckPerW);
        addValue("playersStuckPercMonth", newM != 0 ? FormatUtils.cutDecimals(MathUtils.averageDouble(stuckPerM, newM) * 100.0) + "%" : "-");
        addValue("playersStuckPercWeek", newW != 0 ? FormatUtils.cutDecimals(MathUtils.averageDouble(stuckPerW, newW) * 100.0) + "%" : "-");

        stuckPerDay(newDay, newMonth, newD, playersStuckPerMonth, playersStuckPerWeek);
    }

    private void stuckPerDay(List<PlayerProfile> newDay, List<PlayerProfile> newMonth, long newD, List<PlayerProfile> playersStuckPerMonth, List<PlayerProfile> playersStuckPerWeek) {
        if (newD != 0) {
            // New Players
            stickyMonthData = newMonth.stream().map(StickyData::new).distinct().collect(Collectors.toSet());
            Set<StickyData> stickyW = playersStuckPerMonth.stream().map(StickyData::new).distinct().collect(Collectors.toSet());
            // New Players who stayed
            Set<StickyData> stickyStuckM = newMonth.stream().map(StickyData::new).distinct().collect(Collectors.toSet());
            Set<StickyData> stickyStuckW = playersStuckPerWeek.stream().map(StickyData::new).distinct().collect(Collectors.toSet());

            int stuckPerD = 0;
            for (PlayerProfile playerProfile : newDay) {
                double probability = AnalysisUtils.calculateProbabilityOfStaying(
                        stickyMonthData, stickyW, stickyStuckM, stickyStuckW, playerProfile
                );
                if (probability >= 0.5) {
                    stuckPerD++;
                }
            }
            addValue("playersStuckDay", stuckPerD);
            addValue("playersStuckPercDay", FormatUtils.cutDecimals(MathUtils.averageDouble(stuckPerD, newD) * 100.0) + "%");
        } else {
            addValue("playersStuckDay", 0);
            addValue("playersStuckPercDay", "-");
        }
    }

    private List<Long> toRegistered(List<PlayerProfile> players) {
        return players.stream().map(PlayerProfile::getRegistered).collect(Collectors.toList());
    }

    private void sessionData(long monthAgo, Map<UUID, List<Session>> sessions, List<Session> allSessions) {
        List<Session> sessionsMonth = allSessions.stream()
                .filter(s -> s.getSessionStart() >= monthAgo)
                .collect(Collectors.toList());
        String[] tables = SessionsTableCreator.createTable(sessions, allSessions);
        String[] sessionContent = SessionTabStructureCreator.createStructure(sessions, allSessions);

        addValue("sessionCount", allSessions.size());
        addValue("accordionSessions", sessionContent[0]);
        addValue("sessionTabGraphViewFunctions", sessionContent[1]);
        addValue("tableBodySessions", tables[0]);
        addValue("listRecentLogins", tables[1]);
        addValue("sessionAverage", FormatUtils.formatTimeAmount(MathUtils.averageLong(allSessions.stream().map(Session::getLength))));
        addValue("punchCardSeries", PunchCardGraph.createSeries(sessionsMonth));

        addValue("deaths", ServerProfile.getDeathCount(allSessions));
        addValue("mobKillCount", ServerProfile.getMobKillCount(allSessions));
        addValue("killCount", ServerProfile.getPlayerKills(allSessions).size());
    }

    private void directProfileVariables(ServerProfile profile) {
        WorldTimes worldTimes = profile.getServerWorldtimes();
        long allTimePeak = profile.getAllTimePeak();
        long lastPeak = profile.getLastPeakDate();

        String playersTableBody = profile.createPlayersTableBody();
        addValue("tablePlayerlist", Settings.PLAYERTABLE_FOOTER.isTrue() ?
                Html.TABLE_PLAYERS_FOOTER.parse(playersTableBody)
                : Html.TABLE_PLAYERS.parse(playersTableBody));
        addValue("worldTotal", FormatUtils.formatTimeAmount(worldTimes.getTotal()));
        String[] seriesData = WorldPie.createSeries(worldTimes);
        addValue("worldSeries", seriesData[0]);
        addValue("gmSeries", seriesData[1]);
        addValue("lastPeakTime", lastPeak != -1 ? FormatUtils.formatTimeStampYear(lastPeak) : "No Data");
        addValue("playersLastPeak", lastPeak != -1 ? profile.getLastPeakPlayers() : "-");
        addValue("bestPeakTime", allTimePeak != -1 ? FormatUtils.formatTimeStampYear(allTimePeak) : "No Data");
        addValue("playersBestPeak", allTimePeak != -1 ? profile.getAllTimePeakPlayers() : "-");
    }

    private void performanceTab(List<TPS> tpsData, List<TPS> tpsDataDay, List<TPS> tpsDataWeek, List<TPS> tpsDataMonth) {
        got("tpsSpikeMonth", ServerProfile.getLowSpikeCount(tpsDataMonth));
        got("tpsSpikeWeek", ServerProfile.getLowSpikeCount(tpsDataWeek));
        got("tpsSpikeDay", ServerProfile.getLowSpikeCount(tpsDataDay));
        addValue("tpsSpikeMonth", value("tpsSpikeMonth"));
        addValue("tpsSpikeWeek", value("tpsSpikeWeek"));
        addValue("tpsSpikeDay", value("tpsSpikeDay"));

        addValue("playersOnlineSeries", PlayerActivityGraph.createSeries(tpsData));
        addValue("tpsSeries", TPSGraph.createSeries(tpsData));
        addValue("cpuSeries", CPUGraph.createSeries(tpsData));
        addValue("ramSeries", RamGraph.createSeries(tpsData));
        addValue("entitySeries", WorldLoadGraph.createSeriesEntities(tpsData));
        addValue("chunkSeries", WorldLoadGraph.createSeriesChunks(tpsData));

        double averageCPUMonth = MathUtils.averageDouble(tpsDataMonth.stream().map(TPS::getCPUUsage).filter(i -> i != 0));
        double averageCPUWeek = MathUtils.averageDouble(tpsDataWeek.stream().map(TPS::getCPUUsage).filter(i -> i != 0));
        double averageCPUDay = MathUtils.averageDouble(tpsDataDay.stream().map(TPS::getCPUUsage).filter(i -> i != 0));

        addValue("tpsAverageMonth", FormatUtils.cutDecimals(MathUtils.averageDouble(tpsDataMonth.stream().map(TPS::getTicksPerSecond))));
        addValue("tpsAverageWeek", FormatUtils.cutDecimals(MathUtils.averageDouble(tpsDataWeek.stream().map(TPS::getTicksPerSecond))));
        addValue("tpsAverageDay", FormatUtils.cutDecimals(MathUtils.averageDouble(tpsDataDay.stream().map(TPS::getTicksPerSecond))));

        addValue("cpuAverageMonth", averageCPUMonth >= 0 ? FormatUtils.cutDecimals(averageCPUMonth) + "%" : "Unavailable");
        addValue("cpuAverageWeek", averageCPUWeek >= 0 ? FormatUtils.cutDecimals(averageCPUWeek) + "%" : "Unavailable");
        addValue("cpuAverageDay", averageCPUDay >= 0 ? FormatUtils.cutDecimals(averageCPUDay) + "%" : "Unavailable");

        addValue("ramAverageMonth", FormatUtils.cutDecimals(MathUtils.averageLong(tpsDataMonth.stream().map(TPS::getUsedMemory).filter(i -> i != 0))));
        addValue("ramAverageWeek", FormatUtils.cutDecimals(MathUtils.averageLong(tpsDataWeek.stream().map(TPS::getUsedMemory).filter(i -> i != 0))));
        addValue("ramAverageDay", FormatUtils.cutDecimals(MathUtils.averageLong(tpsDataDay.stream().map(TPS::getUsedMemory).filter(i -> i != 0))));

        addValue("entityAverageMonth", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataMonth.stream().map(TPS::getEntityCount).filter(i -> i != 0))));
        addValue("entityAverageWeek", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataWeek.stream().map(TPS::getEntityCount).filter(i -> i != 0))));
        addValue("entityAverageDay", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataDay.stream().map(TPS::getEntityCount).filter(i -> i != 0))));

        addValue("chunkAverageMonth", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataMonth.stream().map(TPS::getChunksLoaded).filter(i -> i != 0))));
        addValue("chunkAverageWeek", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataWeek.stream().map(TPS::getChunksLoaded).filter(i -> i != 0))));
        addValue("chunkAverageDay", FormatUtils.cutDecimals(MathUtils.averageInt(tpsDataDay.stream().map(TPS::getChunksLoaded).filter(i -> i != 0))));
    }

    private long got(String key, long v) {
        analyzedValues.put(key, v);
        return v;
    }

    public long value(String key) {
        return analyzedValues.getOrDefault(key, 0L);
    }

    public Set<StickyData> getStickyMonthData() {
        return stickyMonthData;
    }

    public List<PlayerProfile> getPlayers() {
        return players;
    }
}