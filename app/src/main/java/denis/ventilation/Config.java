package denis.ventilation;

import android.widget.ToggleButton;
import java.util.Date;

public class Config
{
    Config(String _name, boolean _enabled)
    {
        name = _name;
        enabled = _enabled;
    }

    Config(String _name, int fan1, int durationOn1, int durationOff1, int _entire,
           ToggleButton _modeButton)
    {
        name = _name;
        entire = _entire;
        enabled = true;
        fanConfigs = new Config.FanConfig[1];

        fanConfigs[0] = new Config.FanConfig();
        fanConfigs[0].fan = fan1;
        fanConfigs[0].durationOn = durationOn1;
        fanConfigs[0].durationOff = durationOff1;

        modeButton = _modeButton;
    }

    Config(String _name, int fan1, int durationOn1, int durationOff1, int _entire,
           float _temperature, ToggleButton _modeButton)
    {
        name = _name;
        entire = _entire;
        enabled = true;
        temperature = _temperature;
        fanConfigs = new Config.FanConfig[1];

        fanConfigs[0] = new Config.FanConfig();
        fanConfigs[0].fan = fan1;
        fanConfigs[0].durationOn = durationOn1;
        fanConfigs[0].durationOff = durationOff1;

        modeButton = _modeButton;
    }

    Config(String _name, int fan1, int durationOn1, int durationOff1, String _beginTime,
           String _endTime, float _temperature, ToggleButton _modeButton)
    {
        name = _name;
        beginTime = _beginTime;
        endTime = _endTime;
        enabled = true;
        temperature = _temperature;
        fanConfigs = new Config.FanConfig[1];

        fanConfigs[0] = new Config.FanConfig();
        fanConfigs[0].fan = fan1;
        fanConfigs[0].durationOn = durationOn1;
        fanConfigs[0].durationOff = durationOff1;

        modeButton = _modeButton;
    }

    Config(String _name, int fan1, int durationOn1, int durationOff1, float _temperature,
           int fan2, int durationOn2, int durationOff2, int _entire, ToggleButton _modeButton)
    {
        name = _name;
        entire = _entire;
        enabled = true;
        temperature = _temperature;
        fanConfigs = new Config.FanConfig[2];

        fanConfigs[0] = new Config.FanConfig();
        fanConfigs[0].fan = fan1;
        fanConfigs[0].durationOn = durationOn1;
        fanConfigs[0].durationOff = durationOff1;

        fanConfigs[1] = new Config.FanConfig();
        fanConfigs[1].fan = fan2;
        fanConfigs[1].durationOn = durationOn2;
        fanConfigs[1].durationOff = durationOff2;

        modeButton = _modeButton;
    }

    class FanConfig
    {
        int fan = -1; //номер вентилятора
        int durationOn = 0; //продолжительность включения в минутах
        int durationOff = 0; //продолжительность выключения после включения в минутах
        int delay = 0; //задержка до первого включения в минутах
    };

    Config.FanConfig[] fanConfigs = null;
    String name; //имя конфигурации
    boolean enabled;
    int entire; //общее время работы
    String beginTime = ""; //время начала
    String endTime = ""; //время окончания
    float temperature = 0; //подогрев воздуха

    ToggleButton modeButton = null;
}
