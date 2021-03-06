package com.example.achypur.notepadapp.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.achypur.notepadapp.dbhelper.DataBaseHelper;
import com.example.achypur.notepadapp.entities.Picture;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by achypur on 07.04.2016.
 */
public class PictureDao {
    private SQLiteDatabase mSqLiteDatabase;
    private DataBaseHelper mDataBaseHelper;
    private String[] mColumns = {DataBaseHelper.KEY_ID, DataBaseHelper.KEY_PICTURE, DataBaseHelper.KEY_NOTE_ID};

    public PictureDao(Context context) {
        mDataBaseHelper = new DataBaseHelper(context);
    }

    public void open() throws SQLException {
        mSqLiteDatabase = mDataBaseHelper.getWritableDatabase();
    }

    public void close() {
        mDataBaseHelper.close();
    }

    private ContentValues getPictureContentValues(Picture picture) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseHelper.KEY_PICTURE, picture.getHash());
        contentValues.put(DataBaseHelper.KEY_NOTE_ID, picture.getNoteId());
        return contentValues;
    }

    private Picture cursorToPicture(Cursor cursor) {
        return new Picture(cursor.getLong(0), cursor.getInt(1), cursor.getLong(2));
    }

    public Picture createPicture(Integer hash, Long noteId) {
        Picture picture = new Picture(hash, noteId);
        Long id = mSqLiteDatabase.insert(DataBaseHelper.TABLE_TAG_PICTURE, null, getPictureContentValues(picture));
        picture.setmId(id);

        return picture;
    }

    public List<Integer> getAllPicture(Long id) {
        Cursor cursor = mSqLiteDatabase.rawQuery("select * from " +
                DataBaseHelper.TABLE_TAG_PICTURE + " where note_id = ? ", new String[]{String.valueOf(id)});

        return addPictureToList(cursor);
    }

    public List<Integer> addPictureToList(Cursor cursor) {
        List<Integer> pictureList = new ArrayList<>();
        Picture picture;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                picture = cursorToPicture(cursor);
                pictureList.add(picture.getHash());
                cursor.moveToNext();
            }
        }
        cursor.close();
        return pictureList;
    }

    public List<Long> addIdToList(Cursor cursor) {
        List<Long> idList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                idList.add(cursor.getLong(0));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return idList;
    }

    public void deletePicture(Long id, Long noteId) {
        mSqLiteDatabase.delete(mDataBaseHelper.TABLE_TAG_PICTURE, " id = ? and note_id = ? ",
                new String[]{String.valueOf(id), String.valueOf(noteId)});
    }

    public Long findPictureByNoteId(Long id) {
        Cursor cursor = mSqLiteDatabase.rawQuery("select id from " + mDataBaseHelper.TABLE_TAG_PICTURE
                + " where note_id = ? ", new String[]{String.valueOf(id)});

        if (cursor.getCount() <= 0) {
            return null;
        } else {
            cursor.moveToFirst();
            return cursor.getLong(0);

        }
    }

    public Picture findPictureById(Long id) {
        Cursor cursor = mSqLiteDatabase.rawQuery("select * from " + DataBaseHelper.TABLE_TAG_PICTURE + " where id = ?", new String[]{id.toString()});
        cursor.moveToFirst();

        Log.e("Achyp", "117|PictureDao::findPictureById: " + cursor.getCount());
        return cursorToPicture(cursor);
    }

    public void deletePictureById(Long id) {
        mSqLiteDatabase.delete(DataBaseHelper.TABLE_TAG_PICTURE, " id = ? ", new String[] {id.toString()});
    }

    public List<Long> getAllPictureId(Long noteId) {
        Cursor cursor = mSqLiteDatabase.rawQuery("select * from " + DataBaseHelper.TABLE_TAG_PICTURE + " where note_id = ? " , new String[] {noteId.toString()});

        return addIdToList(cursor);

    }


}
