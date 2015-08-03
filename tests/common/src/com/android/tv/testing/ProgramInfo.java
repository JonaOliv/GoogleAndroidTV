/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.testing;

import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;

import java.util.concurrent.TimeUnit;

public final class ProgramInfo {
    /**
     * If this is specify for title, it will be generated by adding index.
     */
    public static final String GEN_TITLE = "";

    /**
     * If this is specify for episode title, it will be generated by adding index.
     * Also, season and episode numbers would be generated, too.
     * see: {@link #build} for detail.
     */
    public static final String GEN_EPISODE = "";
    private static final int SEASON_MAX = 10;
    private static final int EPISODE_MAX = 12;

    /**
     * If this is specify for poster art,
     * it will be selected one of {@link #POSTER_ARTS_RES} in order.
     */
    public static final String GEN_POSTER = "GEN";
    private static final int[] POSTER_ARTS_RES = {
            0,
            R.drawable.blue,
            R.drawable.red_large,
            R.drawable.green,
            R.drawable.red,
            R.drawable.green_large,
            R.drawable.blue_small};

    /**
     * If this is specified for duration,
     * it will be selected one of {@link #DURATIONS_MS} in order.
     */
    public static final int GEN_DURATION = -1;
    private static final long[] DURATIONS_MS = {
            TimeUnit.MINUTES.toMillis(15),
            TimeUnit.MINUTES.toMillis(45),
            TimeUnit.MINUTES.toMillis(90),
            TimeUnit.MINUTES.toMillis(60),
            TimeUnit.MINUTES.toMillis(30),
            TimeUnit.MINUTES.toMillis(45),
            TimeUnit.MINUTES.toMillis(60),
            TimeUnit.MINUTES.toMillis(90),
            TimeUnit.HOURS.toMillis(5)};
    private static long DURATIONS_SUM_MS;
    static {
        DURATIONS_SUM_MS = 0;
        for (int i = 0; i < DURATIONS_MS.length; i++) {
            DURATIONS_SUM_MS += DURATIONS_MS[i];
        }
    }

    /**
     * If this is specified for genre,
     * it will be selected one of {@link #GENRES} in order.
     */
    public static final String GEN_GENRE = "GEN";
    private static final String[] GENRES = {
            "",
            TvContract.Programs.Genres.SPORTS,
            TvContract.Programs.Genres.NEWS,
            TvContract.Programs.Genres.SHOPPING,
            TvContract.Programs.Genres.DRAMA,
            TvContract.Programs.Genres.ENTERTAINMENT};

    public final String title;
    public final String episode;
    public final int seasonNumber;
    public final int episodeNumber;
    public final String posterArtUri;
    public final String description;
    public final long durationMs;
    public final String genre;
    public final TvContentRating[] contentRatings;
    public final String resourceUri;

    public static ProgramInfo fromCursor(Cursor c) {
        // TODO: Fill other fields.
        Builder builder = new Builder();
        int index = c.getColumnIndex(TvContract.Programs.COLUMN_TITLE);
        if (index >= 0) {
            builder.setTitle(c.getString(index));
        }
        index = c.getColumnIndex(TvContract.Programs.COLUMN_SHORT_DESCRIPTION);
        if (index >= 0) {
            builder.setDescription(c.getString(index));
        }
        index = c.getColumnIndex(TvContract.Programs.COLUMN_EPISODE_TITLE);
        if (index >= 0) {
            builder.setEpisode(c.getString(index));
        }
        return builder.build();
    }

    public ProgramInfo(String title, String episode, int seasonNumber, int episodeNumber,
            String posterArtUri, String description, long durationMs,
            TvContentRating[] contentRatings, String genre, String resourceUri) {
        this.title = title;
        this.episode = episode;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.posterArtUri = posterArtUri;
        this.description = description;
        this.durationMs = durationMs;
        this.contentRatings = contentRatings;
        this.genre = genre;
        this.resourceUri = resourceUri;
    }

    /**
     * Create a instance of {@link ProgramInfo} whose content will be generated as much as possible.
     */
    public static ProgramInfo create() {
        return new Builder().build();
    }

    /**
     * Get index of the program whose start time equals or less than {@code timeMs} and
     * end time more than {@code timeMs}.
     * @param timeMs target time in millis to find a program.
     * @param channelId used to add complexity to the index between two consequence channels.
     */
    public int getIndex(long timeMs, long channelId) {
        if (durationMs != GEN_DURATION) {
            return Math.max((int) (timeMs / durationMs), 0);
        }
        long startTimeMs = channelId * DURATIONS_MS[((int) (channelId % DURATIONS_MS.length))];
        int index = (int) ((timeMs - startTimeMs) / DURATIONS_SUM_MS) * DURATIONS_MS.length;
        startTimeMs += (index / DURATIONS_MS.length) * DURATIONS_SUM_MS;
        while (startTimeMs + DURATIONS_MS[index % DURATIONS_MS.length] < timeMs) {
            startTimeMs += DURATIONS_MS[index % DURATIONS_MS.length];
            index++;
        }
        return index;
    }

    /**
     * Returns the start time for the program with the position.
     * @param index index returned by {@link #getIndex}
     */
    public long getStartTimeMs(int index, long channelId) {
        if (durationMs != GEN_DURATION) {
            return index * durationMs;
        }
        long startTimeMs = channelId * DURATIONS_MS[((int) (channelId % DURATIONS_MS.length))]
                + (index / DURATIONS_MS.length) * DURATIONS_SUM_MS;
        for (int i = 0; i < index % DURATIONS_MS.length; i++) {
            startTimeMs += DURATIONS_MS[i];
        }
        return startTimeMs;
    }

    /**
     * Return complete {@link ProgramInfo} with the generated value.
     * See: {@link #GEN_TITLE}, {@link #GEN_EPISODE}, {@link #GEN_POSTER}, {@link #GEN_DURATION},
     * {@link #GEN_GENRE}.
     * @param index index returned by {@link #getIndex}
     */
    public ProgramInfo build(Context context, int index) {
        if (!GEN_TITLE.equals(title)
                && !GEN_EPISODE.equals(episode)
                && !GEN_POSTER.equals(posterArtUri)
                && durationMs != GEN_DURATION
                && !GEN_GENRE.equals(genre)) {
            return this;
        }
        return new ProgramInfo(
                GEN_TITLE.equals(title) ? "Title(" + index + ")" : title,
                GEN_EPISODE.equals(episode) ? "Episode(" + index + ")" : episode,
                GEN_EPISODE.equals(episode) ? (index % SEASON_MAX + 1) : seasonNumber,
                GEN_EPISODE.equals(episode) ? (index % EPISODE_MAX + 1) : episodeNumber,
                GEN_POSTER.equals(posterArtUri)
                        ? Utils.getUriStringForResource(context,
                                POSTER_ARTS_RES[index % POSTER_ARTS_RES.length])
                        : posterArtUri,
                description,
                durationMs == GEN_DURATION ? DURATIONS_MS[index % DURATIONS_MS.length] : durationMs,
                contentRatings,
                GEN_GENRE.equals(genre) ? GENRES[index % GENRES.length] : genre,
                resourceUri);
    }

    @Override
    public String toString() {
        return "ProgramInfo{title=" + title
                + ", episode=" + episode
                + ", durationMs=" + durationMs + "}";
    }

    public static class Builder {
        private String mTitle = GEN_TITLE;
        private String mEpisode = GEN_EPISODE;
        private int mSeasonNumber;
        private int mEpisodeNumber;
        private String mPosterArtUri = GEN_POSTER;
        private String mDescription;
        private long mDurationMs = GEN_DURATION;
        private TvContentRating[] mContentRatings;
        private String mGenre = GEN_GENRE;
        private String mResourceUri;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setEpisode(String episode) {
            mEpisode = episode;
            return this;
        }

        public Builder setSeasonNumber(int seasonNumber) {
            mSeasonNumber = seasonNumber;
            return this;
        }

        public Builder setEpisodeNumber(int episodeNumber) {
            mEpisodeNumber = episodeNumber;
            return this;
        }

        public Builder setPosterArtUri(String posterArtUri) {
            mPosterArtUri = posterArtUri;
            return this;
        }

        public Builder setDescription(String description) {
            mDescription = description;
            return this;
        }

        public Builder setDurationMs(long durationMs) {
            mDurationMs = durationMs;
            return this;
        }

        public Builder setContentRatings(TvContentRating[] contentRatings) {
            mContentRatings = contentRatings;
            return this;
        }

        public Builder setGenre(String genre) {
            mGenre = genre;
            return this;
        }

        public Builder setResourceUri(String resourceUri) {
            mResourceUri = resourceUri;
            return this;
        }

        public ProgramInfo build() {
            return new ProgramInfo(mTitle, mEpisode, mSeasonNumber, mEpisodeNumber, mPosterArtUri,
                    mDescription, mDurationMs, mContentRatings, mGenre, mResourceUri);
        }
    }
}
