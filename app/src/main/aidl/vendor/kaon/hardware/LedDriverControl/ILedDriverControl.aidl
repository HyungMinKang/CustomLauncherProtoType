package vendor.kaon.hardware.LedDriverControl;


//@VintfStability
interface ILedDriverControl {
    int getAdcValue();
    //void et_setAllOff();
    //void et_setRgb(int baseChannel, int red, int green, int blue);
    void setColor(int red, int green, int blue);
}
