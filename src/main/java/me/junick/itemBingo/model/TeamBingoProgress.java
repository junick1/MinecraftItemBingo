package me.junick.itemBingo.model;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.util.TimerManager;

import java.util.*;

public class TeamBingoProgress extends BingoProgress {
    protected int teamId;

    public int getTeamId() {
        return teamId;
    }

    public TeamBingoProgress(int teamId) {
        this.teamId = teamId;
    }

}
