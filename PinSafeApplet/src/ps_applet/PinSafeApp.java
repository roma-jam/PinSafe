package ps_applet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;
import javacard.framework.Util;
import javacard.security.MessageDigest;

public class PinSafeApp extends Applet {

	
	final static byte CLA 						= (byte) 0x80;
	
	/* Service */
	final static byte INS_SERVICE				= (byte) 0x10; // Service commands
	
	final static byte P1_SERVICE_INFO			= (byte) 0x01; // Get Info
	final static byte P1_SERVICE_GET_CHALLENGE	= (byte) 0x02; // Get Challenge

	/* Password */
	final static byte INS_PASS					= (byte) 0x20; // Password commands
	
	final static byte P1_PASS_SO				= (byte) 0x00; // SO Password
	final static byte P1_PASS_USER				= (byte) 0x01; // User Password
	
	final static byte P2_PASS_CHANGE			= (byte) 0x00; // Password setup
	final static byte P2_PASS_VERIFY			= (byte) 0x01; // Password verify

	/* Storage */	
	final static byte INS_STORAGE				= (byte) 0x30; // Storage commands
		
	final static byte P1_STORAGE_NEW			= (byte) 0x00; // Create New Storage
	
	final static short STORAGE_SZ				= (short) 400; // Storage size
	
	// SW Error
	final static short SW_MEMORY_ERR				= (short)0x6701;
	final static short SW_INVALID_PSSWD				= (short)0x6702;
	final static short SW_CHALLENGE_NOT_GENERATED	= (short)0x6703;
	
	// Applet Version
	final static byte[] APPLET_VERSION			= { 0x00, 0x05 };
	
	private byte[] Storage;
	private byte StorageCapacity;
	
	private PinSafeApp(byte[] bArray, short bOffset, byte bLength) 
	{
		// Create storage
		try {
			Storage = new byte[STORAGE_SZ];
		}
		catch(SystemException e) {
			ISOException.throwIt(SW_MEMORY_ERR);
		}
		Util.arrayFillNonAtomic(Storage, (short)0, STORAGE_SZ, (byte)0xFF);
		StorageCapacity = 20; // Max Card Holder
		
		// AID offset
		short AIDoffs = bOffset;
		byte lAID = bArray[AIDoffs++];
				
		// PASS Offset
		bOffset = (short)(AIDoffs + lAID); 		
		bOffset += (short)(bArray[bOffset] + 1);
				
		// PASS default offset
		short DefPsswd_offs = bOffset; 		
		byte lDefPsswd = bArray[DefPsswd_offs++];
		// Create PASS
		new PASS(bArray, DefPsswd_offs, lDefPsswd);
		// Init HMAC
		new HMAC();
		register();
	}
	
	public static void install(byte bArray[], short bOffset, byte bLength) 
	{
		new PinSafeApp(bArray, bOffset, bLength);
	}
	
	public boolean select() 
	{
		PASS.reset_status();
		return true;
	}
	
	public void deselect() 
	{
		PASS.reset_status();
	}

	public void process(APDU apdu) 
	{
		if(selectingApplet())
		{
			return;
		}
		byte[] buffer = apdu.getBuffer();

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
		}
	}

	private void Service(APDU apdu)
	{
		byte[] buffer = apdu.getBuffer();
		final short Lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);
		
		switch(buffer[ISO7816.OFFSET_P1])
		{
			case P1_SERVICE_INFO:
				buffer[0] = StorageCapacity; // Place Storage capacity in first byte
				Util.arrayCopyNonAtomic(APPLET_VERSION, (short)0, buffer, (short)1, (short)2);
				apdu.setOutgoingAndSend((short)0, (short)3); // place applet version and send answer back
				break;
				
			case P1_SERVICE_GET_CHALLENGE:
				if (Lc != 0 && Lc != 0x10) 
				{
					ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
				}
				
				PASS.generate_challenge();
				
				apdu.setOutgoingAndSend((short)0, Util.arrayCopyNonAtomic(PASS.Challenge, (short)0, buffer, (short)0, PASS.CHALLENGE_LEN));
				break;
			
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
	}
	
	private void Pass(APDU apdu)
	{
		byte[] buffer = apdu.getBuffer();
		final short Lc = (short)(buffer[ISO7816.OFFSET_LC] & 0xff);
		short MsgDataLen = (short)(Lc - MessageDigest.LENGTH_SHA);
		
		if(!PASS.use_challange())
		{
			ISOException.throwIt(SW_CHALLENGE_NOT_GENERATED);
		}
		
		
		switch(buffer[ISO7816.OFFSET_P1])
		{
			case P1_PASS_SO:
			{
				// SO logic
			} // P1_PASS_SO
				break;
			
			case P1_PASS_USER:
			{
				switch(buffer[ISO7816.OFFSET_P2])
				{
					case P2_PASS_CHANGE:
					{
						if(Lc != (byte)0x28) //  20 + 20 = 40 (0x28)
						{
							ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
						}
						// Change
						if(!PASS.is_data_verified(buffer, ISO7816.OFFSET_CDATA, MsgDataLen, buffer, (short)(ISO7816.OFFSET_CDATA + MsgDataLen))) 
						{
							ISOException.throwIt(SW_INVALID_PSSWD);
						}
						
						JCSystem.beginTransaction();
						
						PASS.change(buffer, ISO7816.OFFSET_CDATA);
							
						JCSystem.commitTransaction();
					}
						break;
					
					case P2_PASS_VERIFY:
					{
						if(Lc != (byte)0x14) //  20 + 20 = 40 (0x28)
						{
							ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
						}

						// Verify
						if(!PASS.is_verified(buffer, ISO7816.OFFSET_CDATA))  
						{
							ISOException.throwIt(SW_INVALID_PSSWD);
						}
					}
						break;
					default:
							ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
							
				}
				// User Logic
			} // P1_PASS_USER
				break;
			
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
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

