package com.example.achypur.notepadapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.achypur.notepadapp.DBHelper.DataBaseHelper;
import com.example.achypur.notepadapp.Entities.Coordinate;

import java.sql.SQLException;

/**
 * Created by achypur on 11.03.2016.
 */
public class CoordinateDao {

    private SQLiteDatabase mSqLiteDatabase;
    private DataBaseHelper mDataBaseHelper;
    private String[] mColumns = {DataBaseHelper.KEY_ID,
            DataBaseHelper.KEY_LATITUDE, DataBaseHelper.KEY_LONGTITUDE};

    public CoordinateDao(Context context) {
        mDataBaseHelper = new DataBaseHelper(context);
    }

    public void open() throws SQLException {
        mSqLiteDatabase = mDataBaseHelper.getWritableDatabase();
    }

    public void close() {
        mDataBaseHelper.close();
    }

    public Long createCoordinate(double latitude, double longtitude) {
        Coordinate coordinate = new Coordinate(latitude, longtitude);
        Long id = mSqLiteDatabase.insert(DataBaseHelper.TABLE_COORDINATES, null, getCoordinateContentValues(coordinate));

        coordinate.setId(id);

        return id;
    }

    private ContentValues getCoordinateContentValues(Coordinate coordinate) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseHelper.KEY_LATITUDE, coordinate.getLatitude());
        contentValues.put(DataBaseHelper.KEY_LONGTITUDE, coordinate.getLongtitude());

        return contentValues;
    }

//    public Coordinate getCoordinateByNoteId(Long noteId) {
////        Cursor cursor = mSqLiteDatabase.rawQuery("Select * from " + mDataBaseHelper.TABLE_COORDINATES
////                + " where note_id = ? ", new String[]{String.valueOf(noteId)});
////        if (cursor != null) {
////            cursor.moveToFirst();
////            return cursorToCoordinate(cursor);
////        } else {
////            return null;
////        }
//    }
//
//    private Coordinate cursorToCoordinate(Cursor cursor) {
//        return new Coordinate(
//                cursor.getLong(0),
//                cursor.getDouble(1),
//                cursor.getDouble(2),
//                cursor.getLong(3)
//        );
//    }

}
