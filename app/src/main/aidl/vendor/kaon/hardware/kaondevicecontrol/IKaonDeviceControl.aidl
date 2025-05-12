///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.kaon.hardware.kaondevicecontrol;
//@VintfStability
interface IKaonDeviceControl {
  void ledOff(vendor.kaon.hardware.kaondevicecontrol.LedColor in_color);
  void ledOn(vendor.kaon.hardware.kaondevicecontrol.LedColor in_color);
  void startBreathingEffect(vendor.kaon.hardware.kaondevicecontrol.LedColor color, int ledPower, float interval);
  void stopBreathingEffect();
  void switchLedColorsOnDeviceState(in vendor.kaon.hardware.kaondevicecontrol.DeviceOnOff state, vendor.kaon.hardware.kaondevicecontrol.IKaonCallbackPower callback);
  void handleOnRcuPress();
  void handleManualPairing();
  void handlePulseLedFrom15To100(in vendor.kaon.hardware.kaondevicecontrol.LedColor color);
  void setAndroidBootCompleted();
  void endPairingRCU();
  void startRedLedBlinkError();
  void stopRedLedBlinkError();
  void stopRedLedBlinkHdmiOff();
  void setConnectStatus(int status, boolean value);
  void setBrightness(vendor.kaon.hardware.kaondevicecontrol.LedColor color, int brightness);
}
