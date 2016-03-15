package ps_applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.MessageDigest;

public class HMAC {
	private static MessageDigest	Sha;
	private static byte[] 			k_pad;
	private static final byte 		K_PAD_SIZE 				= (byte)  64;
	
	
	public HMAC() 
	{
		k_pad = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
	}
	
	public static void SHA1_Init() 
	{
		Sha = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
	}
	
	public static void SHA256_Init() 
	{
		Sha = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
	}
	
	public static byte SHA1(
			byte[] pText, short TextOffset, short TextLength,
			byte[] pKey,  short KeyOffset,  short KeyLength,
			byte[] pResult) 
	{
		Util.arrayFillNonAtomic(k_pad, (short)0, K_PAD_SIZE, (byte)0x36);
        for (short i = 0; i < KeyLength; i++) 
        {
        	k_pad[i] ^= pKey[(byte)(KeyOffset + i)];
        }
		Sha.reset();
		Sha.update(k_pad, (short)0, K_PAD_SIZE);
		Sha.doFinal(pText, TextOffset, TextLength, pResult, (short)0);
		
		Util.arrayFillNonAtomic(k_pad, (short)0, K_PAD_SIZE, (byte)0x5C);
		for (short i = 0; i < KeyLength; i++) 
		{
			k_pad[i] ^= pKey[(byte)(KeyOffset + i)];
        }
		Sha.reset();
		Sha.update(k_pad, (short)0, K_PAD_SIZE);
		Sha.doFinal(pResult, (short)0, MessageDigest.LENGTH_SHA, pResult, (short)0);
		return MessageDigest.LENGTH_SHA;
	}
	
	public static byte SHA256(
			byte[] pText, short TextOffset, byte TextLength,
			byte[] pKey,  short KeyOffset,  byte KeyLength,
			byte[] pResult) 
	{
		Util.arrayFillNonAtomic(k_pad, (short)0, K_PAD_SIZE, (byte)0x36);
        for (short i = 0; i < KeyLength; i++) 
        {
        	k_pad[i] ^= pKey[(byte)(KeyOffset + i)];
        }
		Sha.reset();
		Sha.update(k_pad, (short)0, K_PAD_SIZE);
		Sha.doFinal(pText, TextOffset, TextLength, pResult, (short)0);
		
		Util.arrayFillNonAtomic(k_pad, (short)0, K_PAD_SIZE, (byte)0x5C);
		for (short i = 0; i < KeyLength; i++) 
		{
			k_pad[i] ^= pKey[(byte)(KeyOffset + i)];
        }
		Sha.reset();
		Sha.update(k_pad, (short)0, K_PAD_SIZE);
		Sha.doFinal(pResult, (short)0, MessageDigest.LENGTH_SHA_256, pResult, (short)0);
		return MessageDigest.LENGTH_SHA_256;
	}
	
	public static void SHA_DeInit() 
	{
		Sha = null;
		JCSystem.requestObjectDeletion();
	}
}
