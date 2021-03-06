package com.example.achypur.notepadapp.ui;

import android.Manifest;
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
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.achypur.notepadapp.BitmapDecoder;
import com.example.achypur.notepadapp.ForecastFragment;
import com.example.achypur.notepadapp.NoteApplication;
import com.example.achypur.notepadapp.component.DaggerHomeComponent;
import com.example.achypur.notepadapp.component.HomeComponent;
import com.example.achypur.notepadapp.jsonobjects.Forecast;
import com.example.achypur.notepadapp.jsonobjects.ForecastFetcher;
import com.example.achypur.notepadapp.jsonobjects.Rain;
import com.example.achypur.notepadapp.managers.AccountManager;
import com.example.achypur.notepadapp.managers.NoteManager;
import com.example.achypur.notepadapp.module.ActivityModule;
import com.example.achypur.notepadapp.spannable.EmailClickableSpan;
import com.example.achypur.notepadapp.entities.Note;
import com.example.achypur.notepadapp.entities.Tag;
import com.example.achypur.notepadapp.entities.User;
import com.example.achypur.notepadapp.R;
import com.example.achypur.notepadapp.spannable.PhoneCLickableSpan;
import com.example.achypur.notepadapp.spannable.UrlClickableSpan;
import com.example.achypur.notepadapp.view.PictureConvertor;
import com.example.achypur.tagview.TagView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class NoteActivity extends AppCompatActivity {

    private final static String NOTE_ID_KEY = "id";
    private final static String REVISE_MODE = "MODE";
    private final static int UPLOAD_KEY = 1;

    Note mNote = null;
    SupportMapFragment mMapFragment;
    Menu mMenu;
    TagView mTagView;
    List<Tag> mCurrentAddTagsList = new ArrayList<>();
    List<Tag> mTagsList = new ArrayList<>();
    List<Tag> mCurrentRemoveTagsList = new ArrayList<>();
    List<Bitmap> mImgList = new ArrayList<>();
    List<Bitmap> mCurrentAddPictures = new ArrayList<>();
    List<Bitmap> mCurrentRemovePictures = new ArrayList<>();
    ImageGridAdapter mImageGridAdapter;
    GridView mGridView;
    User mLoggedUser;
    boolean mReviseMode = false;
    Long mLocation = (long) 0;
    Forecast mForecast;
    LinearLayout mForecastLayout;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation = null;
    PictureConvertor mPictureConvertor;

    HomeComponent mHomeComponent;
    @Inject
    AccountManager mAccountManager;
    @Inject
    NoteManager mNoteManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        component().inject(this);

        mAccountManager.createUserRepository();
        mNoteManager.createNoteRepo();

        mLoggedUser = mAccountManager.findUserById(mAccountManager.findUserId(mAccountManager.retrieveLogin()));
        mPictureConvertor = PictureConvertor.getInstance();
        mTagView = (TagView) findViewById(R.id.tag_grid);
        mImageGridAdapter = new ImageGridAdapter(this);
        final EditText title = (EditText) findViewById(R.id.note_edit_title);
        final EditText content = (EditText) findViewById(R.id.note_edit_content);
        final TextView time = (TextView) findViewById(R.id.note_edit_time);
        final Button save = (Button) findViewById(R.id.note_button_submit);
        final Button cancel = (Button) findViewById(R.id.note_button_cancel);
//        mForecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        TextView tags = (TextView) findViewById(R.id.note_edit_tag);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        final Date currentLocalTime = calendar.getTime();
        View line = findViewById(R.id.line);
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        time.setText("Created at " + dateFormat.format(currentLocalTime));
        save.setEnabled(false);
        LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttons);
        mGridView = (GridView) findViewById(R.id.note_edit_pictures);
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
        title.requestFocus();
        title.setSelection(title.getText().length());
        if (isEditMode()) {
            time.setText("Last modified at " + mNote.getmModifiedDate());
            tags.setVisibility(View.VISIBLE);

            Reader reader = new StringReader(mNote.getmContent());
            StreamTokenizer streamTokenizer = new StreamTokenizer(reader);
            streamTokenizer.wordChars('@', '@');
            streamTokenizer.wordChars('/', '/');
            streamTokenizer.wordChars(':', ':');
            streamTokenizer.ordinaryChar(' ');

            if (content != null) {
                content.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            content.setMovementMethod(null);
                            content.setTextColor(Color.BLACK);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(content, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
            }

            SpannableStringBuilder builder = new SpannableStringBuilder("");

            try {
                while (streamTokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                    if (content == null)
                        continue;
                    Spannable sp;
                    switch (streamTokenizer.ttype) {
                        case StreamTokenizer.TT_WORD:
                            String word = streamTokenizer.sval;
                            if (Patterns.EMAIL_ADDRESS.matcher(word).matches()) {
                                sp = new SpannableString(word);
                                sp.setSpan(new EmailClickableSpan(this, word), 0, word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.append(sp);
                            } else if (Patterns.WEB_URL.matcher(word).matches()) {
                                sp = new SpannableString(word);
                                sp.setSpan(new UrlClickableSpan(this, word), 0, word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.append(sp);
                            } else {
                                sp = new SpannableString(word);
                                sp.setSpan(null, 0, 0, 0);
                                builder.append(sp);
                            }
                            break;
                        case StreamTokenizer.TT_NUMBER:
                            Double number = streamTokenizer.nval;
                            Integer integer = number.intValue();
                            if (Patterns.PHONE.matcher(integer.toString()).matches()) {
                                sp = new SpannableString(integer.toString());
                                sp.setSpan(new PhoneCLickableSpan(this, integer.toString()), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.append(sp);
                            } else {
                                sp = new SpannableString(String.valueOf(integer));
                                sp.setSpan(null, 0, 0, 0);
                                builder.append(sp);
                            }
                            break;
                        default:
                            char[] chars = Character.toChars(streamTokenizer.ttype);
                            sp = new SpannableString(String.valueOf(chars[0]));
                            sp.setSpan(null, 0, 0, 0);
                            builder.append(sp);
                            break;
                    }
                    content.setMovementMethod(LinkMovementMethod.getInstance());
                    content.setText(builder);

                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (mNoteManager.ifExistForecast(mNote.getmId())) {
                mForecast = mNoteManager.findForecast(mNote.getmId());
            } else {
                mForecast = null;
            }

            if (mForecast != null) {
                showForecastLayout(mForecastLayout);
            }

            title.setText(mNote.getmTitle());

            if (mNote.getmLocation() != 0) {
                mLocation = mNote.getmLocation();
                mMapFragment.getView().setVisibility(View.VISIBLE);
                mMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        googleMap.addMarker(new MarkerOptions().position(mNoteManager.findCurrentPosition(mNote.getmLocation())));
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(mNoteManager.findCurrentPosition(mNote.getmLocation())).zoom(10).build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }
                });
            }
            if (isReviseMode() || mNote.getmUserId() != mLoggedUser.getId()) {
                title.setEnabled(false);
                content.setFocusable(false);
                save.setText("OK");
                title.setTextColor(Color.BLACK);
                content.setTextColor(Color.BLACK);
                mTagView.setEnabled(false);
                mGridView.setLongClickable(false);
                buttonLayout.setVisibility(View.GONE);
                line.setVisibility(View.GONE);
                tags.setVisibility(View.GONE);
            }

            mImgList = mNoteManager.findAllPictureForCurrentNote(mNote.getmId());
            mImageGridAdapter.setExistingImagesList(mImgList);

            mTagView.setList(mNoteManager.findAllTagValueForCurrentNote(mNote.getmId()));
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mNoteManager.showAllTag());
            mTagView.setAdapter(arrayAdapter);
            mTagsList = mNoteManager.findAllTagForCurrentNote(mNote.getmId());
            mGridView.setAdapter(mImageGridAdapter);
        }

        if (mGridView.isLongClickable()) {
            mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    final DecorAdapter decorAdapter = new DecorAdapter(NoteActivity.this);
                    decorAdapter.setAdapter(mImageGridAdapter);
                    decorAdapter.setListener(new DecorAdapter.Listener() {
                        @Override
                        public void onRemoveClicked(int position) {
                            mCurrentRemovePictures.add(mImgList.get(position));
                            mImgList.remove(position);
                            mImageGridAdapter.setExistingImagesList(mImgList);
                        }
                    });
                    mGridView.setAdapter(decorAdapter);
                    return true;
                }
            });
        }

        mGridView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mGridView.setAdapter(mImageGridAdapter);
                    mImageGridAdapter.notifyDataSetChanged();
                }
            }
        });

        final Intent intent = new Intent(this, MainActivity.class);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (isEditMode()) {
                        mNote.setmTitle(title.getText().toString().trim());
                        mNote.setmContent(content.getText().toString().trim());
                        mNote.setmModifiedDate(dateFormat.format(currentLocalTime));
                        mNote.setmLocation(mLocation);
                        mNoteManager.updateNote(mNote);
                        if (mForecast != null) {
                            mNoteManager.createForecast(mForecast, mNote.getmId());
                        } else {
                            mNoteManager.removeForecast(mNote.getmId());
                        }
                        startActivity(createIntentForReviseNote(NoteActivity.this, mNote.getmId(), true));
                        finish();
                    } else {
                        mNote = mNoteManager.createNote(title.getText().toString().trim(),
                                content.getText().toString().trim(), mLoggedUser.getId(),
                                dateFormat.format(currentLocalTime),
                                dateFormat.format(currentLocalTime), false, mLocation);
                        startActivity(intent);
                        finish();
                    }

                    if (!mCurrentAddTagsList.isEmpty()) {
                        mNoteManager.createTags(mCurrentAddTagsList, mNote.getmId(), mNote.getmUserId());
                    }

                    if (!mCurrentRemoveTagsList.isEmpty()) {
                        mNoteManager.deleteTags(mCurrentRemoveTagsList, mNote.getmId());
                    }

                    if (!mCurrentAddPictures.isEmpty()) {
                        for (int i = 0; i < mCurrentAddPictures.size(); i++) {
                            mNoteManager.createPicture(mCurrentAddPictures.get(i), mNote.getmId());
                        }
                    }

                if (!mCurrentRemovePictures.isEmpty()) {
                    mNoteManager.deletePicture(mCurrentRemovePictures, mNote.getmId());
                }
                } finally {
                    mNoteManager.closeTag();
                    mNoteManager.closeTagOfNotes();
                    mNoteManager.closePicture();
                    mNoteManager.closeCoordinate();
                    mNoteManager.closeForecast();
                    mNoteManager.closeNote();
                }
            }
        });

        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNoteManager.closeTag();
                    mNoteManager.closeTagOfNotes();
                    mNoteManager.closePicture();
                    mNoteManager.closeCoordinate();
                    mNoteManager.closeForecast();
                    mNoteManager.closeNote();

                    if (!isEditMode()) {
                        startActivity(new Intent(NoteActivity.this, MainActivity.class));
                        finish();
                    } else {
                        startActivity(createIntentForReviseNote(NoteActivity.this, mNote.getmId(), true));
                        finish();
                    }
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

        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

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
            menu.findItem(R.id.note_menu_edit).setVisible(false);
            if (mNote.getmLocation() != 0) {
                menu.findItem(R.id.note_menu_location).setTitle("Edit location");
            } else {
                menu.findItem(R.id.note_menu_location).setTitle("Add location");
            }
            if (mNote.getmPolicyStatus()) {
                menu.findItem(R.id.note_menu_check_shared).setChecked(true);
            }

//            if (mForecastLayout.getVisibility() == View.VISIBLE) {
//                menu.findItem(R.id.note_menu_weather).setTitle("Update weather");
//            }
        }

        if (isReviseMode()) {
            menu.findItem(R.id.note_menu_location).setVisible(false);
            menu.findItem(R.id.note_menu_check_shared).setVisible(false).
                    setChecked(mNote.getmPolicyStatus());
            menu.findItem(R.id.note_menu_picture).setVisible(false);
            menu.findItem(R.id.note_menu_weather).setVisible(false);
            menu.findItem(R.id.note_menu_edit).setVisible(true);

            if (mNote.getmUserId() != mLoggedUser.getId()) {
                menu.findItem(R.id.note_menu_location).setVisible(false);
                menu.findItem(R.id.note_menu_check_shared).setVisible(false).
                        setChecked(mNote.getmPolicyStatus());
                menu.findItem(R.id.note_menu_edit).setVisible(false);
                menu.findItem(R.id.note_menu_weather).setVisible(false);
                menu.findItem(R.id.note_menu_picture).setVisible(false);
            }
        }

        if (!isReviseMode() && !isEditMode()) {
            menu.findItem(R.id.note_menu_location).setVisible(true);
            menu.findItem(R.id.note_menu_edit).setVisible(false);
            menu.findItem(R.id.note_menu_check_shared).setVisible(false);
            menu.findItem(R.id.note_menu_picture).setVisible(false);
            menu.findItem(R.id.note_menu_weather).setVisible(false);
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
                                    mNote.setmPolicyStatus(true);
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
                    mNote.setmPolicyStatus(false);
                    item.setChecked(false);
                    return true;
                }
            case R.id.note_menu_location:
                locationDialog(mLastLocation);
                return true;
            case R.id.note_menu_picture:
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, UPLOAD_KEY);
                return true;
            case R.id.note_menu_edit:
                startActivity(createIntentForEditNote(this, mNote.getmId()));
                item.setVisible(false);
                finish();
                return true;

            case R.id.note_menu_weather:
                weatherDialog(mLastLocation);
                return true;
            case android.R.id.home:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void locationDialog(final Location location) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_dialog);
        final DialogHolder dialogHolder = new DialogHolder();
        dialogHolder.title = (TextView) dialog.findViewById(R.id.dialog_text);
        dialogHolder.ok = (Button) dialog.findViewById(R.id.dialog_ok);
        dialogHolder.cancel = (Button) dialog.findViewById(R.id.dialog_cancel);
        dialogHolder.refresh = (ImageView) dialog.findViewById(R.id.dialog_refresh);
        dialogHolder.remove = (Button) dialog.findViewById(R.id.dialog_remove);

        List<Address> address;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            if (location != null) {
                address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 2);
                dialogHolder.title.setText(address.get(0).getLocality());
            } else {
                String massage = "Not found";
                dialogHolder.title.setText(massage);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        dialogHolder.ok.setText("OK");
        dialogHolder.cancel.setText("CANCEL");
        dialogHolder.remove.setText("Remove");
        dialogHolder.refresh.setVisibility(View.VISIBLE);

        dialog.show();

        String dialogTitle = "Current location";
        dialog.setTitle(dialogTitle);

        dialogHolder.ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocation != 0) {
                    dialog.dismiss();
                } else {
                    mLocation = mNoteManager.createLocation(location.getLatitude(), location.getLongitude());
                    mMapFragment.getView().setVisibility(View.VISIBLE);
                    mMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(latLng));
                            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(10).build();
                            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            dialog.dismiss();
                            mMenu.findItem(R.id.note_menu_location).setTitle("Edit Location");
                        }
                    });
                }
            }
        });

        dialogHolder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogHolder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapFragment.getView().getVisibility() != View.VISIBLE) {
                    Toast.makeText(NoteActivity.this, "Nothing to remove", Toast.LENGTH_SHORT).show();
                } else {
                    mMapFragment.getView().setVisibility(View.INVISIBLE);
                    mLocation = (long) 0;
                    dialog.dismiss();
                    mMenu.findItem(R.id.note_menu_location).setTitle("Add Location");
                    invalidateOptionsMenu();
                }
            }
        });

        dialogHolder.refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (ContextCompat.checkSelfPermission(NoteActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(NoteActivity.this, "Location permissions required", Toast.LENGTH_SHORT).show();
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                mLocation = mNoteManager.createLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mNote.setmLocation(mLocation);
                locationDialog(mLastLocation);
                dialog.dismiss();
            }
        });
    }

    private void weatherDialog(final Location location) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_dialog);
        final DialogHolder dialogHolder = new DialogHolder();
        dialogHolder.title = (TextView) dialog.findViewById(R.id.dialog_text);
        dialogHolder.ok = (Button) dialog.findViewById(R.id.dialog_ok);
        dialogHolder.cancel = (Button) dialog.findViewById(R.id.dialog_cancel);
        dialogHolder.refresh = (ImageView) dialog.findViewById(R.id.dialog_refresh);
        dialogHolder.remove = (Button) dialog.findViewById(R.id.dialog_remove);

        List<Address> address;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            if (location != null) {
                address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 2);
                dialogHolder.title.setText(address.get(0).getLocality());
            } else {
                String massage = "Not found";
                dialogHolder.title.setText(massage);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        dialogHolder.ok.setText("OK");
        dialogHolder.cancel.setText("CANCEL");
        dialogHolder.remove.setText("Remove");
        dialogHolder.refresh.setVisibility(View.VISIBLE);

        dialog.show();

        String dialogTitle = "Current location";
        dialog.setTitle(dialogTitle);

        dialogHolder.ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNoteManager.ifExistForecast(mNote.getmId())) {
                    dialog.dismiss();
                } else {
                    ForecastFetcher forecastFetcher = new ForecastFetcher();
                    try {
                        if (forecastFetcher.isNetworkAvailable(NoteActivity.this)) {
                            mForecast = forecastFetcher.execute(location).get();
                            showForecastLayout(mForecastLayout);
                            dialog.dismiss();
                            mMenu.findItem(R.id.note_menu_weather).setTitle("Update weather");
                        } else {
                            Toast.makeText(NoteActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        dialogHolder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogHolder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mForecastLayout.getVisibility() != View.VISIBLE) {
                    Toast.makeText(NoteActivity.this, "Nothing to remove", Toast.LENGTH_SHORT).show();
                } else {
                    mForecastLayout.setVisibility(View.GONE);
                    mForecast = null;
                    mMenu.findItem(R.id.note_menu_weather).setTitle("Add weather");
                    dialog.dismiss();
                }
            }
        });

        dialogHolder.refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (ContextCompat.checkSelfPermission(NoteActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(NoteActivity.this, "Location permissions required", Toast.LENGTH_SHORT).show();
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);

                ForecastFetcher forecastFetcher = new ForecastFetcher();
                try {
                    mForecast = forecastFetcher.execute(mLastLocation).get();
                    if (mForecast != null && mNoteManager.ifExistForecast(mNote.getmId())) {
                        mForecast = mNoteManager.updateForecast(mForecast, mNote.getmId());
                        showForecastLayout(mForecastLayout);
                        weatherDialog(mLastLocation);
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                dialog.dismiss();
            }
        });
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
            mNote = mNoteManager.findNote(intent.getLongExtra(NOTE_ID_KEY, -1));
            mReviseMode = true;
            return;
        }

        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(NOTE_ID_KEY)) {
            mNote = mNoteManager.findNote(intent.getLongExtra(NOTE_ID_KEY, -1));
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


    static class DialogHolder {
        TextView title;
        Button ok;
        Button cancel;
        ImageView refresh;
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
                BitmapDecoder bitmapDecoder = new BitmapDecoder();
                mCurrentAddPictures.add(bitmapDecoder.execute(iStream).get());
                mImageGridAdapter.setAddedImagesList(mCurrentAddPictures);
            } catch (InterruptedException | FileNotFoundException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }

    class ImageGridAdapter extends BaseAdapter {
        LayoutInflater mLayoutInflater;
        List<Bitmap> loadedImages = new ArrayList<>();
        List<Bitmap> addedImages = new ArrayList<>();

        public ImageGridAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setExistingImagesList(List<Bitmap> list) {
            loadedImages = list;
            notifyDataSetChanged();
        }

        public void setAddedImagesList(List<Bitmap> list) {
            addedImages = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return loadedImages.size() + addedImages.size();
        }

        @Override
        public Bitmap getItem(int position) {
            if (position >= loadedImages.size()) {
                for (int i = 0; i < addedImages.size(); i++) {
                    return addedImages.get(i);
                }
            }

            return loadedImages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
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
            Bitmap bm = getItem(position);
            imageView.setImageBitmap(bm);
            imageView.setAdjustViewBounds(true);
            return convertView;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isEditMode()) {
            startActivity(createIntentForReviseNote(NoteActivity.this, mNote.getmId(), true));
        }
        if (isReviseMode()) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private void showForecastLayout(View view) {
//        view.setVisibility(View.VISIBLE);
//        TextView city = (TextView) view.findViewById(R.id.forecast_city);
//        TextView temp = (TextView) view.findViewById(R.id.forecast_temp);
//        TextView description = (TextView) view.findViewById(R.id.forecast_description);
//        TextView wind = (TextView) view.findViewById(R.id.forecast_wind);
//        TextView rain = (TextView) view.findViewById(R.id.forecast_rain);
//        ImageView iconWeather = (ImageView) view.findViewById(R.id.forecast_icon);
//
//        city.setText(mForecast.getmCity() + ", " + mForecast.getmOtherInform().getmCountry());
//        temp.setText(String.valueOf(mForecast.getmMain().getmTemperature()) + " C°");
//        iconWeather.setImageBitmap(getImage(mForecast.getIcon()));
//        description.setText(mForecast.getmWeather().get(0).getmDescription());
//        wind.setText(String.valueOf("w: " + mForecast.getmWind().getSpeed()) + " m/s");
//        if (mForecast.getmRain() != null) {
//            rain.setText(String.valueOf("r: " + mForecast.getmRain().getmCount()) + " mm");
//        } else {
//            Rain rainWeather = new Rain();
//            rainWeather.setmCount(0.0);
//            mForecast.setmRain(rainWeather);
//            rain.setText("r: " + String.valueOf(mForecast.getmRain().getmCount()));
//        }
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        if (ContextCompat.checkSelfPermission(NoteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(NoteActivity.this, "Location permissions required", Toast.LENGTH_SHORT).show();
                        }
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);

                        if (mLastLocation == null) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, null, new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {
                                    mLastLocation = location;
                                }
                            });
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.e("Achyp", "985|NoteActivity::onConnectionFailed: ");
                    }
                })
                .addApi(LocationServices.API)
                .build();
    }

    private HomeComponent component() {
        if (mHomeComponent == null) {
            mHomeComponent = DaggerHomeComponent.builder().appComponent(((NoteApplication) getApplication()).component()).activityModule(new ActivityModule(this)).build();
        }
        return mHomeComponent;
    }


}