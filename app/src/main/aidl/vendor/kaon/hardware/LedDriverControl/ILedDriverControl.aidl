package vendor.kaon.hardware.LedDriverControl;


//@VintfStability
interface ILedDriverControl {
    int getAdcValue();
    void setLedColor(int red, int green, int blue);
    void turnOffLed();
    void adjustByAdc(int adcValue);
    String getDriverType();
    void readAdcSub();
    void setDriverType(String type);
    void setLedBreathingConfig(int ledNum, int start, int stop, int jump, int duration);
}
