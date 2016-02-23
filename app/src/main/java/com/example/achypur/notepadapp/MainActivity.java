package com.example.achypur.notepadapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TEST";

    List<Note> mNotes = new ArrayList<Note>();

    NoteListAdapter mAdapter;
    ListView mListView;
    User mUser = new User(null, false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (!mUser.getLogged()) {
            Intent i = new Intent(this, LoginActivity.class);
            startActivityForResult(i, 1);
        }

        final Button button = (Button) findViewById(R.id.edit_button);
        final EditText title = (EditText) findViewById(R.id.edit_title);
        final EditText description = (EditText) findViewById(R.id.edit_description);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (title.getText().toString().trim().equals("") ||
                        description.getText().toString().trim().equals("")) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        title.addTextChangedListener(watcher);
        description.addTextChangedListener(watcher);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNotes.add(new Note(title.getText().toString(), description.getText().toString()));
                mAdapter.notifyDataSetChanged();
            }
        });

        mListView = (ListView) findViewById(R.id.note_list);
        mAdapter = new NoteListAdapter(this);
        mAdapter.setList(mNotes);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: " + position);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                mUser.setLogged(data.getBooleanExtra("logged", false));
                mUser.setLogin(data.getStringExtra("login"));
                Log.i("login",mUser.getLogin());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LinearLayout activity_main = (LinearLayout) findViewById(R.id.activity_main);
        switch (item.getItemId()) {
            case R.id.item_delete:
                Set<Integer> positions = mAdapter.getCheckedPositions();
                for (int i : positions) {
                    mNotes.remove(i);
                }

                if (positions.size() > 0) {
                    mAdapter.clearCheckedPositions();
                    mAdapter.notifyDataSetChanged();
                }

                return true;
            default: {
                super.onOptionsItemSelected(item);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    static class NoteListAdapter extends BaseAdapter {
        List<Note> mNoteList;
        LayoutInflater mInflater;
        Set<Integer> mCheckedPositions = new LinkedHashSet<>();

        public NoteListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public void setList(List<Note> list) {
            mNoteList = list;
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return mNoteList.size();
        }

        @Override
        public Note getItem(int position) {
            return mNoteList.get(position);
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        public Set<Integer> getCheckedPositions() {
            return mCheckedPositions;
        }

        public void clearCheckedPositions() {
            mCheckedPositions.clear();
        }

        class ViewHolderItem {
            TextView title;
            TextView description;
            CheckBox box;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolderItem viewHolderItem;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item, parent, false);

                viewHolderItem = new ViewHolderItem();
                viewHolderItem.title = (TextView) convertView.findViewById(R.id.item_title);
                viewHolderItem.description = (TextView) convertView.findViewById(R.id.item_description);
                viewHolderItem.box = (CheckBox) convertView.findViewById(R.id.item_check);
                convertView.setTag(viewHolderItem);
            } else {
                viewHolderItem = (ViewHolderItem) convertView.getTag();
            }

            convertView.setClickable(true);
            Note note = getItem(position);
            viewHolderItem.box.setClickable(false);
            viewHolderItem.box.setChecked(mCheckedPositions.contains(position));
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewHolderItem.box.setChecked(!viewHolderItem.box.isChecked());
                    if (viewHolderItem.box.isChecked())
                        mCheckedPositions.add(position);
                    else
                        mCheckedPositions.remove(position);
                }
            });

            viewHolderItem.title.setText(note.getTitle());
            viewHolderItem.description.setText(note.getDescription());

            return convertView;
        }
    }
}
