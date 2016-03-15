package ps_applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.MessageDigest;
import javacard.security.RandomData;

public class PASS {
	public static byte[] 			PBKDF2_PASS;			// Buffer for Passwd keep
	public static boolean 			default_pin;
	public static boolean 			isChallengeValid;
	public static boolean 			isPasswordVerified;
	public static byte[] 			Challenge;
	private static RandomData 		rng;
	
	
	public static final byte  		dkLen 						= (byte) 20;
	
	private static byte[] 			bHMAC;
	private static byte[] 			RamBuffer;
	private  static final short  	RAM_BUF_SZ				= (short) (256 + 16);
	
	public static final byte  		CHALLENGE_LEN	= (byte) 16;
	
	// Defines
	public static final byte[] PBKDF2_defaultPIN = {
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
	};
	
	public PASS(byte[] pBuffer, short PinOffset, byte PinLength) 
	{
		rng = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		
		bHMAC = JCSystem.makeTransientByteArray(MessageDigest.LENGTH_SHA, JCSystem.CLEAR_ON_DESELECT);
		RamBuffer = JCSystem.makeTransientByteArray(RAM_BUF_SZ, JCSystem.CLEAR_ON_DESELECT);
		
		Challenge = new byte[CHALLENGE_LEN]; // Init the Challenge Buffer in EEPROM
		
		isChallengeValid = false;
		isPasswordVerified = false;
		default_pin = true;
		
		PBKDF2_PASS = new byte[dkLen];
		
		if(PinLength == dkLen) 
		{
			set_default(pBuffer, PinOffset, PinLength);
		}
		
		set(PBKDF2_defaultPIN, (short)0, dkLen);
	}
	
	private static void set_default(byte[] pPin, short Offset, byte PinLength) 
	{
		Util.arrayCopy(pPin, Offset, PBKDF2_defaultPIN, (short)0, PinLength);
	}
	
	private static void set(byte[] pPin, short Offset, byte PinLength) 
	{
		Util.arrayCopy(pPin, Offset, PBKDF2_PASS, (short)0, PinLength);
	}
	
	private static void getHardwareRandom(byte[] buffer, short offset, short len)
	{
		rng.generateData(buffer, offset, len);
	}
	
	public static void generate_challenge() 
	{
		PASS.getHardwareRandom(Challenge, (short)0, CHALLENGE_LEN);
		isChallengeValid = true;
	}
	
	public static void reset_status()
	{
		PASS.isPasswordVerified = false;
		PASS.isChallengeValid = false;
	}
	
	public static boolean use_challange() 
	{
		if(isChallengeValid) {
			isChallengeValid = false;
			return true;
		}
		return false;
	}
	
	public static void change(byte[] NewXorBuffer, short Offset) 
	{
		byte[] pin = PASS.PBKDF2_PASS;
		for(byte i = 0; i < dkLen; i++) {
			pin[i] ^= NewXorBuffer[(byte)(Offset + i)];
		}
		
		if(0 == Util.arrayCompare(PBKDF2_PASS, (short)0, PBKDF2_defaultPIN, (short)0, dkLen)) 
		{
			default_pin = true;
		}
		else 
		{
			default_pin = false;
		}
	}
	
	public static boolean is_default() 
	{
		return default_pin;
	}
	
	public static boolean is_data_verified(byte[] data, short dOffset, short dLength, byte[] hmac, short hOffset) 
	{
		HMAC.SHA1_Init();
		HMAC.SHA1(RamBuffer, (short)0, Util.arrayCopyNonAtomic(Challenge, (short)0, RamBuffer,
											 Util.arrayCopyNonAtomic(data, dOffset, RamBuffer, (short)0, dLength), 
											 															 CHALLENGE_LEN), 
				  PBKDF2_PASS, (short)0, dkLen, 
				  bHMAC);
		HMAC.SHA_DeInit();
		
		return (0 == Util.arrayCompare(bHMAC, (short)0, hmac, hOffset, MessageDigest.LENGTH_SHA));
	}
	
	public static boolean is_verified(byte[] hmac, short offset) 
	{
		// Count the HMAC
		HMAC.SHA1_Init();
		HMAC.SHA1(Challenge, (short)0, CHALLENGE_LEN, PBKDF2_PASS, (short)0, dkLen, bHMAC);
		HMAC.SHA_DeInit();
		
		return 0 == Util.arrayCompare(bHMAC, (short)0, hmac, offset, MessageDigest.LENGTH_SHA);
	}
}
