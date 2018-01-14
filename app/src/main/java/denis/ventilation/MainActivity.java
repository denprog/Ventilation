package denis.ventilation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

public class MainActivity extends Activity implements View.OnClickListener
{
    ToggleButton dayModeButton;
    ToggleButton nightModeButton;

    BluetoothSocket clientSocket;
    InputStream bluetoothStream;

    Thread readingThread;

    boolean connected = false;
    boolean stopping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try
        {
            setContentView(R.layout.activity_main);
        }
        catch (Exception e)
        {
            Log.d("Error", e.getMessage());
        }

        dayModeButton = (ToggleButton)findViewById(R.id.toggleDayMode);
        nightModeButton = (ToggleButton)findViewById(R.id.toggleNightMode);

        dayModeButton.setOnClickListener(this);
        nightModeButton.setOnClickListener(this);

        if (!connect())
        {
            Toast.makeText(getApplicationContext(), "DISCONNECTED", Toast.LENGTH_LONG).show();
            return;
        }

        readingThread = new Thread(readingBluetooth);
        readingThread.start();

        Toast.makeText(getApplicationContext(), "CONNECTED", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View view)
    {
        if (!connected && !connect())
        {
            dayModeButton.setChecked(false);
            nightModeButton.setChecked(false);

            Toast.makeText(getApplicationContext(), "DISCONNECTED", Toast.LENGTH_LONG).show();
            return;
        }

        if (view == dayModeButton)
        {
            try
            {
                OutputStream outStream = clientSocket.getOutputStream();
                String value = "name=Дневной&fan=1,durationOn=2,durationOff=3&fan=2,durationOn=5,durationOff=6\r";
                outStream.write(value.getBytes(Charset.forName("UTF-8")));
            }
            catch (IOException e)
            {
                Log.d("Bluetooth", e.getMessage());
            }
        }
        else if (view == nightModeButton)
        {
        }
    }

    protected boolean connect()
    {
        String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        startActivityForResult(new Intent(enableBT), 0);

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        try
        {
            BluetoothDevice device = bluetooth.getRemoteDevice("98:D3:32:20:CB:E5");
            Method m = device.getClass().getMethod("createRfcommSocket",
                new Class[]
                    {
                        int.class
                    });

            clientSocket = (BluetoothSocket)m.invoke(device, 1);
            clientSocket.connect();

            bluetoothStream = clientSocket.getInputStream();
        }
        catch (NoSuchMethodException e)
        {
            Log.d("Bluetooth", e.getMessage());
            return false;
        }
        catch (IOException e)
        {
            Log.d("Bluetooth", e.getMessage());
            return false;
        }
        catch (IllegalAccessException e)
        {
            Log.d("Bluetooth", e.getMessage());
            return false;
        }
        catch (InvocationTargetException e)
        {
            Log.d("Bluetooth", e.getMessage());
            return false;
        }

        connected = true;

        return true;
    }

    Runnable readingBluetooth = new Runnable()
    {
        final byte delimiter = 10;
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];

        public void run()
        {
            while (!Thread.currentThread().isInterrupted() && !stopping)
            {
                try
                {
                    int bytes = bluetoothStream.available();
                    if (bytes > 0)
                    {
                        byte[] packetBytes = new byte[bytes];
                        bluetoothStream.read(packetBytes);
                        for (int i = 0; i < bytes; ++i)
                        {
                            byte b = packetBytes[i];
                            if (b == delimiter)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "UTF-8");
                                readBufferPosition = 0;
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    stopping = true;
                }
            }
        }
    };
}
