/*
 * Copyright (c) 2018.
 *
 * This file is part of AvaIre.
 *
 * AvaIre is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AvaIre is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AvaIre.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.avairebot.commands.music.playlist;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.music.PlaylistCommand;
import com.avairebot.contracts.commands.playlist.PlaylistSubCommand;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.database.controllers.PlaylistController;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.NumberUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreatePlaylist extends PlaylistSubCommand {

    public CreatePlaylist(AvaIre avaire, PlaylistCommand command) {
        super(avaire, command);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args, GuildTransformer guild, Collection playlists) {
        String name = args[0].trim().split(" ")[0];
        List<DataRow> playlistItems = playlists.whereLoose("name", name);
        if (!playlistItems.isEmpty()) {
            context.makeWarning(context.i18n("alreadyExists"))
                .set("playlist", name).queue();
            return false;
        }

        if (NumberUtil.isNumeric(name)) {
            context.makeWarning(context.i18n("onlyNumbersInName"))
                .queue();
            return false;
        }

        int playlistLimit = guild.getType().getLimits().getPlaylist().getPlaylists();
        if (playlists.size() >= playlistLimit) {
            context.makeWarning(context.i18n("noMorePlaylistSlots")).queue();
            return false;
        }

        try {
            storeInDatabase(context, name);

            context.makeSuccess(context.i18n("playlistCreated"))
                .set("playlist", name)
                .set("commands", command.generateCommandTrigger(context.getMessage()))
                .queue();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            context.makeError("Error: " + e.getMessage()).queue();
        }

        return false;
    }

    private void storeInDatabase(CommandMessage context, String name) throws SQLException {
        avaire.getDatabase().newQueryBuilder(Constants.MUSIC_PLAYLIST_TABLE_NAME)
            .insert(statement -> {
                statement.set("guild_id", context.getGuild().getId());
                statement.set("name", name, true);
                statement.set("amount", 0);
                statement.set("songs", AvaIre.gson.toJson(new ArrayList<>()));
            });

        PlaylistController.forgetCache(context.getGuild().getIdLong());
    }
}
