package denis.ventilation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.ToggleButton;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainTask
{
    public MainActivity mainActivity;

    private BluetoothSocket clientSocket;
    private InputStream inputStream;
    private OutputStream outStream;

    private Thread connectingThread;
    private Thread readingThread;
    private Thread pingThread;

    private long lastPing = System.currentTimeMillis();

    boolean connected = false;
    private boolean stopping = false;

    List<Config> configs = new ArrayList<Config>();

    public void start(MainActivity _mainActivity)
    {
        mainActivity = _mainActivity;

        ToggleButton b;
        b = mainActivity.findViewById(R.id.toggleStudyDayMode);
        configs.add(new Config(b.getTag().toString(), 1, 3, 20, "8:00", "21:00", 20, b));
        b = mainActivity.findViewById(R.id.toggleStudyNightMode);
        configs.add(new Config(b.getTag().toString(), 1, 4, 30, "21:00", "8:00", 20, b));
        b = mainActivity.findViewById(R.id.toggleStudyInflow5Mode);
        configs.add(new Config(b.getTag().toString(), 1, 2, 2, 5, 20, b));
        b = mainActivity.findViewById(R.id.toggleStudyInflow10Mode);
        configs.add(new Config(b.getTag().toString(), 1, 2, 2, 10, 20, b));
        b = mainActivity.findViewById(R.id.toggleStudyInflow20Mode);
        configs.add(new Config(b.getTag().toString(), 1, 2, 2, 20, 20, b));
        b = mainActivity.findViewById(R.id.toggleStudyExhaust5Mode);
        configs.add(new Config(b.getTag().toString(), 2, 5, 0, 5, b));

        b = mainActivity.findViewById(R.id.toggleBedroomDayMode);
        configs.add(new Config(b.getTag().toString(), 3, 2, 2, "8:00", "21:00", 20, b));
        b = mainActivity.findViewById(R.id.toggleBedroomNightMode);
        configs.add(new Config(b.getTag().toString(), 3, 2, 2, "21:00", "8:00", 20, b));
        b = mainActivity.findViewById(R.id.toggleBedroomInflow5Mode);
        configs.add(new Config(b.getTag().toString(), 3, 5, 0, 5, 20, b));
        b = mainActivity.findViewById(R.id.toggleBedroomInflow10Mode);
        configs.add(new Config(b.getTag().toString(), 3, 5, 0, 10, 20, b));
        b = mainActivity.findViewById(R.id.toggleBedroomInflow20Mode);
        configs.add(new Config(b.getTag().toString(), 3, 5, 0, 20, 20, b));
        b = mainActivity.findViewById(R.id.toggleBedroomExhaust5Mode);
        configs.add(new Config(b.getTag().toString(), 4, 5, 0, 5, b));

        for (Config c : configs)
            c.modeButton.setOnClickListener(mainActivity);

        String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        mainActivity.startActivityForResult(new Intent(enableBT), 0);

        connectingThread = new Thread(connectingDevice);
        connectingThread.start();

        readingThread = new Thread(readingBluetooth);
        readingThread.start();

        pingThread = new Thread(ping);
        pingThread.start();
    }

    public void stop()
    {
        stopping = true;
        try
        {
            if (pingThread != null)
                pingThread.join();
            if (readingThread != null)
                readingThread.join();
            if (connectingThread != null)
                connectingThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        stopping = false;

        disconnect();

        if (mainActivity != null)
        {
            mainActivity.ResetStatusGrid();
            mainActivity.UpdateStatusGrid(0, 1, "Нет");
            mainActivity.UpdateStateTab("Состояние (откл)");

            mainActivity = null;
        }

        configs.clear();
    }

    Runnable connectingDevice = new Runnable()
    {
        boolean lastConnected = false;

        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted() && !stopping)
            {
                if (!connected)
                {
                    disconnect();
                    connect();
                }

                if (lastConnected != connected)
                {
                    mainActivity.showToast(connected ? "Соединение установлено" : "Нет соединения");
                    mainActivity.ResetStatusGrid();
                    mainActivity.UpdateStatusGrid(0, 1, connected ? "Есть" : "Нет");
                    mainActivity.UpdateStateTab(connected ? "Состояние (вкл)" : "Состояние (откл)");

                    lastConnected = connected;
                }

                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    Runnable readingBluetooth = new Runnable()
    {
        final byte delimiter = 10;
        int readBufferPosition = 0;
        final int maxBufferLength = 1024;
        byte[] readBuffer = new byte[maxBufferLength];

        @Override
        public void run()
        {
            Log.d("LOG", "readingBluetooth");
            lastPing = System.currentTimeMillis() + 10000;
            while (!Thread.currentThread().isInterrupted() && !stopping)
            {
                //подождать подключение
                if (!connected)
                {
                    try
                    {
                        Thread.sleep(100);
                        lastPing = System.currentTimeMillis();
                        continue;
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                try
                {
                    if (inputStream == null)
                    {
                        Thread.sleep(100);
                        continue;
                    }

                    int bytes = inputStream.available();
                    if (bytes > 0)
                    {
                        byte[] packetBytes = new byte[bytes];
                        inputStream.read(packetBytes);
                        for (int i = 0; i < bytes; ++i)
                        {
                            byte b = packetBytes[i];
                            if (b == delimiter)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "UTF-8");
                                Log.d("LOG", data);

                                if (!parseJson(data))
                                {
                                    readBufferPosition = 0;
                                    disconnect();
                                    continue;
                                }

                                readBufferPosition = 0;
                                lastPing = System.currentTimeMillis();
                            }
                            else
                            {
                                if (readBufferPosition + 1 >= maxBufferLength)
                                {
                                    readBufferPosition = 0;
                                    disconnect();
                                    continue;
                                }
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                    else
                    {
                        Thread.sleep(100);
                    }
                }
                catch (IOException e)
                {
                    //переподключение
                    e.printStackTrace();
                    disconnect();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                if (System.currentTimeMillis() - lastPing > 10000)
                {
                    //переподключение
                    lastPing = System.currentTimeMillis();
                    disconnect();
                }
            }
        }

        private void SetButtonChecked(final ToggleButton button, final boolean checked)
        {
            mainActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    button.setChecked(checked);
                }
            });
        }

        private boolean parseJson(String json)
        {
            try
            {
                JSONObject jsonObject = new JSONObject(json);

                mainActivity.UpdateStatusGrid(0, 1, "Есть");
                String state = (String)jsonObject.get("state");
                mainActivity.UpdateStatusGrid(1, 1, state);

                if (state.equals("OK"))
                    mainActivity.UpdateStateTab("Состояние (вкл)");
                else
                    mainActivity.UpdateStateTab("Состояние (ошибка)");

                List<ToggleButton> checkedButtons = new ArrayList<>();

                String modeNames = new String();
                JSONArray modes = jsonObject.getJSONArray("modes");
                for (int i = 0; i < modes.length(); ++i)
                {
                    JSONObject mode = modes.getJSONObject(i);

                    String name = mode.getString("name");

                    for (Config config : configs)
                    {
                        String tag = config.modeButton.getTag().toString();
                        if (name.equals(tag))
                        {
                            if (tag.startsWith("study"))
                                modeNames += "Кабинет ";
                            else if (tag.startsWith("bedroom"))
                                modeNames += "Спальня ";
                            modeNames += config.modeButton.getTextOff();
                            SetButtonChecked(config.modeButton, true);
                            checkedButtons.add(config.modeButton);
                        }
                    }

                    if (i < modes.length() - 1)
                        modeNames += "\r\n";
                }

                for (Config config : configs)
                {
                    if (checkedButtons.indexOf(config.modeButton) == -1)
                        SetButtonChecked(config.modeButton, false);
                }

                mainActivity.UpdateStatusGrid(2, 1, modeNames);

                for (int i = 1; i <= 4; ++i)
                {
                    int fan = (int)jsonObject.get("fan" + Integer.toString(i));
                    mainActivity.UpdateStatusGrid(i + 2, 1, fan == 1 ? "Включен" : "Выключен");
                }

                for (int i = 1; i <= 2; ++i)
                {
                    if (jsonObject.has("termo" + Integer.toString(i)))
                    {
                        double termo = jsonObject.getDouble("termo" + Integer.toString(i));
                        mainActivity.UpdateStatusGrid(i + 6, 1, String.format("%.1f", termo));
                    }
                    else
                        mainActivity.UpdateStatusGrid(i + 6, 1, "");
                }

                for (int i = 1; i <= 2; ++i)
                {
                    int heater = jsonObject.getInt("heater" + Integer.toString(i));
                    mainActivity.UpdateStatusGrid(i + 8, 1, heater == 1 ? "Включен" : "Выключен");
                }

                int shutter = jsonObject.getInt("shutterIn");
                mainActivity.UpdateStatusGrid(11, 1, shutter == 1 ? "Открыта" : "Закрыта");
                shutter = jsonObject.getInt("shutterOut");
                mainActivity.UpdateStatusGrid(12, 1, shutter == 1 ? "Открыта" : "Закрыта");
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                return false;
            }

            return true;
        }
    };

    Runnable ping = new Runnable()
    {
        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted() && !stopping)
            {
                if (!connected)
                {
                    //подождать подключение
                    try
                    {
                        Thread.sleep(100);
                        continue;
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                try
                {
                    if (outStream == null)
                    {
                        Thread.sleep(100);
                        continue;
                    }

                    String s = "state";
                    s += '\r';

                    Log.d("LOG", "state");
                    outStream.write(s.getBytes());

                    Thread.sleep(2000);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    disconnect();
                    return;
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    disconnect();
                    return;
                }
            }
        }
    };

    private boolean connect()
    {
        Log.d("LOG", "connect");
        if (connected)
            return true;

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        bluetooth.startDiscovery();

        try
        {
            BluetoothDevice device = bluetooth.getRemoteDevice("98:D3:32:20:CB:E5");
            Method m = device.getClass().getMethod("createRfcommSocket",
                new Class[]
                    {
                        int.class
                    });

            clientSocket = (BluetoothSocket)m.invoke(device, 1);
            if (!clientSocket.isConnected())
                clientSocket.connect();
            if (!clientSocket.isConnected())
                return false;

            lastPing = System.currentTimeMillis() + 10000;

            inputStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
        }
        catch (IOException e)
        {
            Log.d("Bluetooth", e.getMessage());
            return false;
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }

        connected = true;
        Log.d("LOG", "connected");

        return true;
    }

    private void disconnect()
    {
        Log.d("LOG", "disconnect");

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        bluetooth.cancelDiscovery();

        try
        {
            if (inputStream != null)
            {
                inputStream.close();
                inputStream = null;
            }
            if (outStream != null)
            {
                outStream.close();
                outStream = null;
            }
            if (clientSocket != null)
            {
                clientSocket.close();
                clientSocket = null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        connected = false;
    }

    public void sendConfig(Config config)
    {
        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("name", config.name);
            jsonObject.put("enabled", config.enabled);
            jsonObject.put("temperature", config.temperature);

            int delay = 0;
            if (!config.beginTime.isEmpty() && !config.endTime.isEmpty())
            {
                DateTimeFormatter f = DateTimeFormat.forPattern("HH:mm");
                LocalTime beginTime = f.parseLocalTime(config.beginTime);
                LocalTime endTime = f.parseLocalTime(config.endTime);

                int beginTimeMinutes = beginTime.getHourOfDay() * 60 + beginTime.getMinuteOfHour();
                int endTimeMinutes = endTime.getHourOfDay() * 60 + endTime.getMinuteOfHour();

                DateTime now = new DateTime();
                int nowMinutes = now.getMinuteOfDay();
                if (beginTimeMinutes <= endTimeMinutes)
                {
                    if (nowMinutes <= beginTimeMinutes)
                    {
                        config.entire = endTimeMinutes - nowMinutes;
                        delay = beginTimeMinutes - nowMinutes;
                    }
                    else if (nowMinutes > beginTimeMinutes && nowMinutes < endTimeMinutes)
                    {
                        config.entire = endTimeMinutes - nowMinutes;
                    }
                    else
                    {
                        config.entire = 24 * 60 - nowMinutes + beginTimeMinutes + endTimeMinutes;
                        delay = 24 * 60 - nowMinutes + beginTimeMinutes;
                    }
                }
                else
                {
                    if (nowMinutes > beginTimeMinutes && nowMinutes <= 24 * 60)
                    {
                        config.entire = 24 * 60 - nowMinutes + endTimeMinutes;
                    }
                    else if (nowMinutes >= 0 && nowMinutes < endTimeMinutes)
                    {
                        config.entire = endTimeMinutes - nowMinutes;
                    }
                    else
                    {
                        config.entire = 24 * 60 - nowMinutes + endTimeMinutes;
                        delay = beginTimeMinutes - nowMinutes;
                    }
                }
//                if (now.minuteOfDay().get() <= beginTimeMinutes)
//                {
//                    delay = beginTimeMinutes - now.minuteOfDay().get();
//                    if (endTimeMinutes >= beginTimeMinutes)
//                        jsonObject.put("entire", endTimeMinutes - beginTimeMinutes + delay);
//                    else
//                        jsonObject.put("entire", 24 * 60 - beginTimeMinutes + endTimeMinutes + delay);
//                }
//                else
//                {
//                    if (endTimeMinutes >= beginTimeMinutes)
//                    {
//                        jsonObject.put("entire", endTimeMinutes - now.minuteOfDay().get());
//                    }
//                    else
//                    {
//                        jsonObject.put("entire", 24 * 60 - beginTimeMinutes + endTimeMinutes -
//                            now.minuteOfDay().get());
//                    }
//                }
            }
            //else
            jsonObject.put("entire", config.entire);

            if (config.fanConfigs != null)
            {
                JSONArray modes = new JSONArray();

                for (int i = 0; i < config.fanConfigs.length; ++i)
                {
                    JSONObject mode = new JSONObject();
                    mode.put("fan", config.fanConfigs[i].fan);
                    mode.put("durationOn", config.fanConfigs[i].durationOn);
                    mode.put("durationOff", config.fanConfigs[i].durationOff);
                    mode.put("delay", delay + config.fanConfigs[i].delay);

                    modes.put(mode);
                }

                jsonObject.put("modes", modes);
            }

            OutputStream outStream = clientSocket.getOutputStream();
            String value = jsonObject.toString();
            value += '\r';
            Log.d("LOG", value);
            outStream.write(value.getBytes(Charset.forName("UTF-8")));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            Log.d("Bluetooth", e.getMessage());
            e.printStackTrace();
        }
    }
}
