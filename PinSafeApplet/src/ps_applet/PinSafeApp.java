package ps_applet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.SystemException;
import javacard.framework.Util;

public class PinSafeApp extends Applet {

	
	final static byte CLA 					= (byte) 0x80;
	
	/* Service */
	final static byte INS_SERVICE			= (byte) 0x10; // Service commands
	
	final static byte P1_SERVICE_INFO		= (byte) 0x01; // Get Info

	/* Password */
	final static byte INS_PASS				= (byte) 0x20; // Password commands
	
	final static byte P1_PASS_SO			= (byte) 0x00; // SO Password
	final static byte P1_PASS_USER			= (byte) 0x01; // User Password
	
	final static byte P2_PASS_SETUP			= (byte) 0x00; // Password setup
	final static byte P2_PASS_VERIFY		= (byte) 0x01; // Password verify

	/* Storage */
	final static byte INS_STORAGE			= (byte) 0x30; // Storage commnds
	
	final static byte P1_STORAGE_NEW		= (byte) 0x00; // Create New Storage
	
	final static short STORAGE_SZ			= (short) 400; // Storage size
		
	// SW Error
	final static short SW_FILE_MEMORY_ERR		= (short)0x6701;
	
	// Applet Version
	final static byte[] APPLET_VERSION			= { 0x00, 0x01 };
	
	
	private byte[] Storage;
	private byte StorageCapacity;
	
	private PinSafeApp() 
	{
		try 
		{
			Storage = new byte[STORAGE_SZ];
		}
		catch(SystemException e)
		{
			ISOException.throwIt(SW_FILE_MEMORY_ERR);
		}
		Util.arrayFillNonAtomic(Storage, (short)0, STORAGE_SZ, (byte)0xFF);
		StorageCapacity = 20; // Max Card Holder
		register();
	}
	
	public static void install(byte bArray[], short bOffset, byte bLength) 
	{
		new PinSafeApp();
	}

	public void process(APDU apdu) 
	{
		if(selectingApplet())
		{
			return;
		}
		
		byte[] buffer = apdu.getBuffer();
		final short Lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);
		
		if(buffer[ISO7816.OFFSET_CLA] != CLA)
		{
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		switch (buffer[ISO7816.OFFSET_INS])
		{
			case INS_SERVICE:
				Service(apdu);
				break;
				
			case INS_PASS:
				Pass(apdu);
				break;
				
			case INS_STORAGE:
				Storage(apdu);
				break;
				
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				break;
		}
	}

	private void Service(APDU apdu)
	{
		byte[] buffer = apdu.getBuffer();
		switch(buffer[ISO7816.OFFSET_P1])
		{
			case P1_SERVICE_INFO:
				buffer[0] = StorageCapacity; // Place Storage capacity in first byte
				Util.arrayCopyNonAtomic(APPLET_VERSION, (short)0, buffer, (short)1, (short)2);
				apdu.setOutgoingAndSend((short)0, (short)3); // place applet version and send answer back
				break;
			
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
				break;
		}
	}
	
	private void Pass(APDU apdu)
	{
		byte[] buffer = apdu.getBuffer();
		switch(buffer[ISO7816.OFFSET_P1])
		{
			case P1_PASS_SO:
				// SO logic
				break;
			
			case P1_PASS_USER:
				// User Logic
				break;
			
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
				break;
		}
	}
	
	private void Storage(APDU apdu)
	{
		byte[] buffer = apdu.getBuffer();
		switch(buffer[ISO7816.OFFSET_P1])
		{
			case P1_STORAGE_NEW:
				// Create new storage
			break;
			
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
				break;
		}	
	}
	
}

