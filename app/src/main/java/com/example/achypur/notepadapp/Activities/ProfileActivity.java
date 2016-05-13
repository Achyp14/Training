package com.example.achypur.notepadapp.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.achypur.notepadapp.CustomView.ProfilePicture;
import com.example.achypur.notepadapp.DAO.UserDao;
import com.example.achypur.notepadapp.Entities.User;
import com.example.achypur.notepadapp.R;
import com.example.achypur.notepadapp.Session.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;


public class ProfileActivity extends AppCompatActivity {

    private final static int UPLOAD_KEY = 1;

    User mCurrentUser = new User();
    UserDao mUserDao;
    ProfilePicture mProfilePicture;
    SessionManager mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mUserDao = new UserDao(this);

        mProfilePicture = (ProfilePicture) findViewById(R.id.profile_image);
        final EditText firstName = (EditText) findViewById(R.id.profile_first_name);
        final EditText login = (EditText) findViewById(R.id.profile_login);
        final EditText email = (EditText) findViewById(R.id.profile_email);
        Button upload = (Button) findViewById(R.id.profile_upload_button);
        final Button ok = (Button) findViewById(R.id.profile_submit_button);
        Button cancel = (Button) findViewById(R.id.profile_cancel_button);
        Button changePassword = (Button) findViewById(R.id.profile_change_password);

        try {
            mUserDao.open();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        mSession = new SessionManager(this);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            Long id = (Long) extras.get("userId");
            mCurrentUser = mUserDao.findUserById(id);
        }

        firstName.setText(mCurrentUser.getName());
        login.setText(mCurrentUser.getLogin());
        if (mCurrentUser.getImage() != null) {
            mProfilePicture.setImageBitmap(byteToBitMap(mCurrentUser.getImage()));
        } else {
            mProfilePicture.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.people));
        }

        if (!mCurrentUser.getEmail().equals("") && email != null) {
            email.setText(mCurrentUser.getEmail());
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                for (User user : mUserDao.getAllUsers()) {
                    if (user.getLogin().equals(login.getText().toString()) && !user.getLogin().equals(mCurrentUser.getLogin())) {
                        ok.setEnabled(false);
                        Toast.makeText(ProfileActivity.this, "Login is already exist", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        ok.setEnabled(true);
                    }
                }
                if (login.getText().toString().equals("") || firstName.getText().toString().equals("")) {
                    Toast.makeText(ProfileActivity.this, "Field can not be empty ", Toast.LENGTH_SHORT).show();
                    ok.setEnabled(false);
                } else {
                    ok.setEnabled(true);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, UPLOAD_KEY);
            }
        });


        login.addTextChangedListener(textWatcher);
        if (ok != null) {
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateUser(firstName.getText().toString(), login.getText().toString(),
                            email.getText().toString(), mCurrentUser.getPassword(), mCurrentUser.getImage());
                    mSession.createLoginSession(login.getText().toString(), mCurrentUser.getPassword());

                    finish();
                }
            });
        }

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertBuilder();
            }
        });
    }

    public Bitmap byteToBitMap(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPLOAD_KEY && data != null && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();

            try {
                InputStream iStream = getContentResolver().openInputStream(selectedImage);
                byte[] image = getBytes(iStream);
                Bitmap bitmap = byteToBitMap(image);
                mCurrentUser.setImage(image);
                mProfilePicture.setImageBitmap(bitmap);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void updateUser(String name, String login, String email, String password, byte[] image) {
        User user = new User(mCurrentUser.getId(), login, name, email, password, null, image);
        mUserDao.updateUser(user);
    }

    public void alertBuilder() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog alertDialog;
        LayoutInflater layoutInflater = this.getLayoutInflater();
        final ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.change_password_layout, null);
        alertDialog = builder.setView(viewGroup).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText oldPassword = (EditText) viewGroup.findViewById(R.id.profile_old_password);
                EditText newPassword = (EditText) viewGroup.findViewById(R.id.profile_new_password);
                EditText newConfirmPassword = (EditText) viewGroup.findViewById(R.id.profile_confirm_new_password);

                if (!oldPassword.getText().toString().trim().equals("") &&
                        !newPassword.getText().toString().trim().equals("") &&
                        !newConfirmPassword.getText().toString().trim().equals("")
                        && newPassword.getText().toString().trim().equals(newConfirmPassword.getText().toString().trim())
                        && oldPassword.getText().toString().trim().equals(mCurrentUser.getPassword())) {
                    mCurrentUser.setPassword(newPassword.getText().toString().trim());
                    Toast.makeText(ProfileActivity.this, "Password was changed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Code is incorrect", Toast.LENGTH_SHORT).show();
                }
            }
        }).setNegativeButton("Cancel", null).create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
