#include <Adafruit_Sensor.h>
#include <ArduinoJson.h>
#include <DHT.h>

int fan1 = 22;
int fan2 = 23;
int fan3 = 24;
int fan4 = 25;
int termo1 = 26;
int termo2 = 27;
int shutterIn = 28;
int shutterOut = 29;
int heater1 = 30;
int heater2 = 31;

DHT sensor1(termo1, DHT22);
DHT sensor2(termo2, DHT22);

struct FanConfig
{
  void reset()
  {
    fan = -1;
    durationOn = 0;
    durationOff = 0;
  }

  int fan = -1; //номер вентилятора 
  long durationOn = 0; //продолжительность включения в минутах
  long durationOff = 0; //продолжительность выключения после включения в минутах
  long delay = 0; //задержка в минутах
  long on = 0; //текущее время включения в миллисекундах
  long off = 0;
};

struct Config
{
  void reset()
  {
    for (int i = 0; i < 4; ++i)
      fanConfigs[i].reset();
  }
  
  FanConfig fanConfigs[4];
  String name;
  boolean enabled = false;
  long entire = 0; //общее время работы миллисекундах
  long cur_time = 0;
  float temperature = 0;
};

void setup() 
{                
  pinMode(fan1, OUTPUT);
  pinMode(fan2, OUTPUT);
  pinMode(fan3, OUTPUT);
  pinMode(fan4, OUTPUT);
  pinMode(termo1, OUTPUT);
  pinMode(termo2, OUTPUT);
  pinMode(heater1, OUTPUT);
  pinMode(heater2, OUTPUT);
  pinMode(shutterIn, OUTPUT);
  pinMode(shutterOut, OUTPUT);

  Serial.begin(9600);

  sensor1.begin();
  sensor2.begin();
  
  turnFan(1, false);
  turnFan(2, false);
  turnFan(3, false);
  turnFan(4, false);
}

StaticJsonBuffer<2000> jsonBuffer;

#define CONFIGS_COUNT 5

Config configs[CONFIGS_COUNT];

void sendState(boolean ok, String state = "")
{
  jsonBuffer.clear();
  JsonObject& root = jsonBuffer.createObject();
  
  if (!ok)
  {
    root["state"] = state;
    return;
  }
  
  root["state"] = "OK";

  JsonArray& modes = jsonBuffer.createArray();      
  
  for (int i = 0; i < CONFIGS_COUNT; ++i)
  {
    Config& c = configs[i];
    if (!c.enabled)
      continue;
    JsonObject& mode = jsonBuffer.createObject();
    mode["name"] = c.name;
    modes.add(mode);
  }
  
  root["modes"] = modes;
  
  root["fan1"] = digitalRead(fan1);
  root["fan2"] = digitalRead(fan2);
  root["fan3"] = digitalRead(fan3);
  root["fan4"] = digitalRead(fan4);

  double t = sensor1.readTemperature();
  if (!isnan(t))
    root["termo1"] = t;
  t = sensor2.readTemperature();
  if (!isnan(t))
    root["termo2"] = t;

  root["heater1"] = digitalRead(heater1);
  root["heater2"] = digitalRead(heater2);
  
  root["shutterIn"] = digitalRead(shutterIn);
  root["shutterOut"] = digitalRead(shutterOut);
  
  String s;
  root.printTo(s);
  Serial.println(s);
}

boolean parseConfig(String json)
{
  jsonBuffer.clear();
  Config new_config;
  
  //распарсить конфигурацию
  JsonObject& root = jsonBuffer.parseObject(json);
  if (!root.success())
  {
    sendState(false, "Error parsing config string");
    return false;
  }
  
  new_config.name = (const char*)root["name"];
  new_config.enabled = (boolean)root["enabled"];
  new_config.entire = (long)root["entire"] * 60 * 1000;
  new_config.temperature = (float)root["temperature"];
  new_config.cur_time = millis();
  
  JsonArray& modes = root["modes"].asArray();

  for (int i = 0; i < modes.size(); ++i)
  {
    JsonObject& mode = modes[i];
    
    FanConfig c;
    c.fan = (int)mode["fan"];
    c.durationOn = (long)mode["durationOn"];
    c.on = c.durationOn * 60 * 1000;
    c.durationOff = (long)mode["durationOff"];
    c.off = 0;
    c.delay = (long)mode["delay"] * 60 * 1000;
    new_config.fanConfigs[i] = c;
  }
  
  //поиск конфига с таким именем
  for (int i = 0; i < CONFIGS_COUNT; ++i)
  {
    Config& c = configs[i];
    if (c.enabled && c.name == new_config.name)
    {
      if (!new_config.enabled)
      {
        //выключить вентиляторы
        for (int i = 0; i < 4; ++i)
        {
          if (c.fanConfigs[i].fan > 0)
            turnFan(c.fanConfigs[i].fan, false);
        }
      }

      //заменить конфиг
      c = new_config;
      
      return true;
    }
  }

  if (!new_config.enabled)
    return true;
    
  //заменить неиспользуемый
  for (int i = 0; i < CONFIGS_COUNT; ++i)
  {
    Config& c = configs[i];
    if (!c.enabled)
    {
      c = new_config;
      return true;
    }
  }

  sendState(false, "Too many configurations");
  
  return false;
}

boolean turnedFan(int fan)
{
  return digitalRead(21 + fan);
}

int fans[4] = {0, 0, 0, 0}; //количество ссылок на включение вентиляторов

void turnFan(int fan, boolean on)
{
  if (on)
  {
    if (fans[fan - 1] == 0)
    {
      //открыть заслонку
      if (fan == 1 || fan == 3)
        digitalWrite(shutterIn, HIGH);
      else
        digitalWrite(shutterOut, HIGH);
      digitalWrite(21 + fan, HIGH); //включить вентилятор
    }
    ++fans[fan - 1];
  }
  else
  {
    if (fans[fan - 1] > 0)
      --fans[fan - 1];
    if (fans[fan - 1] == 0)
    {
      //выключить нагреватель
      if (fan == 1)
        turnHeater(1, false);
      else if (fan == 3)
        turnHeater(2, false);
        
      digitalWrite(21 + fan, LOW); //выключить вентилятор
      
      //закрыть заслонку если другие ее не используют
      switch (fan - 1)
      {
        case 0:
          if (fans[2] == 0)
            digitalWrite(shutterIn, LOW);
          break;
        case 1:
          if (fans[3] == 0)
            digitalWrite(shutterOut, LOW);
          break;
        case 2:
          if (fans[0] == 0)
            digitalWrite(shutterIn, LOW);
          break;
        case 3:
          if (fans[1] == 0)
            digitalWrite(shutterOut, LOW);
          break;
      }
    }
  }
}

void turnHeater(int heater, boolean on)
{
  if (on)
  {
    if (!digitalRead(heater1 + heater - 1))
      digitalWrite(heater1 + heater - 1, HIGH);
  }
  else
  {
    if (digitalRead(heater1 + heater - 1))
      digitalWrite(heater1 + heater - 1, LOW);
  }
}

void keepTemperature(int heater, float temperature)
{
  if (temperature == 0)
    return;

  float t = (heater == 1 ? sensor1.readTemperature() : sensor2.readTemperature());
  if (isnan(t))
    return;
  if (t >= temperature)
    turnHeater(heater, false);
  else
    turnHeater(heater, true);
}

void updateVentilation()
{
  for (int i = 0; i < CONFIGS_COUNT; ++i)
  {
    Config& conf = configs[i];
    if (!conf.enabled)
      continue;
      
    //вычислить время, прошедшее с последней проверки
    int delta_time = 0;
    int cur_time = millis();
    if (conf.cur_time < cur_time)
      delta_time = cur_time - conf.cur_time;
    else
      delta_time = 0xffffffff - conf.cur_time + conf.cur_time; //переход через max int
    conf.cur_time = cur_time;
      
    if (conf.entire > 0)
      conf.entire -= delta_time;
    if (conf.entire <= 0)
    {
      conf.entire = 0;
      conf.enabled = false;
      
      //выключить вентиляторы
      for (int i = 0; i < 4; ++i)
      {
        if (conf.fanConfigs[i].fan > 0)
          turnFan(conf.fanConfigs[i].fan, false);
      }
      return;
    }

    for (int i = 0; i < 4; ++i)
    {
      FanConfig& c = conf.fanConfigs[i];
      
      if (c.delay > 0)
      {
        c.delay -= delta_time; //уменьшить задержку
        if (c.delay < 0)
          c.delay = 0;
        if (c.delay > 0)
          continue; //продолжение задержки
      }
  
      if (c.on > 0)
      {
        if (!turnedFan(c.fan))
          turnFan(c.fan, true);
          
        c.on -= delta_time;
        if (c.on > 0)
        {
          keepTemperature(c.fan == 1 ? 1 : 2, conf.temperature);
          continue; //продолжение работы вентилятора
        }
          
        turnFan(c.fan, false); //выключить вентилятор
        
        c.on = 0;
        c.off = c.durationOff * 60 * 1000;
        continue;
      }
      
      if (c.off > 0)
      {
        c.off -= delta_time;
        if (c.off > 0)
          continue; //продолжение останова вентилятора
        
        turnFan(c.fan, true); //включить вентилятор
        
        c.off = 0;
        c.on = c.durationOn * 60 * 1000;
      }
    }
  }
}

long update = 0;
String serialString;

void loop() 
{
  if (Serial.available())
  {
    char c = Serial.read();
    if (c == '\r')
    {
      if (serialString == "state")
      {
        sendState(true);
      }
      else if (!parseConfig(serialString))
      {
        serialString = "";
        return;
      }
      
      serialString = "";
    }
    else
    {
      serialString += c;
    }
  }

  if (millis() - update > 1000) 
  {
    updateVentilation();
    update = millis();
  }
}

