package com.bitsblender.racepad;



public class KeyCodes {

	public static final String[] VK_NAME = { "VK_LBUTTON", "VK_RBUTTON",
			"VK_CANCEL", "VK_MBUTTON", "VK_BACK", "VK_TAB", "VK_CLEAR",
			"VK_RETURN", "VK_SHIFT", "VK_CONTROL", "VK_MENU", "VK_PAUSE",
			"VK_CAPITAL", "VK_ESCAPE", "VK_SPACE", "VK_PRIOR", "VK_NEXT",
			"VK_END", "VK_HOME", "VK_LEFT", "VK_UP", "VK_RIGHT", "VK_DOWN",
			"VK_SELECT", "VK_EXECUTE", "VK_SNAPSHOT", "VK_INSERT", "VK_DELETE",
			"VK_HELP", "VK_LWIN", "VK_RWIN", "VK_APPS", "VK_NUMPAD0",
			"VK_NUMPAD1", "VK_NUMPAD2", "VK_NUMPAD3", "VK_NUMPAD4",
			"VK_NUMPAD5", "VK_NUMPAD6", "VK_NUMPAD7", "VK_NUMPAD8",
			"VK_NUMPAD9", "VK_MULTIPLY", "VK_ADD", "VK_SEPARATOR",
			"VK_SUBTRACT", "VK_DECIMAL", "VK_DIVIDE", "VK_F1", "VK_F2",
			"VK_F3", "VK_F4", "VK_F5", "VK_F6", "VK_F7", "VK_F8", "VK_F9",
			"VK_F10", "VK_F11", "VK_F12", "VK_F13", "VK_F14", "VK_F15",
			"VK_F16", "VK_F17", "VK_F18", "VK_F19", "VK_F20", "VK_F21",
			"VK_F22", "VK_F23", "VK_F24", "VK_NUMLOCK", "VK_SCROLL",
			"VK_LSHIFT", "VK_RSHIFT", "VK_LCONTROL", "VK_RCONTROL", "VK_LMENU",
			"VK_RMENU", "VK_PACKET", "VK_ATTN", "VK_CRSEL", "VK_EXSEL",
			"VK_EREOF", "VK_PLAY", "VK_ZOOM", "VK_NONAME", "VK_PA1",
			"VK_OEM_CLEAR", "VK_KEYLOCK", "VK_KEY_0", "VK_KEY_1", "VK_KEY_2",
			"VK_KEY_3", "VK_KEY_4", "VK_KEY_5", "VK_KEY_6", "VK_KEY_7",
			"VK_KEY_8", "VK_KEY_9", "VK_KEY_A", "VK_KEY_B", "VK_KEY_C",
			"VK_KEY_D", "VK_KEY_E", "VK_KEY_F", "VK_KEY_G", "VK_KEY_H",
			"VK_KEY_I", "VK_KEY_J", "VK_KEY_K", "VK_KEY_L", "VK_KEY_M",
			"VK_KEY_N", "VK_KEY_O", "VK_KEY_P", "VK_KEY_Q", "VK_KEY_R",
			"VK_KEY_S", "VK_KEY_T", "VK_KEY_U", "VK_KEY_V", "VK_KEY_W",
			"VK_KEY_X", "VK_KEY_Y", "VK_KEY_Z" };
	public static final int[] VK_CODE = { 0x01, 0x02, 0x03, 0x04, 0x08, 0x09,
			0x0C, 0x0D, 0x00, 0x00, 0x00, 0x13, 0x14, 0x1B, 0x20, 0x21, 0x22,
			0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2B, 0x2C, 0x2D, 0x2E,
			0x2F, 0x5B, 0x5C, 0x5D, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
			0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71,
			0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C,
			0x7D, 0x7E, 0x7F, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
			0x90, 0x91, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xE7, 0xF6, 0xF7,
			0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xF22, 0x30, 0x31, 0x32,
			0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44,
			0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A };

	public static String getKeyName(int code) {
		int i;
		for (i = 0; i < VK_CODE.length; i++) {
			if (code == VK_CODE[i])
				break;
		}
		return VK_NAME[i];
	}

	public static int getKeyCode(String code) {
		int i;
		for (i = 0; i < VK_NAME.length; i++) {
			if (code.equals(VK_NAME[i])) {
				break;
			}
		}
		return VK_CODE[i];
	}
	public static String getKeyNormalName(int code) {
		String codeName = getKeyName(code);
		String checkString = new String("VK_KEY_");
		if(codeName.startsWith(checkString)){
			
			return codeName.substring(checkString.length());
		}
		else{
			return codeName.substring(3,codeName.length());
		}
	}
}
