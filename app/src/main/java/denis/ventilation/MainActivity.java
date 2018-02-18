package denis.ventilation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements View.OnClickListener
{
    static MainTask mainTask;

    GridView statusGrid;
    StatusGridAdapter statusGridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("LOG", "onCreate");
        super.onCreate(savedInstanceState);

        try
        {
            setContentView(R.layout.activity_main);
        }
        catch (Exception e)
        {
            Log.d("Error", e.getMessage());
        }

        TabHost mainTabHost = findViewById(R.id.tabHost);
        mainTabHost.setup();

        TabHost.TabSpec mSpec = mainTabHost.newTabSpec("");
        mSpec.setContent(R.id.tab1);
        mSpec.setIndicator("Кабинет");
        mainTabHost.addTab(mSpec);

        mSpec = mainTabHost.newTabSpec("");
        mSpec.setContent(R.id.tab2);
        mSpec.setIndicator("Спальня");
        mainTabHost.addTab(mSpec);

        mSpec = mainTabHost.newTabSpec("");
        mSpec.setContent(R.id.tab3);
        mSpec.setIndicator("Состояние (выкл)");
        mainTabHost.addTab(mSpec);

        statusGrid = findViewById(R.id.statusGrid);
        statusGrid.setAdapter(new StatusGridAdapter(this));
        statusGridAdapter = (StatusGridAdapter)statusGrid.getAdapter();

        if (mainTask != null)
            mainTask.stop();
        if (mainTask == null)
            mainTask = new MainTask();
        mainTask.start(this);
    }

    @Override
    public Object onRetainNonConfigurationInstance()
    {
        mainTask.stop();
        return mainTask;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mainTask.stop();
    }

    @Override
    public void onStart()
    {
        Log.d("LOG", "onStart");
        super.onStart();
        if (mainTask.mainActivity == null)
            mainTask.start(this);
    }

    @Override
    public void onStop()
    {
        Log.d("LOG", "onStop");
        super.onStop();
        mainTask.stop();
    }

    @Override
    public void onClick(View view)
    {
        ToggleButton b = (ToggleButton)view;

        if (mainTask.connected)
        {
            if (!b.isChecked())
            {
                Config config = new Config(view.getTag().toString(), false);
                mainTask.sendConfig(config);
            }
            else
            {
                for (Config config : mainTask.configs)
                {
                    if (config.modeButton == b)
                    {
                        mainTask.sendConfig(config);
                        break;
                    }
                }
            }
        }

        b.setChecked(!b.isChecked());
    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void UpdateStatusGrid(final int i, final int j, final String text)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                statusGridAdapter.setItem(i, j, text);
            }
        });
    }

    public void UpdateStatusGridFan(final int i, final String text)
    {
        if (i < 2)
            UpdateStatusGrid(i + 6, 1, text);
        else
            UpdateStatusGrid(i + 9, 1, text);
    }

    public void UpdateStatusGridTemperature(final int i, final String text)
    {
        if (i == 0)
            UpdateStatusGrid(i + 8, 1, text);
        else
            UpdateStatusGrid(i + 12, 1, text);
    }

    public void UpdateStatusGridHeater(final int i, final String text)
    {
        if (i == 0)
            UpdateStatusGrid(i + 9, 1, text);
        else
            UpdateStatusGrid(i + 13, 1, text);
    }

    public void UpdateStatusGridShutter(final int i, final String text)
    {
        if (i == 0)
            UpdateStatusGrid(3, 1, text);
        else
            UpdateStatusGrid(4, 1, text);
    }

    public void ResetStatusGrid()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                statusGridAdapter.reset();
            }
        });
    }

    public void UpdateStateTab(final String text)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TabHost mainTabHost = findViewById(R.id.tabHost);
                TextView tv = mainTabHost.getTabWidget().getChildAt(2).findViewById(android.R.id.title);
                tv.setText(text);
            }
        });
    }
}
