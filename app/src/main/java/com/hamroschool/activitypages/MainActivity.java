package com.hamroschool.activitypages;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;


import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.TabLayout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import Database.DBReceiverForProfile;
import Database.DBReceiveTokenAndUserType;
import Database.DataStoreInTokenAndUserType;
import Fragments.CommunicationFragment;
import Fragments.ExamFragment;
import Fragments.StudentInfoFragment;
import service.AdChangeCheckerService;
import service.MessagesService;
import service.PollService;
import service.TeacherAttendanceListService;
import xmlparser.HamroSchoolXmlParser;
import xmlparser.XMLParserForAds;
import xmlparser.XMLParserForMessages;

public class MainActivity extends AppCompatActivity {
    String urll = "http://www.hamroschool.net/myschoolapp/loginapi/getstudentdetails.php?usertoken=";
    InputStream input;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private static final String PREF_NAME = "LOGIN_PREF";
    private static final String PREF_NAME_FIRST_LOGIN = "FIRST LOGIN";
    private static final String PREF_NAME_HAS_INFO_SYNCED_FIRST_TIME = "HAS_INFO_SSYNCED_FIRST_TIME";

    private String received;
    private String result, result_ads, result_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting up toolbar
        Toolbar toolbar;
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        SharedPreferences settings = getSharedPreferences(PREF_NAME_FIRST_LOGIN, 0);
//Get "hasLoggedIn" value. If the value doesn't exist yet false is returned
        boolean firstlogin = settings.getBoolean("isfirst", false);
        if (firstlogin) {
            MainActivity.ConnectToServerForInformation connect = new MainActivity.ConnectToServerForInformation();
            connect.execute("sagar");


            MainActivity.ConnectToServerForMessages connect_msg = new MainActivity.ConnectToServerForMessages();
            connect_msg.execute("sagar");

            MainActivity.ConnectToServerForAds connect_again = new MainActivity.ConnectToServerForAds();
            connect_again.execute("sagar");


            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME_FIRST_LOGIN, 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isfirst", false);
            editor.apply();


        }
        SharedPreferences settings1 = getSharedPreferences(PREF_NAME, 0);
//Get "hasLoggedIn" value. If the value doesn't exist yet false is returned
        boolean hasLogged = settings1.getBoolean("hasLoggedIn", false);
        if (!hasLogged) {
            stopService(new Intent(getApplicationContext(), PollService.class));
            stopService(new Intent(getApplicationContext(), AdChangeCheckerService.class));
            Intent intent = new Intent(this, LoginPage.class);

            startActivity(intent);
            finish();
        }
        //startservices
        startServices();


        //shows back button in toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        viewPager = (ViewPager) findViewById(R.id.viewpager);

        setupViewPager(viewPager);


        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        DBReceiverForProfile dbp = new DBReceiverForProfile(getApplicationContext());
        String name = dbp.getData("School_Name");

        try {
            name = name.toUpperCase();
        } catch (NullPointerException e) {

        }

        TextView title_bar = (TextView) findViewById(R.id.mainToolBar);
        title_bar.setText(name);

    }

    private class ConnectToServerForMessages extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            //has access to main thread(i.e UI thread)

        }

        @Override
        protected String doInBackground(String... params) {
            //all code here runs in background thread
            DBReceiveTokenAndUserType rec = new DBReceiveTokenAndUserType(getApplicationContext());
            //1 is for getting token 2 is for getting user type
            String token = rec.getTokenAndLoginPersonType(1);
            String urlll = "http://www.hamroschool.net/myschoolapp/loginapi/messageservice.php?usertoken=";
            urlll = urlll + token;

            URL url = null;
            try {
                url = new URL(urlll);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (urlConnection.getResponseCode() == 200) {
                    urlConnection.setConnectTimeout(5 * 1000);

                    try {

                        int statusCode = urlConnection.getResponseCode();

                        InputStream in;

                        if (statusCode >= 200 && statusCode < 400) {
                            // Create an InputStream in order to extract the response object
                            in = new BufferedInputStream(urlConnection.getInputStream());
                        } else {

                            in = new BufferedInputStream(urlConnection.getErrorStream());
                        }
                        result_msg = readStream(in);


                    } finally {

                        urlConnection.disconnect();
                    }
                } else {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            XMLParserForMessages hp = new XMLParserForMessages(getApplicationContext());

            DataStoreInTokenAndUserType db_store = new DataStoreInTokenAndUserType(getApplicationContext());
            db_store.storeXMLMSG(result_msg, true, false);

            try {
                InputStream stream = new ByteArrayInputStream(result_msg.getBytes());
                hp.parse(stream);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "sa";
        }

        private String readStream(InputStream in) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            return (result.toString());
        }


        @Override
        protected void onPostExecute(String s) {

        }

    }


    private void startServices() {
        PollService.setServiceAlarm(getApplicationContext(), true);
        AdChangeCheckerService.setServiceAlarm(getApplicationContext(), true);
        MessagesService.setServiceAlarm(getApplicationContext(), true);
        stopService(new Intent(getApplicationContext(), TeacherAttendanceListService.class));
    }


    private class ConnectToServerForAds extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            //has access to main thread(i.e UI thread)

        }

        @Override
        protected String doInBackground(String... params) {
            //all code here runs in background thread
            String urla = "http://www.hamroschool.net/myschoolapp/loginapi/adservice.php?action=getads";
            URL url = null;
            try {
                url = new URL(urla);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (urlConnection.getResponseCode() == 200) {
                    urlConnection.setConnectTimeout(5 * 1000);

                    try {

                        int statusCode = urlConnection.getResponseCode();

                        InputStream in;

                        if (statusCode >= 200 && statusCode < 400) {
                            // Create an InputStream in order to extract the response object
                            in = new BufferedInputStream(urlConnection.getInputStream());
                        } else {

                            in = new BufferedInputStream(urlConnection.getErrorStream());
                        }

                        //converting inputstream to string
                        result_ads = readStream(in);


                    } finally {

                        urlConnection.disconnect();
                    }
                } else {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            XMLParserForAds hp = new XMLParserForAds(getApplicationContext());

            try {
                InputStream stream = new ByteArrayInputStream(result_ads.getBytes());
                hp.parse(stream);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return "sa";
        }


        @Override
        protected void onPostExecute(String s) {

        }

    }


    private class ConnectToServerForInformation extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            //has access to main thread(i.e UI thread)

        }

        @Override
        protected String doInBackground(String... params) {
            //all code here runs in background thread
            DBReceiveTokenAndUserType rec = new DBReceiveTokenAndUserType(getApplicationContext());
            //1 is for getting token 2 is for getting user type
            String token = rec.getTokenAndLoginPersonType(1);
            urll = urll + token;
            URL url = null;
            try {
                url = new URL(urll);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (urlConnection.getResponseCode() == 200) {
                    urlConnection.setConnectTimeout(5 * 1000);

                    try {

                        int statusCode = urlConnection.getResponseCode();

                        InputStream in;

                        if (statusCode >= 200 && statusCode < 400) {
                            // Create an InputStream in order to extract the response object
                            in = new BufferedInputStream(urlConnection.getInputStream());
                        } else {

                            in = new BufferedInputStream(urlConnection.getErrorStream());
                        }
                        result = readStream(in);


                    } finally {

                        urlConnection.disconnect();
                    }
                } else {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            HamroSchoolXmlParser hp = new HamroSchoolXmlParser(getApplicationContext());
            DataStoreInTokenAndUserType db_store = new DataStoreInTokenAndUserType(getApplicationContext());
            db_store.storeXML(result, true, false);

            try {
                InputStream stream = new ByteArrayInputStream(result.getBytes());
                hp.parse(stream);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "sa";
        }

        private String readStream(InputStream in) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            return (result.toString());
        }

        @Override
        protected void onPostExecute(String s) {
            DBReceiverForProfile dbp = new DBReceiverForProfile(getApplicationContext());
            String name = dbp.getData("School_Name").toUpperCase();
            TextView title_bar = (TextView) findViewById(R.id.mainToolBar);
            title_bar.setText(name);


            SharedPreferences sharedPreferences1 = getSharedPreferences(PREF_NAME_HAS_INFO_SYNCED_FIRST_TIME, 0);
            SharedPreferences.Editor editor1 = sharedPreferences1.edit();
            editor1.putBoolean("hasInfoSynced", true);
            editor1.apply();
        }

    }

    private String readStream(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        return (result.toString());
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new StudentInfoFragment(), "Student's Info");
        adapter.addFragment(new ExamFragment(), "Exams");
        adapter.addFragment(new CommunicationFragment(), "comunication");

        viewPager.setAdapter(adapter);

    }


    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            return mFragmentTitleList.get(position);
        }
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu, menu);
        Drawable drawable = menu.findItem(R.id.logout).getIcon();

        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.white));
        menu.findItem(R.id.logout).setIcon(drawable);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:

                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Do you really want to Logout?")
                        .setIcon(R.drawable.logout)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, 0);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("hasLoggedIn", false);
                                editor.apply();
                                //stopping the previous services
                                stopService(new Intent(getApplicationContext(), AdChangeCheckerService.class));
                                stopService(new Intent(getApplicationContext(), PollService.class));
                                stopService(new Intent(getApplicationContext(), MessagesService.class));

                                Intent intent = new Intent(MainActivity.this, LoginChecker.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();

                // /need to edit with database later
                return true;

            case android.R.id.home:

                //for back button in the toolbar
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;


            case R.id.feedbacks:
                //for feedbacks
                Intent intent_email = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + "sagarach65@gmail.com"));
                intent_email.putExtra(Intent.EXTRA_SUBJECT, "Regarding HamroSchool App");
                startActivity(intent_email);
                return  true;


            case R.id.rate_us:
                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }

    }


}
