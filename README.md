# JDC Windoo Manager

JDCWindooManager is an API that can be used with the [Skywatch Windoo](http://www.windoo.ch) dongle.

The API is currently available for Android and [iOS](https://gist.github.com/jdc-electronic/4b50e3750f7ad96a88d7) (full compatibility list is available on the Skywatch Windoo [website](http://www.windoo.ch/specifications)).

## Releases
### Version 0.1 - 15th December 2014
- Extracting API from Windoo project

## Demo Project
With the library you also received one project that provide an example how to use the API.

## How to get the API
In Android Studio: Move the received aar file into the __app/aars__ folder and add the following lines to your `build.gradle` files.

In `/build.gradle`
```
allprojects {
    repositories {
        ...
        flatDir {
            dirs 'aars'
        }
    }
}
```

In `/app/build.gradle`
```gradle
dependencies {
    ...
    compile 'ch.skywatch.windoo.api:windoo-api-x.x@aar'
    ...
}
```

## How to use the API
The API is built on a singleton pattern. First you need to get the instance `JDCWindooManager jdcWindooManager = JDCWindooManager.getInstance()`. Then it is possible to call `jdcWindooManager.enable(this)` to start listening to the dongle. You can stop the process anytime by calling `jdcWindooManager.disable(this)`

If the user decrease the volume or unplugged the Windoo, the API will raise events (described below) but will also continue listening to the jack port if it can restart the process.

### Available methods on JDCWindooManager
Set your personal token:
```java 
public void setToken(String token);
```
Start the process:
```java 
public void enable(Activity activity);
```
Stop the process:
```java 
public void disable(Activity activity);
```
Get the live measurements:
```java 
public JDCWindooMeasurement getLive();
```
Publish the measurement on the Skywatch Windoo platform:
```java 
public void publishMeasure(JDCWindooMeasurement measure);
```

### Listen to Events triggered from JDCWindooManager
You might need to do specific actions when events are triggered. You can implement Observer in order to listen to the JDCWindoo Manager events:
```java
public class WindooApiDemo extends Activity implements Observer {
    ...
    @Override
    public void onResume(){
        super.onResume();
        jdcWindooManager.addObserver(this);
    }
    @Override
    public void onPause(){
        super.onPause();
        jdcWindooManager.deleteObserver(this);
    }
}
```

Then, you have to implement the `update` method to catch the events
```java
public void update(Observable observable, final Object object) {
    runOnUiThread(new Runnable() {
        public void run() {
            JDCWindooEvent e = (JDCWindooEvent) object;
            if (e.getType() == JDCWindooEvent.JDCWindooAvailable) {
                Log.d(TAG, "Windoo available");
            } else if (e.getType() == JDCWindooEvent.JDCWindooNewWindValue) {
                Log.d(TAG, "Wind received : " + e.getData());
            }
            ...
        }
    });
}
```

### Events triggered
__Between `enable()` and `disable()`__
- `JDCWindooNotAvailable` The Windoo is unplugged
- `JDCWindooAvailable` The Windoo is plugged
- `JDCWindooCalibrated` The Windoo finishes its calibration process and starts emitting data
- `JDCWindooVolumeNotAtItsMaximum` The volume is too low
- `JDCWindooNewWindValue` A new wind value is available. This event also contains a `Double` with the wind speed in __km/h__
- `JDCWindooNewTemperatureValue` A new temperature value is available. This event also contains a `Double` with the temperature in __Ã‚Â°C__
- `JDCWindooNewHumidityValue` A new new humidity value is available. This event also contains a `Double` with the humidity in __%RH__
- `JDCWindooNewPressureValue` A new pressure value is available. This event also contains a `Double` with the pressure __HPa__

__After calling `publishMeasure()`__
- `JDCWindooPublishSuccess` The measurement was successfully published. This event also contains a `JSONObject` with the published measure
- `JDCWindooPublishException` A problem occurs while publishing the measure. This event also contains a `String` with the exception description. This can be a `TOKEN_ERROR`, `MISSING_DATA`, `TIMEOUT_ERROR`, `SERVER_ERROR` or an `UNKNOWN_ERROR`

#### This is an example of the sequence of events that occured with a Windoo 3 (already plugged)
1. `JDCWindooAvailable`
2. `JDCWindooCalibrated`
3. __`JDCWindooNewWindValue`__
4. `JDCWindooNewTemperatureValue`
5. __`JDCWindooNewWindValue`__
6. `JDCWindooNewHumidityValue`
7. __`JDCWindooNewWindValue`__
8. `JDCWindooNewPressureValue`
9. __`JDCWindooNewWindValue`__
10. `JDCWindooNewTemperatureValue`
11. ...

The wind is sent more often than the other data events cause he is supposed to change more quickly.

## Classes and Methods
`JDCWindooManager`
```java
class JDCWindooManager {
    public static JDCWindooManager getInstance();
    public void setToken(String token);
    public void enable(Activity activity);
    public void disable(Activity activity);
    public JDCWindooMeasurement getLive();
    public void publishMeasure(JDCWindooMeasurement measure);
}
```

`JDCWindooEvent`
```java
class JDCWindooEvent {
    public static final int JDCWindooNotAvailable = 0;
    public static final int JDCWindooAvailable = 1;
    public static final int JDCWindooCalibrated = 2;
    public static final int JDCWindooVolumeNotAtItsMaximum = 3;
    public static final int JDCWindooNewWindValue = 4;
    public static final int JDCWindooNewTemperatureValue = 5;
    public static final int JDCWindooNewHumidityValue = 6;
    public static final int JDCWindooNewPressureValue = 7;
    public static final int JDCWindooPublishSuccess = 8;
    public static final int JDCWindooPublishException = 9;

    public int getType();
    public Object getData();
}
```

`JDCWindooMeasurement`
```java
class JDCWindooMeasurement {

    public Double getWind();
    public void setWind(Double wind);

    public Double getTemperature();
    public void setTemperature(Double temperature);

    public Double getHumidity();
    public void setHumidity(Double humidity);

    public Double getPressure();
    public void setPressure(Double pressure);

    public Date getCreatedAt();
    public void setCreatedAt(Date createdAt);

    public Date getUpdatedAt();
    public void setUpdatedAt(Date updatedAt);

    public Double getLatitude();
    public void setLatitude(Double latitude);

    public Double getLongitude();
    public void setLongitude(Double longitude);

    public Float getOrientation();
    public void setOrientation(Float orientation);

    public Float getAccuracy();
    public void setAccuracy(Float accuracy);

    public Double getAltitude();
    public void setAltitude(Double altitude);

    public Float getSpeed();
    public void setSpeed(Float speed);

    public String getNickname(),
    public void setNickname(String nickname);

    public String getEmail();
    public void setEmail(String email);

    public File getPicture();
    public void setPicture(File picture);

    public String getPictureGuid();
    public void setPictureGuid(String pictureGuid);
}

```

## Support
- Samsung Galaxy S3, S4, S4 mini, S5, S5 mini
- Samsung Galaxy K Zoom
- Nexus 4
- Nexus 5 (Only available on Windoo 1)
- HTC One S, One (M8), One mini 2
- Sony Xperia Z3 compact
- LG G3
- Samsung Galaxy Tab 10.1
- Samsung Galaxy Tab2 10.1
- Samsung Galaxy TabPRO 10.1, TabPRO 12.2
- Samsung Galaxy Tab S 10.5 (SM-T805)
- Samsung Galaxy Note 2
- Huawei MediaPad M1 8.0

__Android Ice Cream Sandwich (4.0) or newer__ is required

## Contact

- http://www.windoo.ch
- apps [at] jdc [dot] ch

## Web services
We also offer web services that can be used with your registered token. More informations [here](https://gist.github.com/jdc-electronic/4eb3243ba242005d9448)

## License

The licensing informations are described in `LICENSE.md`