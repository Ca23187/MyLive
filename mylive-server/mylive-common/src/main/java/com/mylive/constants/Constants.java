package com.mylive.constants;

import java.time.Duration;

public class Constants {
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";

    public static final int DEFAULT_THEME = 1;

    public static final String FILE_FOLDER_TEMP = "temp/";
    public static final String FILE_FOLDER = "file/";
    public static final String FILE_COVER = "cover/";
    public static final String FILE_VIDEO = "video/";

    public static final Long MB = 1024 * 1024L;

    public static final String TOKEN_WEB = "token";
    public static final String TOKEN_ADMIN = "adminToken";
    public static final int COOKIE_TTL_SECONDS = 60 * 60 * 24;
    public static final Duration TOKEN_GRACE_PERIOD = Duration.ofSeconds(30);
    public static final Duration TOKEN_UPDATE_THRESHOLD = Duration.ofDays(1);
    public static final Duration REDIS_TTL_TOKEN_INFO = Duration.ofDays(7);
    public static final Duration REDIS_TTL_ADMIN_TOKEN = Duration.ofDays(1);

    public static final String REDIS_KEY_PREFIX = "mylive:";

    public static final int EMAIL_CODE_LENGTH = 6;
    public static final String REDIS_KEY_EMAIL_CODE = REDIS_KEY_PREFIX + "auth:email-code:";
    public static final Duration REDIS_TTL_EMAIL_CODE = Duration.ofMinutes(15);

    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "auth:checkcode:";
    public static final Duration REDIS_TTL_CHECK_CODE = Duration.ofMinutes(10);

    public static final String REDIS_KEY_UPLOADING_FILE = REDIS_KEY_PREFIX + "file:uploading:";
    public static final Duration REDIS_TTL_UPLOADING_FILE = Duration.ofDays(1);

    public static final String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web:";
    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PREFIX + "token:admin:";

    public static final String REDIS_KEY_SYS_SETTING = REDIS_KEY_PREFIX + "sys-setting:";

    public static final String REDIS_KEY_FILE_TRANSCODE_QUEUE = REDIS_KEY_PREFIX + "file:transcode-queue:";
    public static final int TRANSCODE_THREAD_POOL_NUM = 2;
    public static final Duration TRANSCODE_CONSUME_BLOCK = Duration.ofSeconds(30);

    public static final String REDIS_KEY_FILE_DEL_PATH_LIST = REDIS_KEY_PREFIX + "file:del:";
    public static final Duration REDIS_TTL_FILE_DEL_PATH_LIST = Duration.ofDays(7);

    public static final String REDIS_KEY_VIDEO_ONLINE_COUNT_ONLINE = REDIS_KEY_PREFIX + "video:online:";
    public static final long VIDEO_ONLINE_ZSET_CLEAN_MILLI = 25000;
    public static final Duration VIDEO_ONLINE_ZSET_TTL = Duration.ofSeconds(50);

    public static final String REDIS_KEY_CATEGORY_LIST = REDIS_KEY_PREFIX + "category:list:";

    public static final int HOT_KEYWORDS_DAYS = 7;
    public static final int HOT_KEYWORDS_NUM = 10;
    public static final Duration REDIS_KEY_TTL_HOT_KEY = Duration.ofDays(HOT_KEYWORDS_DAYS);
    public static final String REDIS_KEY_HOT_KEYWORDS_BUCKET = REDIS_KEY_PREFIX + "video:search:bucket:";
    public static final String REDIS_KEY_HOT_KEYWORDS_AGG = REDIS_KEY_PREFIX + "video:search:agg:";
    public static final Duration REDIS_TTL_HOT_KEYWORDS_AGG = Duration.ofMinutes(5);

    public static final String M3U8_NAME = "index.m3u8";

    public static final String TEMP_VIDEO_NAME = "temp.mp4";

    public static final String THUMBNAIL_SUFFIX = "_thumb.png";
    public static final int THUMBNAIL_WIDTH = 200;
    public static final String THUMBNAIL_TYPE = "image/png";

    public static final Integer UPDATE_NICKNAME_COIN = 5;

    public static final int VIDEO_UPLOAD_ID_LENGTH = 15;
    public static final int FILE_NAME_LENGTH = 30;
    public static final int VIDEO_ID_LENGTH = 10;
    public static final int FILE_ID_LENGTH = 20;

    public static final int PAGE_SIZE = 15;
    public static final int PAGE_SIZE_UHOME_DEFAULT = 10;
    public static final int PAGE_SIZE_SEARCH = 30;
    public static final Integer PAGE_SIZE_RECOMMEND_VIDEO = 10;

    public static final String REDIS_KEY_VIDEO_FILE = REDIS_KEY_PREFIX + "video:file:";
    public static final String REDIS_KEY_VIDEO_FILE_NEG = REDIS_KEY_PREFIX + "video:file:neg:";

    public static final Duration REDIS_TTL_VIDEO_FILE = Duration.ofMinutes(30);
    public static final Duration REDIS_TTL_VIDEO_FILE_NEG = Duration.ofMinutes(1);

    public static final String REDIS_KEY_VIDEO_PLAY_COUNT = REDIS_KEY_PREFIX + "video:play:count:";
    public static final long VIDEO_PLAY_COUNT_FLUSH_MILLI = 60000;

    public static final String REDIS_KEY_VIDEO_PLAY_HISTORY = REDIS_KEY_PREFIX + "video:play:history:";
    public static final long VIDEO_HISTORY_FLUSH_MILLI = 60000;

    public static final String REDIS_KEY_USER_COUNT_INFO = REDIS_KEY_PREFIX + "user:count-info:";
    public static final Duration REDIS_TTL_USER_COUNT_INFO = Duration.ofMinutes(5);

    public static final String REDIS_KEY_STAT_PREFIX = REDIS_KEY_PREFIX + "user:stat:";
    public static final String REDIS_KEY_STAT_USER_SETS = REDIS_KEY_STAT_PREFIX + "users:";
    public static final Duration REDIS_TTL_STAT = Duration.ofDays(3);
}
