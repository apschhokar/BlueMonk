package edu.buffalo.rms.bluemountain.localapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.buffalo.rms.bluemountain.databaseshim.BmUtilsSingleton;

/**
 * @author Ramanpreet Singh Khinda
 *         <p>
 *         Created by raman on 11/20/16.
 *         <p>
 *         This is the main class
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = BmUtilsSingleton.GLOBAL_TAG + MainActivity.class.getSimpleName();

    @Bind(R.id.pluggable_fs_radio_btn)
    RadioButton pluggableFsRadioBtn;

    @Bind(R.id.database_sync_radio_btn)
    RadioButton databaseSyncRadioBtn;

    @Bind(R.id.submit_btn)
    Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BmUtilsSingleton.INSTANCE.DEBUG) {
            Log.v(TAG, "onCreate() called");
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        submitBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent myIntent;

        switch (v.getId()) {
            case R.id.submit_btn:
                if (pluggableFsRadioBtn.isChecked()) {
                    myIntent = new Intent(this, PluggableFSActivity.class);
                    startActivity(myIntent);

                } else if (databaseSyncRadioBtn.isChecked()) {
                    myIntent = new Intent(this, DatabaseSyncActivity.class);
                    startActivity(myIntent);
                }

                break;
        }
    }
}
