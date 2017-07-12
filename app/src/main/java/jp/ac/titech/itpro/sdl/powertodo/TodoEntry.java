package jp.ac.titech.itpro.sdl.powertodo;

import android.provider.BaseColumns;

/**
 * Created by lyukaixie on 2017/07/12.
 */

public class TodoEntry implements BaseColumns {
    public static final String TABLE_NAME = "todos";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_TIME = "time";
    public static final String COLUMN_NAME_DONE = "done";
}