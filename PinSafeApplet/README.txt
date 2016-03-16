Here would be a applet part for PinSafe project

Description of Applet

1. AID: 

2. Commands List
Proprietary CLA = 0x80
Summary of INS = {0x10, 0x20, 0x30}
P1/P2 - depends on INS.

Service Commands (CLA = 0x10)
- GET INFO (P1 = 0x01)
CAPDU: 0x80 0x10 0x01 0x00
RADPU: 0x14 0x00 0x01 0x90 0x00
	    |    |    |    |    | 
		|    |    |    |    +---- SW LO
		|    |    |    +--------- SW HI
		|    |    +-------------- VERSION LO
		|    +------------------- VERSION HI
		+------------------------ FREE STORAGE CAPACITY
		
- GET RANDON CHALLANGE (P1 = 0x02)
CAPDU: 0x80 0x10 0x02 0x00
RAPDU: [16 RANDOM BYTES] 0x90 0x00		
		
2.2 Password Commands
- USER PASSWORD CHANGE (P1 = 0x01, P2 = 0x00)
CAPDU: 0x80 0x20 0x01 0x00 0x28 [Data1 = 20 bytes: PBKDF2(NewPass) xor PBKDF2(CurrPass)][Data 2 = 20 bytes: HMAC((Data1||Challenge), PBKDF2(CurrPass)]
RAPDU's: 
0x67 0x00 - Wrong Length
0x67 0x02 - Wrong Password


- USER PASSWORD VERIFY (P1 = 0x01, P2 = 0x01)
CAPDU: 0x80 0x20 0x01 0x00 0x14 [Data1 = 20 Bytes: HMAC(Challenge, PBKDF2(CurrPass))]

2.3 Storage Commands