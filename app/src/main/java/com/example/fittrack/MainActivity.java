package com.example.fittrack;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.activities.DashboardActivity;
import com.example.fittrack.activities.ProfileActivity;
import com.example.fittrack.database.DBHelper;

/** Launcher: sends new users to the profile setup and returning users to the dashboard. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean hasProfile = new DBHelper(this).getProfile() != null;
        startActivity(new Intent(this,
                hasProfile ? DashboardActivity.class : ProfileActivity.class));
        overridePendingTransition(R.anim.slide_up, R.anim.fade_dim);
        finish();
    }
}