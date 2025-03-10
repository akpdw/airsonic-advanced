/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service.upnp;

import com.google.common.collect.Lists;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Sindre Mehus
 * @version $Id: TagBasedContentDirectory.java 3739 2013-12-03 11:55:01Z sindre_mehus $
 */
public abstract class CustomContentDirectory extends AbstractContentDirectoryService {

    protected static final String CONTAINER_ID_ROOT = "0";
    private static final Logger LOG = LoggerFactory.getLogger(CustomContentDirectory.class);

    @Autowired
    protected SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    protected JWTSecurityService jwtSecurityService;

    public CustomContentDirectory() {
        super(Lists.newArrayList("*"), Lists.newArrayList());
    }

    protected Res createResourceForSong(MediaFile song) {
        Player player = playerService.getGuestPlayer(null);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("ext/stream")
                .queryParam("id", song.getId())
                .queryParam("player", player.getId());

        if (song.isVideo()) {
            builder.queryParam("format", TranscodingService.FORMAT_RAW);
        }

        builder = jwtSecurityService.addJWTToken(User.USERNAME_JWT, builder);

        String url = getBaseUrl() + builder.toUriString();
        if (url.length() > 255) {
            LOG.warn("songResource url > 255: " + song.getPath());
        }
        String suffix = song.isVideo() ? FilenameUtils.getExtension(song.getPath()) : transcodingService.getSuffix(player, song, null);
        String mimeTypeString = StringUtil.getMimeType(suffix);
        MimeType mimeType = mimeTypeString == null ? null : MimeType.valueOf(mimeTypeString);

        Res res = new Res(mimeType, null, url);
        res.setDuration(formatDuration(song.getDuration()));
        return res;
    }

    private String formatDuration(Double seconds) {
        if (seconds == null) {
            return null;
        }
        return StringUtil.formatDuration((long) (seconds * 1000), true);
    }

    protected String getBaseUrl() {
        String dlnaBaseLANURL = settingsService.getDlnaBaseLANURL();
        if (StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new RuntimeException("DLNA Base LAN URL is not set correctly");
        }
        return StringUtils.appendIfMissing(dlnaBaseLANURL, "/");
    }

    protected BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws Exception {
        return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
    }

    @Override
    public BrowseResult search(String containerId,
                               String searchCriteria, String filter,
                               long firstResult, long maxResults,
                               SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setJwtSecurityService(JWTSecurityService jwtSecurityService) {
        this.jwtSecurityService = jwtSecurityService;
    }
}
