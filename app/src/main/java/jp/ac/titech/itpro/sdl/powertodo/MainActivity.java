package jp.ac.titech.itpro.sdl.powertodo;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.BooleanResult;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EasyPermissions.PermissionCallbacks {

    private RecyclerView todoList;
    private TodoAdapter todoAdapter;
    private List<Todo> todos = new ArrayList<Todo>();

    private Set<String> pulledTodos = new HashSet<String>();
    private List<Todo> tempTodos = new ArrayList<Todo>();

    final Context context = this;
    private QuickTodoDBHelper quickTodoDBHelper = new QuickTodoDBHelper(context);

    // Parameter list for google calendar API
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                final View v = inflater.inflate(R.layout.create_new_todo, null);


                alertDialogBuilder.setTitle("Create a new TODO")
                        .setView(v)
                        .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                EditText editTitle = (EditText) v.findViewById(R.id.editTitle);
                                EditText editDescription = (EditText) v.findViewById(R.id.editDescription);
                                DatePicker dp = (DatePicker) v.findViewById(R.id.datePicker);

                                int month = dp.getMonth() + 1;
                                String monthStr = "" + ((month >= 10) ? month : ("0" + month));

                                int day = dp.getDayOfMonth();
                                String dayStr = "" + ((day >= 10) ? day : ("0" + day));

                                String date = "" + dp.getYear() + "/" + monthStr + "/" + dayStr;
                                String description = editDescription.getText().toString();

                                Todo newTodo = new Todo(editTitle.getText().toString(), description, date);

                                todoAdapter.addItem(newTodo);
                                insertIntoDB(newTodo);
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // Get RecyclerView and set it up
        todoList = (RecyclerView) findViewById(R.id.todoList);
        todoList.setLayoutManager(new LinearLayoutManager(this));
        todoList.setItemAnimator(new DefaultItemAnimator());
        // Initial the adapter and assign it to 'todoList'
        if(savedInstanceState != null){
            List<String> titleList = savedInstanceState.getStringArrayList("titleList");
            List<String> descriptionList = savedInstanceState.getStringArrayList("descriptionList");
            List<String> timeList = savedInstanceState.getStringArrayList("timeList");
            List<Integer> doneList = savedInstanceState.getIntegerArrayList("doneList");
            for(int i = 0; i < titleList.size(); i++){
                Todo newTodo = new Todo(titleList.get(i), descriptionList.get(i), timeList.get(i));
                newTodo.done = (doneList.get(i) == 1);
                todos.add(newTodo);
            }
        }
        else{
            List<Todo> fromDB = getTodoFromDB();
            if(fromDB.size() > 0) {
                todos = fromDB;
                for(Todo todo : todos){
                    pulledTodos.add(todo.title + todo.description + todo.time);
                }
            }
        }
        todoAdapter = new TodoAdapter(this, todos);
        todoList.setAdapter(todoAdapter);

        // Setup Google Calendar API
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling out Google Calendar API ...");

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }
    /**
     * This is the segment to handle database events
     */
    private long insertIntoDB(Todo todo){
        SQLiteDatabase db = quickTodoDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TodoEntry.COLUMN_NAME_TITLE, todo.title);
        values.put(TodoEntry.COLUMN_NAME_DESCRIPTION, todo.description);
        values.put(TodoEntry.COLUMN_NAME_TIME, todo.time);
        values.put(TodoEntry.COLUMN_NAME_DONE, todo.done ? 1 : 0);

        long newRowId = db.insert(TodoEntry.TABLE_NAME, null, values);
        return newRowId;
    }

    private void deleteFromDB(Todo todo){
        SQLiteDatabase db = quickTodoDBHelper.getWritableDatabase();
        String selection = TodoEntry.COLUMN_NAME_TITLE + " = ? AND " +
                TodoEntry.COLUMN_NAME_DESCRIPTION + " = ? AND " +
                TodoEntry.COLUMN_NAME_TIME + " = ?";
        String[] selectionArgs = {todo.title, todo.description, todo.time};
        db.delete(TodoEntry.TABLE_NAME, selection, selectionArgs);
    }

    private List<Todo> getTodoFromDB(){
        SQLiteDatabase db = quickTodoDBHelper.getReadableDatabase();
        List<Todo> result = new ArrayList<Todo>();
        String[] projection = {
            TodoEntry._ID,
            TodoEntry.COLUMN_NAME_TITLE,
            TodoEntry.COLUMN_NAME_DESCRIPTION,
            TodoEntry.COLUMN_NAME_TIME,
            TodoEntry.COLUMN_NAME_DONE
        };

        Cursor cursor = db.query(
            TodoEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        );
        while (cursor.moveToNext()){
            String title = cursor.getString(cursor.getColumnIndex(TodoEntry.COLUMN_NAME_TITLE));
            String description = cursor.getString(cursor.getColumnIndex(TodoEntry.COLUMN_NAME_DESCRIPTION));
            String time = cursor.getString(cursor.getColumnIndex(TodoEntry.COLUMN_NAME_TIME));

            Todo newTodo = new Todo(title, description, time);
            newTodo.done = (cursor.getInt(cursor.getColumnIndex(TodoEntry.COLUMN_NAME_DONE)) == 1);
            result.add(newTodo);
        }

        return result;
    }
    /**
     * This is the segment to enable kaiten
     */
    @Override
    protected void onResume(){
        super.onResume();
    }

    private ArrayList<String> getTitleList(List<Todo> todoList){
        ArrayList<String> result = new ArrayList<String>();
        for(Todo todo : todoList)
            result.add(todo.title);
        return result;
    }

    private ArrayList<String> getDescriptionList(List<Todo> todoList){
        ArrayList<String> result = new ArrayList<String>();
        for(Todo todo : todoList)
            result.add(todo.description);
        return result;
    }

    private ArrayList<String> getTimeList(List<Todo> todoList){
        ArrayList<String> result = new ArrayList<String>();
        for(Todo todo : todoList)
            result.add(todo.time);
        return result;
    }

    private ArrayList<Integer> getDoneList(List<Todo> todoList){
        ArrayList<Integer> result = new ArrayList<Integer>();
        for(Todo todo : todoList)
            result.add((todo.done) ? 1 : 0);
        return result;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("titleList", getTitleList(todos));
        outState.putStringArrayList("descriptionList", getDescriptionList(todos));
        outState.putStringArrayList("timeList", getTimeList(todos));
        outState.putIntegerArrayList("doneList", getDoneList(todos));
    }

    /**
     * This is the segment to handle google calendar api activities
     * */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void getResultsFromApi() {
        if( ! isGooglePlayServicesAvailable()){
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            // No network connection available.
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }
    /**
    * End Google Calendar API handle segment
    * */


    /**
     * This is the segment to implement EasyPermissions.PermissionCallbacks
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, android.Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //mOutputText.setText(
                    //        "This app requires Google Play Services. Please install " +
                    //                "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }


    /**
     * End implement EasyPermissions.PermissionCallbacks segment
     */

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Delete TODOs")
                    .setMessage("Are you sure to delete completed TODOs?")
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Remove the completed todos
                            for(int j = 0; j < todos.size(); j++){
                                if(todos.get(j).done){
                                    String temp = todos.get(j).title + todos.get(j).description + todos.get(j).time;
                                    deleteFromDB(todos.get(j));
                                    pulledTodos.remove(temp);
                                    todoAdapter.removeItem(j);
                                    j--;
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_all) {
            if(tempTodos.size() > 0){
                while(tempTodos.size() != 0){
                    Todo temp = tempTodos.remove(0);
                    todoAdapter.addItem(temp);
                }
            }
        } else if (id == R.id.nav_quicktodo) {
            if(tempTodos.size() > 0){
                while(tempTodos.size() != 0){
                    Todo temp = tempTodos.remove(0);
                    todoAdapter.addItem(temp);
                }
            }
            for(int i = 0; i < todos.size(); i++){
                if(todos.get(i).description.length() > 0){
                    tempTodos.add(todos.get(i));
                }
            }
            for(int i = 0; i < tempTodos.size(); i++)
                todoAdapter.removeItem(tempTodos.get(i));
        } else if (id == R.id.nav_plan) {
            if(tempTodos.size() > 0){
                while(tempTodos.size() != 0){
                    Todo temp = tempTodos.remove(0);
                    todoAdapter.addItem(temp);
                }
            }
            for(int i = 0; i < todos.size(); i++){
                if(todos.get(i).description.length() == 0){
                    tempTodos.add(todos.get(i));
                }
            }
            for(int i = 0; i < tempTodos.size(); i++)
                todoAdapter.removeItem(tempTodos.get(i));
        } else if (id == R.id.nav_pull) {
            getResultsFromApi();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<Todo>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Quick TODO")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<Todo> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<Todo> getDataFromApi() throws IOException {
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            List<Todo> eventTodos = new ArrayList<Todo>();
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                DateTime start = event.getStart().getDate();
                if(start == null){
                    start = event.getStart().getDateTime();
                }
                String date = start.toString().substring(0, 10).replace('-', '/');
                String title = event.getSummary();
                String description = event.getDescription();
                String location = event.getLocation();
                String finalDescription = "";
                finalDescription += (description == null) ? "" : description;
                finalDescription += (location == null) ? "" : ((description == null) ? location : ("\n" + location));

                Todo newTodo = new Todo(title, finalDescription, date);
                String pulled = title + finalDescription + date;
                if(!pulledTodos.contains(pulled)) {
                    pulledTodos.add(pulled);
                    eventTodos.add(newTodo);
                }
            }
            return eventTodos;
        }


        @Override
        protected void onPreExecute() {
            // mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Todo> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                Toast.makeText(context, "No results returned.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Synchronize succeed!", Toast.LENGTH_LONG).show();
                for(Todo todo : output){
                    todoAdapter.addItem(todo);
                    insertIntoDB(todo);
                }
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    String text = "The following error occurred: " + mLastError.getMessage();
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "Request cancelled.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
