package denis.ventilation;

import android.widget.ToggleButton;

public class Config
{
    Config(String _text, boolean _enabled)
    {
        text = _text;
        enabled = _enabled;
    }

    class FanConfig
    {
        int fan = -1; //номер вентилятора
        int durationOn = 0; //продолжительность включения в минутах
        int durationOff = 0; //продолжительность выключения после включения в минутах
        int delay = 0; //задержка до первого включения в минутах
    };

    Config.FanConfig[] fanConfigs = new Config.FanConfig[4];
    String name; //уникальное имя конфигурации
    boolean enabled;
    int entire; //общее время работы
    String beginTime = ""; //время начала
    String endTime = ""; //время окончания
    float temperature = 0; //подогрев воздуха

    ToggleButton modeButton = null;
    String text; //текст на кнопке
}
