package com.example.achypur.notepadapp.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.achypur.notepadapp.DAO.CoordinateDao;
import com.example.achypur.notepadapp.DAO.NoteDao;
import com.example.achypur.notepadapp.DAO.PictureDao;
import com.example.achypur.notepadapp.DAO.TagDao;
import com.example.achypur.notepadapp.DAO.TagOfNotesDao;
import com.example.achypur.notepadapp.DAO.UserDao;
import com.example.achypur.notepadapp.Entities.Coordinate;
import com.example.achypur.notepadapp.Entities.Note;
import com.example.achypur.notepadapp.Entities.Tag;
import com.example.achypur.notepadapp.Entities.User;
import com.example.achypur.notepadapp.R;
import com.example.achypur.notepadapp.Session.SessionManager;
import com.example.achypur.notepadapp.Util.DataBaseUtil;
import com.example.achypur.tagview.TagView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NoteActivity extends BaseActivity {
    private final static String NOTE_ID_KEY = "id";
    private final static String REVISE_MODE = "MODE";
    private final static int UPLOAD_KEY = 1;
    private final static int MAP_PERMISSION = 10;


    NoteDao mNoteDao;
    UserDao mUserDao;
    TagDao mTagDao;
    CoordinateDao mCoordinateDao;
    TagOfNotesDao mTagOfNotesDao;
    Note mNote = null;
    HashMap<String, String> mCurrentUser;
    SessionManager mSession;
    LocationManager mLocationManager;
    SupportMapFragment mMapFragment;
    Menu mMenu;
    TagView mTagView;
    Long mLocation = null;
    List<Tag> mCurrentAddTagsList = new ArrayList<>();
    List<Tag> mTagsList = new ArrayList<>();
    List<Tag> mCurrentRemoveTagsList = new ArrayList<>();
    DataBaseUtil mDataBaseUtil;
    List<byte[]> mUriList = new ArrayList<>();
    List<byte[]> mCurrentAddPictures = new ArrayList<>();
    List<byte[]> mCurrentRemovePictures = new ArrayList<>();
    GridViewAdapter mGridViewAdapter;
    PictureDao mPictureDao;
    GridView mGridView;
    User mLoggedUser;
    boolean mReviseMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mNoteDao = new NoteDao(this);
        mUserDao = new UserDao(this);
        mCoordinateDao = new CoordinateDao(this);
        mTagDao = new TagDao(this);
        mTagOfNotesDao = new TagOfNotesDao(this);
        mSession = new SessionManager(this);
        mPictureDao = new PictureDao(this);

        try {
            mNoteDao.open();
            mUserDao.open();
            mCoordinateDao.open();
            mTagDao.open();
            mTagOfNotesDao.open();
            mPictureDao.open();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        mCurrentUser = mSession.getUserDetails();
        mLoggedUser = mUserDao.findUserById(mUserDao.findUserByLogin(mCurrentUser.get(SessionManager.KEY_LOGIN)));
        setContentView(R.layout.activity_note);

        mTagView = (TagView) findViewById(R.id.tag_grid);
        mGridViewAdapter = new GridViewAdapter(this);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.note_parent_layout);

        final EditText title = (EditText) findViewById(R.id.note_edit_title);
        final EditText content = (EditText) findViewById(R.id.note_edit_content);
        final TextView time = (TextView) findViewById(R.id.note_edit_time);
        final Button save = (Button) findViewById(R.id.note_button_submit);
        final Button cancel = (Button) findViewById(R.id.note_button_cancel);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT + 2:00"));
        final Date currentLocalTime = calendar.getTime();
        final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT + 2:00"));
        time.setText("Created at " + dateFormat.format(currentLocalTime));
        save.setEnabled(false);
        mGridView = (GridView) findViewById(R.id.note_edit_pictures);
        mDataBaseUtil = new DataBaseUtil(mTagOfNotesDao, mTagView, mTagDao, mNote);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (title.getText().toString().trim().equals("") ||
                        content.getText().toString().trim().equals("")) {
                    save.setEnabled(false);
                } else {
                    save.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        if (title != null && content != null) {
            title.addTextChangedListener(watcher);
            content.addTextChangedListener(watcher);
        }
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.note_map);
        mMapFragment.getView().setVisibility(View.GONE);
        title.requestFocus();

        initParamsFromIntent(getIntent());


        if (isEditMode()) {
            time.setText("Last modified at " + dateFormat.format(currentLocalTime));
            title.setText(mNote.getmTitle());
            content.setText(mNote.getmContent());

            if (isReviseMode() || mNote.getmUserId() != mLoggedUser.getId()) {
                title.setEnabled(false);
                content.setEnabled(false);
                save.setText("OK");
                title.setTextColor(Color.BLACK);
                content.setTextColor(Color.BLACK);
                mTagView.setEnabled(false);
                mGridView.setEnabled(false);
            } else {
                if (mNote.getmLocation() != 0) {
                    mMapFragment.getView().setVisibility(View.VISIBLE);
                    mMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            Coordinate coordinate = mCoordinateDao.getCoordinateById(mNote.getmLocation());
                            final LatLng currentPosition = new LatLng(coordinate.getLatitude(), coordinate.getLongtitude());
                            googleMap.addMarker(new MarkerOptions().position(currentPosition));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(8), 2000, null);
                        }
                    });
                }
            }
            mUriList = mPictureDao.getAllPicture(mNote.getmId());
            mGridViewAdapter.setList(mUriList);
            mTagsList = mDataBaseUtil.showAllTags(mNote.getmId(), mTagsList);
            mGridView.setAdapter(mGridViewAdapter);
        }

        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final DecorAdapter decorAdapter = new DecorAdapter(NoteActivity.this);
                decorAdapter.setAdapter(mGridViewAdapter);
                mGridView.requestFocus();
                mGridView.setFocusable(true);
                decorAdapter.setListener(new DecorAdapter.Listener() {
                    @Override
                    public void onRemoveClicked(int position) {
                        mCurrentRemovePictures.add(mUriList.get(position));
                        mUriList.remove(position);
                        mGridViewAdapter.setList(mUriList);
                    }
                });
                mGridView.setAdapter(decorAdapter);
                return true;
            }
        });

        mGridView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mGridView.setAdapter(mGridViewAdapter);
                    mGridViewAdapter.notifyDataSetChanged();
                }
            }
        });

        final Intent intent = new Intent();
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode()) {
                    mNote.setmTitle(title.getText().toString().trim());
                    mNote.setmContent(content.getText().toString().trim());
                    mNote.setmModifiedDate(dateFormat.format(currentLocalTime));
                    mNoteDao.updateNote(mNote);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    mNote = mNoteDao.createNote(title.getText().toString().trim(),
                            content.getText().toString().trim(), mUserDao.findUserByLogin
                                    (mCurrentUser.get(SessionManager.KEY_LOGIN)),
                            dateFormat.format(currentLocalTime),
                            dateFormat.format(currentLocalTime), false, mLocation);
                    setResult(RESULT_OK, intent);
                    finish();
                }

                mDataBaseUtil.setmNote(mNote);
                if (!mCurrentAddTagsList.isEmpty()) {
                    mDataBaseUtil.createTagInDb(mCurrentAddTagsList);
                }

                if (!mCurrentRemoveTagsList.isEmpty()) {
                    mDataBaseUtil.deleteTagFromDb(mCurrentRemoveTagsList);
                }

                if (!mCurrentAddPictures.isEmpty()) {
                    for (int i = 0; i < mCurrentAddPictures.size(); i++) {
                        mPictureDao.createPicture(mCurrentAddPictures.get(i), mNote.getmId());
                    }
                }

                if (!mCurrentRemovePictures.isEmpty()) {
                    for (int i = 0; i < mCurrentRemovePictures.size(); i++) {
                        Long id = mPictureDao.findPictureByNoteId(mNote.getmId());
                        mPictureDao.deletePicture(id, mNote.getmId());
                    }
                }
                mNoteDao.close();
                mPictureDao.close();
                mTagDao.close();
                mTagOfNotesDao.close();
                mCoordinateDao.close();
                mUserDao.close();
            }
        });

        final Intent mainActivity = new Intent(this, MainActivity.class);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(mainActivity);
                    mNoteDao.close();
                    mPictureDao.close();
                    mTagDao.close();
                    mTagOfNotesDao.close();
                    mCoordinateDao.close();
                    mUserDao.close();
                    finish();
                }
            });
        }

        mTagView.setListener(new TagView.Listener() {
            @Override
            public void onAddingTag(String tag) {
                Tag item = new Tag(tag);
                if (ifExistTag(item, mCurrentRemoveTagsList)) {
                    mCurrentRemoveTagsList.remove(item);
                    mTagView.addTag(tag);
                    return;
                }

                if (ifExistTag(item, mCurrentAddTagsList)) {
                    alertForTag(item);
                    return;
                }

                if (!ifExistTag(item, mTagsList)) {
                    mCurrentAddTagsList.add(item);
                    mTagView.addTag(tag);
                } else {
                    alertForTag(item);
                }
            }

            @Override
            public void onRemovingTag(String tag) {
                mTagView.removeTag(tag);
                mCurrentRemoveTagsList.add(new Tag(tag));
            }
        });

    }

    private void alertForTag(Tag tag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
        AlertDialog alertDialog = builder.setMessage("Tag " + tag.getmTag() + " already exists").
                setPositiveButton("OK", null).create();
        alertDialog.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isEditMode()) {
            menu.findItem(R.id.note_menu_location).setVisible(true);
            menu.findItem(R.id.note_menu_picture).setVisible(true);
            menu.findItem(R.id.note_menu_check_shared).setVisible(true).
                    setChecked(mNote.getmPolicyStatus());

            if (mNote.getmUserId() != mLoggedUser.getId() || isReviseMode()) {
                menu.findItem(R.id.note_menu_location).setVisible(false);
                menu.findItem(R.id.note_menu_check_shared).setVisible(false).
                        setChecked(mNote.getmPolicyStatus());
                menu.findItem(R.id.note_menu_picture).setVisible(false);
            }

            if (mNote.getmLocation() != 0) {
                menu.findItem(R.id.note_menu_location).setTitle("Edit Location");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        this.mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
        final AlertDialog alertDialog;
        switch (item.getItemId()) {
            case R.id.note_menu_check_shared:
                if (!item.isChecked()) {
                    aBuilder.setMessage("Make this note public?").setCancelable(true)
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mNote.setmPolicyId(true);
                                    item.setChecked(true);
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    alertDialog = aBuilder.create();
                    alertDialog.show();
                    return true;
                } else {
                    mNote.setmPolicyId(false);
                    item.setChecked(false);
                    return true;
                }
            case R.id.note_menu_location:
                Location location = findingLocation(this);
                if (location != null) {
                    LatLng latLng = findingCoordinate(location);
                    String city = findingCityName();
                    dialogWindow(this, latLng, city);
                    return true;
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    return true;
                }

            case R.id.note_menu_picture:
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, UPLOAD_KEY);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isEditMode() {
        return mNote != null;
    }

    private boolean isReviseMode() {
        return mReviseMode;
    }

    private void initParamsFromIntent(Intent intent) {
        mNote = null;

        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(NOTE_ID_KEY) &&
                intent.getExtras().containsKey(REVISE_MODE)) {
            mNote = mNoteDao.getNoteById(intent.getLongExtra(NOTE_ID_KEY, -1));
            mReviseMode = true;
            return;
        }

        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(NOTE_ID_KEY)) {
            mNote = mNoteDao.getNoteById(intent.getLongExtra(NOTE_ID_KEY, -1));
        }

    }

    public static Intent createIntentForAddNote(Context context) {
        Intent intent = new Intent(context, NoteActivity.class);
        return intent;
    }

    public static Intent createIntentForReviseNote(Context context, Long id, boolean reviseMode) {
        Intent intent = new Intent(context, NoteActivity.class);
        intent.putExtra(NOTE_ID_KEY, id);
        intent.putExtra(REVISE_MODE, reviseMode);
        return intent;
    }

    public static Intent createIntentForEditNote(Context context, Long id) {
        Intent intent = new Intent(context, NoteActivity.class);
        intent.putExtra(NOTE_ID_KEY, id);
        return intent;
    }

    private Location findingLocation(final Context context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permissions required", Toast.LENGTH_SHORT).show();
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        final String provider = mLocationManager.getBestProvider(criteria, true);
        Location location = mLocationManager.getLastKnownLocation(provider);
        return location;
    }

    private LatLng findingCoordinate(Location location) {
        if (location != null) {
            if (mNote != null) {
                mNote.setmLocation(mCoordinateDao.createCoordinate(location.getLatitude(),
                        location.getLongitude()));
            } else {
                mLocation = mCoordinateDao.createCoordinate(location.getLatitude(),
                        location.getLongitude());
            }
        }
        try {
            Coordinate coordinate;
            if (mNote != null) {
                coordinate = mCoordinateDao.getCoordinateById(mNote.getmLocation());
            } else {
                coordinate = mCoordinateDao.getCoordinateById(mLocation);
            }
            return new LatLng(coordinate.getLatitude(), coordinate.getLongtitude());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    private String findingCityName() {
        Coordinate coordinate;
        if (mNote != null) {
            coordinate = mCoordinateDao.getCoordinateById(mNote.getmLocation());
        } else {
            coordinate = mCoordinateDao.getCoordinateById(mLocation);
        }
        Geocoder geocoder = new Geocoder(NoteActivity.this, Locale.getDefault());
        List<Address> address = null;
        try {
            address = geocoder.getFromLocation(coordinate.getLatitude(), coordinate.getLongtitude(), 100);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (address.get(0).getLocality() != null) {
            return "Current location: " + address.get(0).getLocality();
        } else {
            return "Can't find your current location";
        }
    }

    private void dialogWindow(Context context, final LatLng latLng, String city) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.layout_dialog);

        final DialogHolder dialogHolder = new DialogHolder();

        dialogHolder.title = (TextView) dialog.findViewById(R.id.dialog_text);
        dialogHolder.ok = (Button) dialog.findViewById(R.id.dialog_ok);
        dialogHolder.cancel = (Button) dialog.findViewById(R.id.dialog_cancel);
        dialogHolder.refresh = (Button) dialog.findViewById(R.id.dialog_refresh);
        dialogHolder.remove = (Button) dialog.findViewById(R.id.dialog_remove);

        dialogHolder.title.setText(city);
        dialogHolder.ok.setText("OK");
        dialogHolder.cancel.setText("CANCEL");
        dialogHolder.remove.setText("Remove");

        dialog.show();

        dialogHolder.ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapFragment.getView().setVisibility(View.VISIBLE);
                mMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        googleMap.addMarker(new MarkerOptions().position(latLng));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
                    }
                });
                dialog.dismiss();
                mMenu.findItem(R.id.note_menu_location).setTitle("Edit Location");
            }
        });

        dialogHolder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNote.setmLocation(Long.valueOf(0));
                dialog.dismiss();
            }
        });

        dialogHolder.refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Location location = findingLocation(NoteActivity.this);
                LatLng latLng = findingCoordinate(location);
                String city = findingCityName();
                dialogWindow(NoteActivity.this, latLng, city);
            }
        });

        dialogHolder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapFragment.getView().getVisibility() != View.VISIBLE) {
                    Toast.makeText(NoteActivity.this, "Nothing to remove", Toast.LENGTH_SHORT).show();
                } else {
                    mMapFragment.getView().setVisibility(View.INVISIBLE);
                    mNote.setmLocation(Long.valueOf(0));
                    mMenu.findItem(R.id.note_menu_location).setTitle("Add Location");
                    dialog.dismiss();
                }
            }
        });
    }

    static class DialogHolder {
        TextView title;
        Button ok;
        Button cancel;
        Button refresh;
        Button remove;

        public DialogHolder() {
        }
    }

    public boolean ifExistTag(Tag tag, List<Tag> list) {
        for (Tag itemTag : list) {
            if (tag.equals(itemTag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPLOAD_KEY && data != null && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();

            try {
                InputStream iStream = getContentResolver().openInputStream(selectedImage);
                byte[] inputData = getBytes(iStream);
                mUriList.add(inputData);
                mCurrentAddPictures.add(inputData);
                mGridViewAdapter.setList(mUriList);
                mGridViewAdapter.notifyDataSetChanged();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    class GridViewAdapter extends BaseAdapter {
        LayoutInflater mLayoutInflater;
        List<byte[]> uriList;

        public GridViewAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setList(List<byte[]> list) {
            uriList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return uriList.size();
        }

        @Override
        public byte[] getItem(int position) {
            return uriList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return uriList.indexOf(uriList.get(position));
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.note_picture, parent, false);
                imageView = (ImageView) convertView.findViewById(R.id.note_picture);
                convertView.setTag(imageView);
            } else {
                imageView = (ImageView) convertView.getTag();

            }
            byte[] uri = getItem(position);
            imageView.setImageBitmap(getImage(uri));
            imageView.setAdjustViewBounds(true);
            return convertView;
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void disabledView(View view) {

    }

}