package kristof.fr.droshed.activity.HomeActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import kristof.fr.droshed.CollectionUtils;
import kristof.fr.droshed.Explorer.FileItemExplorer;
import kristof.fr.droshed.Explorer.FolderItemExplorer;
import kristof.fr.droshed.Explorer.ItemExplorer;
import kristof.fr.droshed.JsonUtil;
import kristof.fr.droshed.R;
import kristof.fr.droshed.ServerInfo;
import kristof.fr.droshed.Util;
import kristof.fr.droshed.activity.CreateModelActivity.ListModelActivity;
import kristof.fr.droshed.activity.ModelActivity.ModelActivity;
import kristof.fr.droshed.custom.FontCache;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, CustomFragment.FolderManager {

    public static final int PICK_FILE = 100;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private ServerInfo serverInfo;
    private ProgressBar progressBar;
    private FrameLayout frameLayout;
    private HashMap<FolderItemExplorer, CustomFragment> hashMap;
    private FolderItemExplorer currentFolderItemExplorer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        setToolbar();
        initUiElements();
        hashMap = new HashMap<>();
        //manage rotation
        if (savedInstanceState != null) {
            manageRotation(savedInstanceState);
        } else {
            Intent intent = getIntent();
            if (intent != null && serverInfo == null) {
                Bundle bundle = intent.getExtras();
                serverInfo = bundle.containsKey("serverInfo") ? bundle.getParcelable("serverInfo") : null;
                refreshListFolder();
            }
        }
    }

    private class GenericCls<T> {
        private Class<T> type;

        GenericCls(Class<T> cls) {
            type = cls;
        }

        Class<T> getType() {
            return type;
        }
    }

    private void manageRotation(Bundle savedInstanceState) {
        serverInfo = savedInstanceState.containsKey("serverInfo") ? savedInstanceState.getParcelable("serverInfo") : null;
        currentFolderItemExplorer = savedInstanceState.containsKey("currentFolderItemExplorer") ? savedInstanceState.getParcelable("currentFolderItemExplorer") : null;
        HashMap<String, FolderItemExplorer> hashMapBundle;
        GenericCls<FolderItemExplorer> clas = new GenericCls<>(FolderItemExplorer.class);
        hashMapBundle = new HashMap<>(CollectionUtils.fromBundle(savedInstanceState.getBundle("hashMap"), clas.getType()));
        for (String key : hashMapBundle.keySet()) {
            FolderItemExplorer folderItemExplorer = hashMapBundle.get(key);
            CustomFragment customFragment = CustomFragment.createNewFragment(folderItemExplorer);
            hashMap.put(folderItemExplorer, customFragment);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("serverInfo", serverInfo);
        outState.putParcelable("currentFolderItemExplorer", currentFolderItemExplorer);
        HashMap<String, FolderItemExplorer> hashMapBundle = new HashMap<>();
        for (FolderItemExplorer folderItemExplorer : hashMap.keySet()) {
            hashMapBundle.put(folderItemExplorer.getName(), folderItemExplorer);
        }
        outState.putBundle("hashMap", CollectionUtils.toBundle(hashMapBundle));
        super.onSaveInstanceState(outState);
    }

    private void initUiElements() {
        drawer = (DrawerLayout) findViewById(R.id.homeDrawerLayout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        frameLayout = (FrameLayout) findViewById(R.id.flContent);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();
        drawer.addDrawerListener(toggle);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(__ -> launchCreateSpreadSheetActivity());
        NavigationView navigationView = (NavigationView) findViewById(R.id.nvView);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void refreshListFolder() {
        URL url;
        try {
            url = new URL(serverInfo + "/"+ serverInfo.getDataPath());
            new CustomAsyncTask().execute(url);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(drawer, getString(R.string.error_connexion), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    private void setToolbar() {
        toolbar = (Toolbar) findViewById(R.id.homeToolbar);
        toolbar.setTitle(getString(R.string.title_home_activity));
        setToolbarFont(toolbar);
        setSupportActionBar(toolbar);
    }

    private void setToolbarFont(Toolbar toolbar) {
        Typeface font = FontCache.getFont(this, getString(R.string.font));
        for (int i = 0; i <= toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                textView.setTypeface(font);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //int id = item.getItemId();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.homeDrawerLayout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public CustomFragment getFragment(FolderItemExplorer fileItemExplorer) {
        return hashMap.get(fileItemExplorer);
    }

    @Override
    public void manageItem(FileItemExplorer fileItemExplorer) {
        startModelActivity(serverInfo, fileItemExplorer);
    }

    private void launchCreateSpreadSheetActivity() {
        Intent mainIntent = new Intent(HomeActivity.this, ListModelActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("serverInfo", serverInfo);
        bundle.putParcelable("currentFolderItemExplorer", currentFolderItemExplorer);
        mainIntent.putExtras(bundle);
        startActivityForResult(mainIntent, PICK_FILE);
    }

    private void startModelActivity(ServerInfo serverInfo, FileItemExplorer fileItemExplorer) {
        Intent mainIntent = new Intent(HomeActivity.this, ModelActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("serverInfo", serverInfo);
        bundle.putBoolean("isNewFile", false);
        bundle.putParcelable("fileItemExplorer", fileItemExplorer);
        bundle.putParcelable("currentFolderItemExplorer", currentFolderItemExplorer);
        mainIntent.putExtras(bundle);
        startActivityForResult(mainIntent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to

        if (requestCode == PICK_FILE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                FileItemExplorer fileItemExplorer = bundle.getParcelable("fileItemExplorer");
                currentFolderItemExplorer = bundle.getParcelable("currentFolderItemExplorer");
                CustomFragment customFragment = hashMap.get(currentFolderItemExplorer);
                if (customFragment != null) {
                    ArrayList<ItemExplorer> itemExplorers = customFragment.getItemExplorerList();
                    if (!itemExplorers.contains(fileItemExplorer)) {
                        itemExplorers.add(fileItemExplorer);
                        customFragment.updateGridViewList(itemExplorers);
                    }
                }
            }else{
                Snackbar.make(drawer, getString(R.string.error_connexion), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private class CustomAsyncTask extends AsyncTask<URL, Void, ItemExplorer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            displayProgressBar(true);
        }

        @Override
        protected ItemExplorer doInBackground(URL... params) {
            for (URL url : params) {
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(3000); //set timeout to 3 seconds
                    urlConnection.setReadTimeout(3000);
                    urlConnection.setRequestProperty("Authorization", serverInfo.getAuthBase64());
                    urlConnection.setDoInput(true);
                    // Starts the query
                    urlConnection.connect();
                    if (urlConnection.getResponseCode() == 200) {
                        return JsonUtil.toCustomItem(
                                Util.getStringFromInputStream(urlConnection.getInputStream())
                        );
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ItemExplorer s) {
            super.onPostExecute(s);
            if (s == null) {
                Snackbar.make(drawer, getString(R.string.error_connexion), Snackbar.LENGTH_SHORT).show();
            } else {
                if (s instanceof FolderItemExplorer) {
                    FolderItemExplorer folderItemExplorer = (FolderItemExplorer) s;
                    CustomFragment customFragment = CustomFragment.createNewFragment(folderItemExplorer);
                    addFragmentToStack(folderItemExplorer, customFragment);
                }
            }
            displayProgressBar(false);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            displayProgressBar(false);
        }
    }

    private void displayProgressBar(boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        frameLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        frameLayout.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                frameLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void addFragmentToHashMap(FolderItemExplorer folderItemExplorer, CustomFragment customFragment) {
        hashMap.put(folderItemExplorer, customFragment);
        currentFolderItemExplorer = folderItemExplorer;
        addFragmentToBackStack(folderItemExplorer, customFragment);
    }

    private void addFragmentToStack(FolderItemExplorer folderItemExplorer, CustomFragment customFragment) {
        hashMap.put(folderItemExplorer, customFragment);
        currentFolderItemExplorer = folderItemExplorer;
        getFragmentManager().beginTransaction().replace(R.id.flContent, customFragment, folderItemExplorer.getPath())
                .commit();
    }

    private void addFragmentToBackStack(FolderItemExplorer folderItemExplorer, CustomFragment customFragment) {
        if (hashMap.containsKey(folderItemExplorer)) {
            getFragmentManager().beginTransaction().replace(R.id.flContent, customFragment, folderItemExplorer.getPath())
                    .addToBackStack(folderItemExplorer.getPath())
                    .commit();
        } else {
            getFragmentManager().beginTransaction().add(R.id.flContent, customFragment, folderItemExplorer.getPath())
                    .addToBackStack(folderItemExplorer.getPath())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }
}
