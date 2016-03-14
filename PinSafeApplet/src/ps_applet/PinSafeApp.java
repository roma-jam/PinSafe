package ps_applet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;

public class PinSafeApp extends Applet {

	private PinSafeApp() {
	}

	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new PinSafeApp().register();
	}

	public void process(APDU arg0) throws ISOException {

	}

}
