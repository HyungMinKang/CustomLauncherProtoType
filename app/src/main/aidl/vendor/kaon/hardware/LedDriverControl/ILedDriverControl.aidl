package vendor.kaon.hardware.LedDriverControl;


//@VintfStability
interface ILedDriverControl {
    int getAdcValue();
    void setLedColor(int red, int green, int blue);
    void turnOffLed();
    void adjustByAdc(int adcValue);
    void setDriverType(String type);
    void startAdcUpTest();
    void startAdcDownTest();
    void stopAdcTestMode();
    void setLedBreathingConfig(int ledNum, int start, int stop, int jump, int duration);
}
