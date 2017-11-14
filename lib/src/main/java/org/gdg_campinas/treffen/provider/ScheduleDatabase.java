/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package org.gdg_campinas.treffen.provider;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.gdg_campinas.treffen.sync.SyncHelper;
import org.gdg_campinas.treffen.util.AccountUtils;
import org.gdg_campinas.treffen.sync.ConferenceDataHandler;

import org.gdg_campinas.treffen.util.LogUtils;

import static org.gdg_campinas.treffen.util.LogUtils.LOGD;
import static org.gdg_campinas.treffen.util.LogUtils.LOGI;
import static org.gdg_campinas.treffen.util.LogUtils.LOGW;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for {@link ScheduleProvider}.
 */
public class ScheduleDatabase extends SQLiteOpenHelper {
    private static final String TAG = LogUtils.makeLogTag(ScheduleDatabase.class);

    private static final String DATABASE_NAME = "schedule.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_2014_RELEASE_A = 122; // app version 2.0.0, 2.0.1
    private static final int VER_2014_RELEASE_C = 207; // app version 2.1.x
    private static final int VER_2015_RELEASE_A = 208;
    private static final int VER_2015_RELEASE_B = 210;
    private static final int VER_2016_RELEASE_A = 211;
    private static final int VER_2016_RELEASE_B = 212;
    private static final int VER_2017_RELEASE_A = 213;
    private static final int VER_2017_RELEASE_B = 214;
    private static final int VER_2017_RELEASE_C = 215;
    private static final int VER_2017_RELEASE_D = 216; // 5.0.0

    private static final int CUR_DATABASE_VERSION = VER_2017_RELEASE_D;

    private final Context mContext;

    interface Tables {
        String BLOCKS = "blocks";
        String CARDS = "cards";
        String TAGS = "tags";
        String ROOMS = "rooms";
        String SESSIONS = "sessions";
        String MY_SCHEDULE = "myschedule";
        String MY_RESERVATIONS = "myreservations";
        String MY_VIEWED_VIDEO = "myviewedvideos";
        String MY_FEEDBACK_SUBMITTED = "myfeedbacksubmitted";
        String SPEAKERS = "speakers";
        String SESSIONS_TAGS = "sessions_tags";
        String SESSIONS_SPEAKERS = "sessions_speakers";
        String ANNOUNCEMENTS = "announcements";
        String MAPTILES = "mapoverlays";
        String HASHTAGS = "hashtags";
        String FEEDBACK = "feedback";
        String MAPGEOJSON = "mapgeojson";

        String VIDEOS = "videos";

        String SESSIONS_SEARCH = "sessions_search";

        String SEARCH_SUGGEST = "search_suggest";

        String RELATED_SESSIONS = "related_sessions";

        String SESSIONS_JOIN_MYSCHEDULE = "sessions "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name=? ";

        String SESSIONS_JOIN_ROOMS_TAGS = "sessions "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN sessions_tags ON sessions.session_id=sessions_tags.session_id "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name = ? ";

        String SESSIONS_JOIN_ROOMS_TAGS_FEEDBACK_MYSCHEDULE = "sessions "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN sessions_tags ON sessions.session_id=sessions_tags.session_id "
                + "LEFT OUTER JOIN feedback ON sessions.session_id=feedback.session_id "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name = ? ";

        String SESSIONS_JOIN_ROOMS = "sessions "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name =? ";

        String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
                + "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

        String SESSIONS_TAGS_JOIN_TAGS = "sessions_tags "
                + "LEFT OUTER JOIN tags ON sessions_tags.tag_id=tags.tag_id";

        String SESSIONS_SPEAKERS_JOIN_SESSIONS_ROOMS = "sessions_speakers "
                + "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name = ? ";

        String SESSIONS_SEARCH_JOIN_SESSIONS_ROOMS = "sessions_search "
                + "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
                + "LEFT OUTER JOIN myschedule ON sessions.session_id=myschedule.session_id "
                + "AND myschedule.account_name=? "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN myreservations ON sessions.session_id=myreservations.session_id "
                + "AND myreservations.account_name = ? ";

        // When tables get deprecated, add them to this list (so they get correctly deleted
        // on database upgrades).
        enum DeprecatedTables {
            TRACKS("tracks"),
            SESSIONS_TRACKS("sessions_tracks"),
            SANDBOX("sandbox"),
            PEOPLE_IVE_MET("people_ive_met"),
            EXPERTS("experts"),
            PARTNERS("partners"),
            MAPMARKERS("mapmarkers");

            String tableName;

            DeprecatedTables(String tableName) {
                this.tableName = tableName;
            }
        }
    }

    private interface Triggers {
        // Deletes from dependent tables when corresponding sessions are deleted.
        String SESSIONS_TAGS_DELETE = "sessions_tags_delete";
        String SESSIONS_SPEAKERS_DELETE = "sessions_speakers_delete";
        String SESSIONS_MY_SCHEDULE_DELETE = "sessions_myschedule_delete";
        String SESSIONS_MY_RESERVATIONS_DELETE = "sessions_myreservations_delete";
        String SESSIONS_FEEDBACK_DELETE = "sessions_feedback_delete";

        // When triggers get deprecated, add them to this list (so they get correctly deleted
        // on database upgrades).
        interface DeprecatedTriggers {
            String SESSIONS_TRACKS_DELETE = "sessions_tracks_delete";
        }
    }

    public interface SessionsSpeakers {
        String SESSION_ID = "session_id";
        String SPEAKER_ID = "speaker_id";
    }

    public interface SessionsTags {
        String SESSION_ID = "session_id";
        String TAG_ID = "tag_id";
    }

    interface SessionsSearchColumns {
        String SESSION_ID = "session_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID
                + "," + SessionsSearchColumns.BODY + ")";

        String SESSIONS_TAGS_SESSION_ID = Tables.SESSIONS_TAGS + "."
                + SessionsTags.SESSION_ID;

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SESSION_ID;

        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SPEAKER_ID;

        String SPEAKERS_SPEAKER_ID = Tables.SPEAKERS + "." + ScheduleContract.Speakers.SPEAKER_ID;

        String FEEDBACK_SESSION_ID = Tables.FEEDBACK + "." + ScheduleContract.FeedbackColumns.SESSION_ID;
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + ScheduleContract.Blocks.BLOCK_ID + ")";
        String TAG_ID = "REFERENCES " + Tables.TAGS + "(" + ScheduleContract.Tags.TAG_ID + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + ScheduleContract.Rooms.ROOM_ID + ")";
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + ScheduleContract.Sessions.SESSION_ID + ")";
        String VIDEO_ID = "REFERENCES " + Tables.VIDEOS + "(" + ScheduleContract.Videos.VIDEO_ID + ")";
        String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + ScheduleContract.Speakers.SPEAKER_ID + ")";
    }

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, CUR_DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.BLOCKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.BlocksColumns.BLOCK_ID + " TEXT NOT NULL,"
                + ScheduleContract.BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
                + ScheduleContract.BlocksColumns.BLOCK_START + " INTEGER NOT NULL,"
                + ScheduleContract.BlocksColumns.BLOCK_END + " INTEGER NOT NULL,"
                + ScheduleContract.BlocksColumns.BLOCK_TYPE + " TEXT,"
                + ScheduleContract.BlocksColumns.BLOCK_SUBTITLE + " TEXT,"
                + "UNIQUE (" + ScheduleContract.BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TAGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.TagsColumns.TAG_ID + " TEXT NOT NULL,"
                + ScheduleContract.TagsColumns.TAG_CATEGORY + " TEXT NOT NULL,"
                + ScheduleContract.TagsColumns.TAG_NAME + " TEXT NOT NULL,"
                + ScheduleContract.TagsColumns.TAG_ORDER_IN_CATEGORY + " INTEGER,"
                + ScheduleContract.TagsColumns.TAG_COLOR + " TEXT NOT NULL,"
                + ScheduleContract.TagsColumns.TAG_ABSTRACT + " TEXT NOT NULL,"
                + "UNIQUE (" + ScheduleContract.TagsColumns.TAG_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ROOMS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.RoomsColumns.ROOM_ID + " TEXT NOT NULL,"
                + ScheduleContract.RoomsColumns.ROOM_NAME + " TEXT,"
                + ScheduleContract.RoomsColumns.ROOM_FLOOR + " TEXT,"
                + "UNIQUE (" + ScheduleContract.RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + ScheduleContract.SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                + ScheduleContract.Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                + ScheduleContract.SessionsColumns.SESSION_START + " INTEGER NOT NULL,"
                + ScheduleContract.SessionsColumns.SESSION_END + " INTEGER NOT NULL,"
                + ScheduleContract.SessionsColumns.SESSION_LEVEL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_TITLE + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_ABSTRACT + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_REQUIREMENTS + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_KEYWORDS + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_HASHTAG + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_YOUTUBE_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_MODERATOR_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_PDF_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_NOTES_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_CAL_EVENT_ID + " INTEGER,"
                + ScheduleContract.SessionsColumns.SESSION_LIVESTREAM_ID + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_TAGS + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_GROUPING_ORDER + " INTEGER,"
                + ScheduleContract.SessionsColumns.SESSION_SPEAKER_NAMES + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_IMPORT_HASHCODE + " TEXT NOT NULL DEFAULT '',"
                + ScheduleContract.SessionsColumns.SESSION_MAIN_TAG + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_COLOR + " INTEGER,"
                + ScheduleContract.SessionsColumns.SESSION_CAPTIONS_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_PHOTO_URL + " TEXT,"
                + ScheduleContract.SessionsColumns.SESSION_RELATED_CONTENT + " TEXT,"
                + "UNIQUE (" + ScheduleContract.SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + ScheduleContract.SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
                + ScheduleContract.SpeakersColumns.SPEAKER_NAME + " TEXT,"
                + ScheduleContract.SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
                + ScheduleContract.SpeakersColumns.SPEAKER_COMPANY + " TEXT,"
                + ScheduleContract.SpeakersColumns.SPEAKER_ABSTRACT + " TEXT,"
                + ScheduleContract.SpeakersColumns.SPEAKER_URL + " TEXT,"
                + ScheduleContract.SpeakersColumns.SPEAKER_IMPORT_HASHCODE + " TEXT NOT NULL DEFAULT '',"
                + "UNIQUE (" + ScheduleContract.SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.MY_SCHEDULE + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MySchedule.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME + " TEXT NOT NULL,"
                + ScheduleContract.MySchedule.MY_SCHEDULE_DIRTY_FLAG + " INTEGER NOT NULL DEFAULT 1,"
                + ScheduleContract.MySchedule.MY_SCHEDULE_IN_SCHEDULE + " INTEGER NOT NULL DEFAULT 1,"
                + "UNIQUE (" + ScheduleContract.MySchedule.SESSION_ID + ","
                + ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSpeakers.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsSpeakers.SPEAKER_ID + " TEXT NOT NULL " + References.SPEAKER_ID + ","
                + "UNIQUE (" + SessionsSpeakers.SESSION_ID + ","
                + SessionsSpeakers.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_TAGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsTags.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsTags.TAG_ID + " TEXT NOT NULL " + References.TAG_ID + ","
                + "UNIQUE (" + SessionsTags.SESSION_ID + ","
                + SessionsTags.TAG_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ANNOUNCEMENTS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + ScheduleContract.AnnouncementsColumns.ANNOUNCEMENT_ID + " TEXT,"
                + ScheduleContract.AnnouncementsColumns.ANNOUNCEMENT_TITLE + " TEXT NOT NULL,"
                + ScheduleContract.AnnouncementsColumns.ANNOUNCEMENT_ACTIVITY_JSON + " BLOB,"
                + ScheduleContract.AnnouncementsColumns.ANNOUNCEMENT_URL + " TEXT,"
                + ScheduleContract.AnnouncementsColumns.ANNOUNCEMENT_DATE + " INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + Tables.MAPTILES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MapTileColumns.TILE_FLOOR + " INTEGER NOT NULL,"
                + ScheduleContract.MapTileColumns.TILE_FILE + " TEXT NOT NULL,"
                + ScheduleContract.MapTileColumns.TILE_URL + " TEXT NOT NULL,"
                + "UNIQUE (" + ScheduleContract.MapTileColumns.TILE_FLOOR + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.FEEDBACK + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + ScheduleContract.Sessions.SESSION_ID + " TEXT " + References.SESSION_ID + ","
                + ScheduleContract.FeedbackColumns.SESSION_RATING + " INTEGER NOT NULL,"
                + ScheduleContract.FeedbackColumns.ANSWER_RELEVANCE + " INTEGER NOT NULL,"
                + ScheduleContract.FeedbackColumns.ANSWER_CONTENT + " INTEGER NOT NULL,"
                + ScheduleContract.FeedbackColumns.ANSWER_SPEAKER + " INTEGER NOT NULL,"
                + ScheduleContract.FeedbackColumns.COMMENTS + " TEXT,"
                + ScheduleContract.FeedbackColumns.SYNCED + " INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_FEEDBACK_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.FEEDBACK + " "
                + " WHERE " + Qualified.FEEDBACK_SESSION_ID + "=old." + ScheduleContract.Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TABLE " + Tables.HASHTAGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.HashtagColumns.HASHTAG_NAME + " TEXT NOT NULL,"
                + ScheduleContract.HashtagColumns.HASHTAG_DESCRIPTION + " TEXT NOT NULL,"
                + ScheduleContract.HashtagColumns.HASHTAG_COLOR + " INTEGER NOT NULL,"
                + ScheduleContract.HashtagColumns.HASHTAG_ORDER + " INTEGER NOT NULL,"
                + "UNIQUE (" + ScheduleContract.HashtagColumns.HASHTAG_NAME + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.VIDEOS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.VideoColumns.VIDEO_ID + " TEXT NOT NULL,"
                + ScheduleContract.VideoColumns.VIDEO_YEAR + " INTEGER NOT NULL,"
                + ScheduleContract.VideoColumns.VIDEO_TITLE + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_DESC + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_VID + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_TOPIC + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_SPEAKERS + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_THUMBNAIL_URL + " TEXT,"
                + ScheduleContract.VideoColumns.VIDEO_IMPORT_HASHCODE + " TEXT NOT NULL,"
                + "UNIQUE (" + ScheduleContract.VideoColumns.VIDEO_ID + ") ON CONFLICT REPLACE)");

        // Full-text search index. Update using updateSessionSearchIndex method.
        // Use the porter tokenizer for simple stemming, so that "frustration" matches "frustrated."
        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
                + SessionsSearchColumns.SESSION_ID
                + " TEXT NOT NULL " + References.SESSION_ID + ","
                + "UNIQUE (" + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        // Search suggestions
        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

        // Session deletion triggers
        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_TAGS_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_TAGS + " "
                + " WHERE " + Qualified.SESSIONS_TAGS_SESSION_ID + "=old." + ScheduleContract.Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SPEAKERS_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_SPEAKERS + " "
                + " WHERE " + Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=old." + ScheduleContract.Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_MY_SCHEDULE_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.MY_SCHEDULE + " "
                + " WHERE " + Tables.MY_SCHEDULE + "." + ScheduleContract.MySchedule.SESSION_ID +
                "=old." + ScheduleContract.Sessions.SESSION_ID
                + ";" + " END;");

        upgradeFrom2014Cto2015A(db);
        upgradeFrom2015Ato2015B(db);
        upgradeFrom2015Bto2016A(db);
        upgradeFrom2016Ato2016B(db);
        upgradeFrom2016Bto2017A(db);
        upgradeFrom2017Ato2017B(db);
        upgradeFrom2017Bto2017C(db);
        upgradeFrom2017Cto2017D(db);
    }

    private void upgradeFrom2014Cto2015A(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.MY_FEEDBACK_SUBMITTED + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MyFeedbackSubmitted.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + " TEXT NOT NULL,"
                + ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_DIRTY_FLAG +
                " INTEGER NOT NULL DEFAULT 1,"
                + "UNIQUE (" + ScheduleContract.MyFeedbackSubmitted.SESSION_ID + ","
                + ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME +
                ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.MY_VIEWED_VIDEO + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MyViewedVideos.VIDEO_ID + " TEXT NOT NULL " + References.VIDEO_ID + ","
                + ScheduleContract.MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + " TEXT NOT NULL,"
                + ScheduleContract.MyViewedVideos.MY_VIEWED_VIDEOS_DIRTY_FLAG + " INTEGER NOT NULL DEFAULT 1,"
                + "UNIQUE (" + ScheduleContract.MyViewedVideos.VIDEO_ID + ","
                + ScheduleContract.MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + ") ON CONFLICT REPLACE)");
    }

    private void upgradeFrom2015Ato2015B(SQLiteDatabase db) {
        // Note: SpeakersColumns.SPEAKER_URL is unused in 2015. The columns added here are used
        // instead.
        db.execSQL("ALTER TABLE " + Tables.SPEAKERS
                + " ADD COLUMN " + ScheduleContract.SpeakersColumns.SPEAKER_PLUSONE_URL + " TEXT");
        db.execSQL("ALTER TABLE " + Tables.SPEAKERS
                + " ADD COLUMN " + ScheduleContract.SpeakersColumns.SPEAKER_TWITTER_URL + " TEXT");
    }

    private void upgradeFrom2015Bto2016A(SQLiteDatabase db) {
        // Note: Adding photoUrl to tags
        db.execSQL("ALTER TABLE " + Tables.TAGS
                + " ADD COLUMN " + ScheduleContract.TagsColumns.TAG_PHOTO_URL + " TEXT");

        // Adds a timestamp value to my schedule. Used when syncing and merging local and remote
        // data with the version having the more recent timestamp assuming precedence.
        db.execSQL("ALTER TABLE " + Tables.MY_SCHEDULE
                + " ADD COLUMN " + ScheduleContract.MyScheduleColumns.MY_SCHEDULE_TIMESTAMP + " DATETIME");
    }

    private void upgradeFrom2016Ato2016B(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CARDS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.Cards.ACTION_COLOR + " TEXT, "
                + ScheduleContract.Cards.ACTION_TEXT + " TEXT, "
                + ScheduleContract.Cards.ACTION_URL + " TEXT, "
                + ScheduleContract.Cards.BACKGROUND_COLOR + " TEXT, "
                + ScheduleContract.Cards.CARD_ID + " TEXT, "
                + ScheduleContract.Cards.DISPLAY_END_DATE + " INTEGER, "
                + ScheduleContract.Cards.DISPLAY_START_DATE + " INTEGER, "
                + ScheduleContract.Cards.MESSAGE + " TEXT, "
                + ScheduleContract.Cards.TEXT_COLOR + " TEXT, "
                + ScheduleContract.Cards.TITLE + " TEXT,  "
                + ScheduleContract.Cards.ACTION_TYPE + " TEXT,  "
                + ScheduleContract.Cards.ACTION_EXTRA + " TEXT, "
                + "UNIQUE (" + ScheduleContract.Cards.CARD_ID + ") ON CONFLICT REPLACE)");
    }

    private void upgradeFrom2016Bto2017A(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.RELATED_SESSIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.Sessions.SESSION_ID + " TEXT NOT NULL, "
                + ScheduleContract.Sessions.RELATED_SESSION_ID + " TEXT NOT NULL, "
                + "UNIQUE (" + ScheduleContract.Sessions.SESSION_ID + "," + ScheduleContract.Sessions.RELATED_SESSION_ID + ") "
                + "ON CONFLICT IGNORE)");
    }

    private void upgradeFrom2017Ato2017B(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.MAPGEOJSON + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MapGeoJson.GEOJSON + " TEXT NOT NULL)");
    }

    // Adding session reservations
    private void upgradeFrom2017Bto2017C(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.MY_RESERVATIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ScheduleContract.MyReservations.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + ScheduleContract.MyReservations.MY_RESERVATION_ACCOUNT_NAME + " TEXT NOT NULL,"
                + ScheduleContract.MyReservations.MY_RESERVATION_STATUS + " INTEGER NOT NULL DEFAULT "
                + Integer.toString(ScheduleContract.MyReservations.RESERVATION_STATUS_UNRESERVED) + ","
                + ScheduleContract.MyReservations.MY_RESERVATION_TIMESTAMP + " DATETIME, "
                + "UNIQUE (" + ScheduleContract.MyReservations.SESSION_ID + ", "
                + ScheduleContract.MyReservations.MY_RESERVATION_ACCOUNT_NAME + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_MY_RESERVATIONS_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.MY_RESERVATIONS + " "
                + " WHERE " + Tables.MY_RESERVATIONS + "." + ScheduleContract.MyReservations.SESSION_ID +
                "=old." + ScheduleContract.Sessions.SESSION_ID
                + ";" + " END;");

    }

    // Adding block_kind
    private void upgradeFrom2017Cto2017D(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.BLOCKS + " ADD COLUMN " + ScheduleContract.Blocks.BLOCK_KIND + " TEXT");
    }

    /**
     * Updates the session search index. This should be done sparingly, as the queries are rather
     * complex.
     */
    static void updateSessionSearchIndex(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + Tables.SESSIONS_SEARCH);

        db.execSQL("INSERT INTO " + Qualified.SESSIONS_SEARCH
                + " SELECT s." + ScheduleContract.Sessions.SESSION_ID + ",("

                // Full text body
                + ScheduleContract.Sessions.SESSION_TITLE + "||'; '||"
                + ScheduleContract.Sessions.SESSION_ABSTRACT + "||'; '||"
                + "IFNULL(GROUP_CONCAT(t." + ScheduleContract.Speakers.SPEAKER_NAME + ",' '),'')||'; '||"
                + "'')"

                + " FROM " + Tables.SESSIONS + " s "
                + " LEFT OUTER JOIN"

                // Subquery resulting in session_id, speaker_id, speaker_name
                + "(SELECT " + ScheduleContract.Sessions.SESSION_ID + "," + Qualified.SPEAKERS_SPEAKER_ID
                + "," + ScheduleContract.Speakers.SPEAKER_NAME
                + " FROM " + Tables.SESSIONS_SPEAKERS
                + " INNER JOIN " + Tables.SPEAKERS
                + " ON " + Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "="
                + Qualified.SPEAKERS_SPEAKER_ID
                + ") t"

                // Grand finale
                + " ON s." + ScheduleContract.Sessions.SESSION_ID + "=t." + ScheduleContract.Sessions.SESSION_ID
                + " GROUP BY s." + ScheduleContract.Sessions.SESSION_ID);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        Account account = AccountUtils.getActiveAccount(mContext);
        if (account != null) {
            LogUtils.LOGI(TAG, "Cancelling any pending syncs for account");
            ContentResolver.cancelSync(account, ScheduleContract.CONTENT_AUTHORITY);
        }

        // Current DB version. We update this variable as we perform upgrades to reflect
        // the current version we are in.
        int version = oldVersion;

        // Indicates whether the data we currently have should be invalidated as a
        // result of the db upgrade. Default is true (invalidate); if we detect that this
        // is a trivial DB upgrade, we set this to false.
        boolean dataInvalidated = true;

        // Check if we can upgrade from release 2014 C to release 2015 A.
        if (version == VER_2014_RELEASE_C) {
            LogUtils.LOGD(TAG, "Upgrading database from 2014 release C to 2015 release A.");
            upgradeFrom2014Cto2015A(db);
            version = VER_2015_RELEASE_A;
        }

        // Check if we can upgrade from release 2015 A to release 2015 B.
        if (version == VER_2015_RELEASE_A) {
            LogUtils.LOGD(TAG, "Upgrading database from 2015 release A to 2015 release B.");
            upgradeFrom2015Ato2015B(db);
            version = VER_2015_RELEASE_B;
        }

        // Check if we can upgrade from release 2015 B to release 2016 A.
        if (version == VER_2015_RELEASE_B) {
            LogUtils.LOGD(TAG, "Upgrading database from 2015 release B to 2016 release A.");
            upgradeFrom2015Bto2016A(db);
            version = VER_2016_RELEASE_A;
        }

        // Check if we can upgrade from release 2016 release A to 2016 release B.
        if (version == VER_2016_RELEASE_A) {
            LogUtils.LOGD(TAG, "Upgrading database from 2016 release A to 2016 release B.");
            upgradeFrom2016Ato2016B(db);
            version = VER_2016_RELEASE_B;
        }

        // Check if we can upgrade from release 2016 release B to 2017 release A.
        if (version == VER_2016_RELEASE_B) {
            LogUtils.LOGD(TAG, "Upgrading database from 2016 release B to 2017 release A.");
            upgradeFrom2016Bto2017A(db);
            version = VER_2017_RELEASE_A;
        }

        // Check if we can upgrade from release 2017 release A to 2017 release B.
        if (version == VER_2017_RELEASE_A) {
            LogUtils.LOGD(TAG, "Upgrading database from 2017 release A to 2017 release B.");
            upgradeFrom2017Ato2017B(db);
            version = VER_2017_RELEASE_B;
        }

        // Check if we can upgrade from release 2017 release B to 2017 release C.
        if (version == VER_2017_RELEASE_B) {
            LogUtils.LOGD(TAG, "Upgrading database from 2017 release B to 2017 release C.");
            upgradeFrom2017Bto2017C(db);
            version = VER_2017_RELEASE_C;
        }

        // Check if we can upgrade from release 2017 release C to 2017 release D.
        if (version == VER_2017_RELEASE_C) {
            LogUtils.LOGD(TAG, "Upgrading database from 2017 release C to 2017 release D.");
            upgradeFrom2017Cto2017D(db);
            version = VER_2017_RELEASE_D;
        }

        LogUtils.LOGD(TAG, "After upgrade logic, at version " + version);

        // Drop tables that have been deprecated.
        for (Tables.DeprecatedTables deprecatedTable : Tables.DeprecatedTables.values()) {
            db.execSQL(("DROP TABLE IF EXISTS " + deprecatedTable.tableName));
        }

        // At this point, we ran out of upgrade logic, so if we are still at the wrong
        // version, we have no choice but to delete everything and create everything again.
        if (version != CUR_DATABASE_VERSION) {
            LogUtils.LOGW(TAG, "Upgrade unsuccessful -- destroying old data during upgrade");

            // Drop triggers and tables in reverse order of creation.

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.DeprecatedTriggers.SESSIONS_TRACKS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_MY_RESERVATIONS_DELETE);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.MY_RESERVATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MAPGEOJSON);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.RELATED_SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CARDS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MY_VIEWED_VIDEO);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MY_FEEDBACK_SUBMITTED);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_MY_SCHEDULE_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SPEAKERS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_TAGS_DELETE);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.VIDEOS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.HASHTAGS);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_FEEDBACK_DELETE);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.FEEDBACK);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MAPTILES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ANNOUNCEMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TAGS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MY_SCHEDULE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TAGS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);

            onCreate(db);
            version = CUR_DATABASE_VERSION;
        }

        if (dataInvalidated) {
            LogUtils.LOGD(TAG, "Data invalidated; resetting our data timestamp.");
            ConferenceDataHandler.resetDataTimestamp(mContext);
            if (account != null) {
                LogUtils.LOGI(TAG, "DB upgrade complete. Requesting resync.");
                SyncHelper.requestManualSync();
            }
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}
