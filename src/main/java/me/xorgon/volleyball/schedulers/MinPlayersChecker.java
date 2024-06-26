package me.xorgon.volleyball.schedulers;

import me.xorgon.volleyball.VManager;
import me.xorgon.volleyball.objects.Court;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Elijah on 06/09/2016.
 */
public class MinPlayersChecker implements Runnable {

    private VManager manager;
    private Map<Player, Boolean> warnedPlayers; // Player and whether the warning has been sent.

    public MinPlayersChecker(VManager manager) {
        this.manager = manager;
        warnedPlayers = new HashMap<>();
    }

    @Override
    public void run() {
        String ffMessage = manager.messages.getLeaveGameThreatMessage();
        for (Court court : manager.getCourts().values()) {
            if (court.isStarted()) {
                for (Player player : court.getAllPlayers()) {
                    if (!court.isInCourt(player.getLocation())) {
                        if (!warnedPlayers.containsKey(player)) {
                            warnedPlayers.put(player, false);
                            Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), () -> {
                                if (!court.isInCourt(player.getLocation())) {
                                    if (!ffMessage.isEmpty()) {
                                        player.sendMessage(ffMessage);
                                    }
                                    warnedPlayers.remove(player);
                                    warnedPlayers.put(player, true);
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), new PlayerLeaveScheduler(court, player), 100);
                                }
                            }, 100);
                        }
                    } else {
                        if (warnedPlayers.containsKey(player)) {
                            if (warnedPlayers.get(player)) {
                                String returnToCourtMessage = manager.messages.getReturnToCourtMessage();
                                if (!returnToCourtMessage.isEmpty()) {
                                    player.sendMessage(returnToCourtMessage);
                                }
                            }
                            warnedPlayers.remove(player);
                        }
                    }
                }
            }
        }
    }

    public void removeWarnedPlayer(Player player) {
        if (warnedPlayers.containsKey(player)) {
            warnedPlayers.remove(player);
        }
    }


    public class PlayerLeaveScheduler implements Runnable {

        Court court;
        Player player;

        public PlayerLeaveScheduler(Court court, Player player) {
            this.court = court;
            this.player = player;
        }

        @Override
        public void run() {
            if (!court.isInCourt(player.getLocation()) && warnedPlayers.containsKey(player)) {
                Court.Team team = court.getTeam(player);
                court.removePlayer(player);
                String leftGameMessage = manager.messages.getLeftGameMessage();
                if (!leftGameMessage.isEmpty()) {
                    player.sendMessage(leftGameMessage);
                }
                if (!court.hasEnoughPlayers()) {
                    int redSize = court.getRedPlayers().size();
                    int blueSize = court.getBluePlayers().size();
                    int minSize = court.getMinTeamSize();

                    Court.Team winner = Court.Team.NONE;

                    String forfeitMessage = manager.messages.getForfeitMessage(team);
                    if (!forfeitMessage.isEmpty()) {
                        court.sendAllPlayersMessage(forfeitMessage);
                    }

                    court.endGame(winner);
                }
            }
        }
    }
}
