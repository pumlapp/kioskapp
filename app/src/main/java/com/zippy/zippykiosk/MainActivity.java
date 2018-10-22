package com.zippy.zippykiosk;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;


import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.HitBuilders;
import com.zippy.zippykiosk.rest.AccessTokenStore;
import com.zippy.zippykiosk.rest.Business;
import com.zippy.zippykiosk.rest.Device;
import com.zippy.zippykiosk.rest.User;
import com.zippy.zippykiosk.rest.ZippyApi;


import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by KB on 7/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class MainActivity extends AppCompatActivity {
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";
    private static final String PREF_ACCOUNT_GROUP = "pref_account_group";
    private static final String PREF_ACCOUNT_DETAILS = "pref_account_details";
    private static final String PREF_ACCOUNT_LOGIN = "pref_account_login";
    private static final String PREF_ABOUT = "about_zippykiosk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        /*
        int retServiceAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if(retServiceAvailable != ConnectionResult.SUCCESS) {
            Log.i("GooglePlayServices " + (
                   retServiceAvailable == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ? "version update required" :
                   retServiceAvailable == ConnectionResult.SERVICE_MISSING ? "version update required" :
                   retServiceAvailable == ConnectionResult.SERVICE_UPDATING ? "version update required" :
                   retServiceAvailable == ConnectionResult.SERVICE_DISABLED ? "version update required" :
                   retServiceAvailable == ConnectionResult.SERVICE_INVALID ? "version update required" :
                   "not available"
            ));
        }
        */
        int versionCode = Utils.getAppVersion(this);
        final SharedPreferences prefs = AppPrefs.getSharedPreferences(this);

        int storedVersionCode = prefs.getInt(AppPrefs.PREF_APP_VERSION, -1);

        if (storedVersionCode < versionCode) {
            // First app start after install or after upgrade
            // New install if storedVersionCode==-1
            // At the moment we don't have anything to do here, but may want to know prev version
            // of app at some stage, or when app first runs after upgrade
            prefs.edit()
                    .putInt(AppPrefs.PREF_APP_VERSION, versionCode)
                    .apply();
        }

        if (savedInstanceState == null) {
            if (prefs.getBoolean(AppPrefs.PREF_KIOSK_MODE, false)) {

                if(!isTaskRoot()){
                    finish();
                    return;
                }

                // If new update is ready, then prompt user to install it
                /*
                if(prefs.getInt(AppPrefs.APP_UPDATE_VERSION,-1) > versionCode) {
                    String path = prefs.getString(AppPrefs.APP_UPDATE_FILE,"");
                    if(!TextUtils.isEmpty(path)) {

                        final File apkFile = new File(path);
                        if (apkFile.exists()) {
                            //requestWindowFeature(Window.FEATURE_NO_TITLE);
                            //setContentView(R.layout.activity_sendecg);
                            Intent intent = new Intent();
                            intent.setClass(MainActivity.this, UpgradeActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                }
                */
                
                //KioskApp.setAnalyticsUserIdentifier();
                KioskApp.tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("App Kiosk Mode")
                        .setAction("Started")
                        .setLabel("Auto Started")
                        .build());

                Intent intent = new Intent();
                intent.setClass(this, KioskActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set up the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle("  ZippyKiosk");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setLogo(R.mipmap.ic_launcher);
        }


        // http://stackoverflow.com/questions/14184182/why-wont-fragment-retain-state-when-screen-is-rotated
        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();


        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        finish();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            Preference pref = findPreference(PREF_ABOUT);
            if (pref != null) {
                String versionName = "unknown";
                Context context = getActivity().getApplicationContext();
                try {
                    versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                @SuppressWarnings("StringBufferReplaceableByString")
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append("ZippyKiosk version ");
                strBuilder.append(versionName);
                strBuilder.append("\n(c) Copyright 2015 Zippy.com.au.");
                strBuilder.append("\nDevice Serial No.: ");
                strBuilder.append(Utils.getAndroidDeviceID(getActivity()));
                strBuilder.append("\nDevice IMEI: ");
                strBuilder.append(Utils.getTelephonyDeviceID(getActivity()));
                strBuilder.append("\nDevice model: ");
                strBuilder.append(Build.MODEL);
                pref.setSummary(strBuilder.toString());
            }

            updateAccountPrefs();



            pref = findPreference(PREF_KIOSK_MODE);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AccessTokenStore accessTokenStore = new AccessTokenStore(getActivity().getApplication());
                    if (accessTokenStore.getEmail() != null && Business.isRegistered(getActivity())) {
                        AppPrefs.getSharedPreferences(getActivity()).edit()
                                .putBoolean("bootstart", false)
                                .putBoolean(AppPrefs.PREF_KIOSK_MODE, true)
                                .commit();

                        KioskApp.setAnalyticsUserIdentifier();
                        KioskApp.tracker.send(new HitBuilders.EventBuilder()
                                .setCategory("App Kiosk Mode")
                                .setAction("Started")
                                .setLabel("User Started")
                                .build());

                        final Intent intent = new Intent();
                        intent.setClass(getActivity(), KioskActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                    return true;
                }
            });

            findPreference(PREF_ACCOUNT_LOGIN).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AccessTokenStore accessTokenStore = new AccessTokenStore(getActivity().getApplication());
                    if (accessTokenStore.getEmail() == null || !Business.isRegistered(getActivity())) {
                        loginAndRegister();
                    } else {
                        Business.save(getActivity(), null);
                        accessTokenStore.clear();
                        updateAccountPrefs();
                    }

                    return true;
                }

            });

            updatePrefSummary(findPreference(AppPrefs.PREF_ACCOUNT_FIELDS));

            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        }

        @Override
        public void onDestroy() {
            // Unregister the listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            super.onDestroy();
        }

        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                updatePrefSummary(findPreference(key));
            }
        };
        private void updatePrefSummary(@Nullable Preference p) {
            if(p==null) return;
            if (p instanceof ListPreference) {
                ListPreference listPref = (ListPreference) p;
                p.setSummary(listPref.getEntry());
            }
        }

        private void updateAccountPrefs() {
            AccessTokenStore accessTokenStore = new AccessTokenStore(getActivity().getApplication());
            PreferenceGroup prefAccountGroup =  (PreferenceGroup) findPreference(PREF_ACCOUNT_GROUP);
            Preference prefStartKiosk = findPreference(PREF_KIOSK_MODE);
            Preference prefAccountDetails = findPreference(PREF_ACCOUNT_DETAILS);
            Preference prefLogin = findPreference(PREF_ACCOUNT_LOGIN);
            if(accessTokenStore.getEmail()==null || !Business.isRegistered(getActivity())) {
                prefStartKiosk.setEnabled(false);
                if(prefAccountDetails!=null) {
                    prefAccountGroup.removePreference(prefAccountDetails);
                }
                prefLogin.setTitle("Login");
            }else {
                if(prefAccountDetails==null) {
                    prefAccountDetails = new Preference(getActivity());
                    prefAccountDetails.setKey(PREF_ACCOUNT_DETAILS);
                    prefAccountDetails.setOrder(1);
                    prefAccountGroup.addPreference(prefAccountDetails);
                }
                prefStartKiosk.setEnabled(true);
                Business business = Business.load(getActivity());
                prefAccountDetails.setTitle(business!=null && business.name!=null ? business.name : "");
                prefAccountDetails.setSummary(accessTokenStore.getEmail());
                prefLogin.setTitle("Log out");
            }

        }

        private void loginAndRegister() {
            final MaterialDialog loginDialog = new MaterialDialog.Builder(getActivity())
                .title("Register Device")
                .customView(R.layout.login_layout, true)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final View view = dialog.getCustomView();
                        assert view != null;
                        final EditText emailEditText = (EditText) view.findViewById(R.id.email);
                        final EditText passwordEditText = (EditText) view.findViewById(R.id.password);

                        final TextView errorTextView = (TextView) view.findViewById(R.id.error);
                        final View progressBar = view.findViewById(R.id.progressBar);

                        String email = emailEditText.getText().toString();
                        String password = passwordEditText.getText().toString();

                        errorTextView.setVisibility(View.GONE);

                        // Basic validation
                        if (TextUtils.isEmpty(email)) {
                            emailEditText.setError("Enter email");
                            //errorTextView.setText("Enter email");
                            //errorTextView.setVisibility(View.VISIBLE);
                            emailEditText.requestFocus();
                            return;
                        }
                        if (TextUtils.isEmpty(password)) {
                            //errorTextView.setText("Enter password");
                            //errorTextView.setVisibility(View.VISIBLE);
                            passwordEditText.setError("Enter password");
                            passwordEditText.requestFocus();
                            return;
                        }
                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
                        emailEditText.setEnabled(false);
                        passwordEditText.setEnabled(false);
                        progressBar.setVisibility(View.VISIBLE);

                        ZippyApi.getInstance().login(email, password, new Callback<User>() {

                            @Override
                            public void success(User user, Response response) {
                                // If user has a business (owner or edit rights) then register device for this business
                                Log.i(user.toString());
                                final ArrayList<Business> businessArrayList = User.getEnabledBusinesses(user);
                                if (businessArrayList.size() > 0) {
                                    dialog.dismiss();
                                    registerDeviceWithBusiness(businessArrayList);
                                } else {
                                    // No businesses
                                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                    dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                    emailEditText.setEnabled(true);
                                    passwordEditText.setEnabled(true);
                                    progressBar.setVisibility(View.GONE);
                                    errorTextView.setVisibility(View.VISIBLE);

                                    errorTextView.setText("No business connected with this user");
                                }

                            }

                            @Override
                            public void failure(RetrofitError error) {
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                emailEditText.setEnabled(true);
                                passwordEditText.setEnabled(true);
                                progressBar.setVisibility(View.GONE);
                                errorTextView.setVisibility(View.VISIBLE);

                                errorTextView.setText(error.getLocalizedMessage());
                                CrashlyticsCore.getInstance().logException(new Exception("Register Device Failure.", error));
                            }
                        });
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();

            View view = loginDialog.getCustomView();
            assert view != null;

            // Prefill login to help with testing
            //((EditText) view.findViewById(R.id.email)).setText("admin@zippy.com.au");
            //((EditText) view.findViewById(R.id.password)).setText("test123");


            TextView serverTextView = (TextView) view.findViewById(R.id.server);
            if(!ZippyApi.ZIPPY_API_ENDPOINT.equalsIgnoreCase(ZippyApi.ZIPPY_PRODUCTION_API_ENDPOINT)) {
                serverTextView.setText(ZippyApi.ZIPPY_API_ENDPOINT);
            }else {
                serverTextView.setVisibility(View.GONE);
            }
            loginDialog.show();
        }

        private void registerDeviceWithBusiness(@NonNull final ArrayList<Business> businessArrayList) {
            if (businessArrayList.size() == 1) {
                new MaterialDialog.Builder(getActivity())
                        .title("Register Device To")
                        .content(businessArrayList.get(0).name)
                        .negativeText(android.R.string.cancel)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                registerDevice(businessArrayList.get(0));
                            }
                        })
                        .show();
            } else if (businessArrayList.size() > 1) {
                // Multiple businesses, so prompt user to select which one
                final String busArray[] = new String[businessArrayList.size()];
                int i = 0;
                for (Business business : businessArrayList) {
                    busArray[i] = "(" + business.id + ") " + business.name;
                    i++;
                }
                new MaterialDialog.Builder(getActivity())
                    .title("Register Device To")
                    .items(busArray)
                    .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            // onSelection gets called only after positive action button is pressed.
                            // Dialog is dismissed automatically
                            registerDevice(businessArrayList.get(which));
                            return true;
                        }
                    })
                    .negativeText(android.R.string.cancel)
                    .positiveText(android.R.string.ok)
                    .show();
            }
        }

        private void registerDevice(@NonNull final Business business) {
            final MaterialDialog progressDialog = new MaterialDialog.Builder(getActivity())
                    .title("Registering device")
                    .content("Please wait")
                    .progress(true, 0)
                    .show();
            final Device device = new Device();
            device.serial = Utils.getAndroidDeviceID(getActivity());
            device.imei = Utils.getTelephonyDeviceID(getActivity());
            device.manufacturer = Build.MANUFACTURER;
            device.model = Build.MODEL;
            device.brand = Build.BRAND;
            device.businessUri = business.uri;

            ZippyApi.getInstance().registerDevice(device, new Callback<Integer>() {

                @Override
                public void success(Integer deviceId, Response response) {
                    AccessTokenStore tokenStore = new AccessTokenStore(getActivity().getApplication());
                    tokenStore.saveDeviceId(deviceId);
                    Business.save(getActivity(), business);
                    updateAccountPrefs();
                    progressDialog.dismiss();
                }

                @Override
                public void failure(RetrofitError error) {
                    // TODO: Display error to the user
                    CrashlyticsCore.getInstance().logException(new Exception("Login Register Device Failure.", error));
                    progressDialog.dismiss();
                }
            });
        }
    }
}