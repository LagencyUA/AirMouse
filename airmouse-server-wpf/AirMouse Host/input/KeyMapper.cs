using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;

namespace AirMouse_Host.input
{
    public class KeyMapper
    {
        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern short VkKeyScan(char ch);
        private readonly Dictionary<string, ushort> map = new();
        public KeyMapper()
        {
            InitializeStaticKeys();
            InitializeNumericKeys();
            InitializeStandardLatin();
        }

        private void InitializeStaticKeys()
        {
            // Common keys
            map.Add("ENTER", 0x0D);
            map.Add("ESC", 0x1B);
            map.Add("SPACE", 0x20);
            map.Add("TAB", 0x09);
            map.Add("BACKSPACE", 0x08);

            // Modifier keys
            map.Add("CTRL", 0x11);
            map.Add("RCTRL", 0xA3);
            map.Add("SHIFT", 0x10);
            map.Add("RSHIFT", 0xA1);
            map.Add("ALT", 0x12);
            map.Add("RALT", 0xA5);
            map.Add("WIN", 0x5B);
            map.Add("CAPSLOCK", 0x14);
            map.Add("NUMLOCK", 0x90);

            // Function keys
            for (int i = 1; i <= 12; i++)
            {
                map.Add($"F{i}", (ushort)(0x70 + i - 1));
            }

            // Arrow keys
            map.Add("LEFT", 0x25);
            map.Add("UP", 0x26);
            map.Add("RIGHT", 0x27);
            map.Add("DOWN", 0x28);

            // Other special keys
            map.Add("PRINTSCREEN", 0x9A);
            map.Add("SCROLLLOCK", 0x91);
            map.Add("PAUSE", 0x13);
            map.Add("INSERT", 0x2D);
            map.Add("DELETE", 0x2E);
            map.Add("HOME", 0x24);
            map.Add("END", 0x23);
            map.Add("PAGEUP", 0x21);
            map.Add("PAGEDOWN", 0x22);

            // media keys
            map.Add("VOLUME_MUTE", 0xAD);
            map.Add("VOLUME_DOWN", 0xAE);
            map.Add("VOLUME_UP", 0xAF);
            map.Add("MEDIA_NEXT_TRACK", 0xB0);
            map.Add("MEDIA_PREV_TRACK", 0xB1);
            map.Add("MEDIA_STOP", 0xB2);
            map.Add("MEDIA_PLAY_PAUSE", 0xB3);

        }

        private void InitializeNumericKeys()
        {
            for (int i = 0; i <= 9; i++)
            {
                map.Add(i.ToString(), (ushort)(0x30 + i));
            }
        }

        private void InitializeStandardLatin()
        {
            // A-Z мають коди від 0x41 до 0x5A
            for (char c = 'A'; c <= 'Z'; c++)
            {
                map.Add(c.ToString(), (ushort)c);
            }
        }

        public void Press(string key)
        {
            ushort vk = GetVkCode(key);
            if (vk == 0)
                return;

            Send(vk, false);
            Send(vk, true);
        }

        public void Combo(List<string> keys)
        {
            if (keys == null || keys.Count == 0)
                return;

            var modifiers = new List<ushort>();
            var regular = new List<ushort>();

            foreach (var key in keys)
            {
                ushort vk = GetVkCode(key);
                if (vk == 0)
                    continue;

                string up = key.ToUpperInvariant();
                if (up == "SHIFT" || up == "CTRL" || up == "CONTROL" || up == "ALT" || up == "WIN" || up == "RWIN" || up == "LSHIFT" || up == "RSHIFT" || up == "LCTRL" || up == "RCTRL" || up == "LALT" || up == "RALT")
                    modifiers.Add(vk);
                else
                    regular.Add(vk);
            }

            // Press modifiers down
            foreach (var m in modifiers)
                Send(m, false);

            // Press regular keys (down + up)
            foreach (var r in regular)
            {
                Send(r, false);
                Send(r, true);
            }

            // Release modifiers in reverse order
            for (int i = modifiers.Count - 1; i >= 0; i--)
                Send(modifiers[i], true);
        }

        private void Send(ushort vk, bool keyUp)
        {
            var input = new NativeInput.INPUT
            {
                type = NativeInput.INPUT_KEYBOARD,
                U = new NativeInput.INPUTUnion
                {
                    ki = new NativeInput.KEYBDINPUT
                    {
                        wVk = vk,
                        dwFlags = keyUp ? NativeInput.KEYEVENTF_KEYUP : 0
                    }
                }
            };

            NativeInput.SendInput(
                1,
                new[] { input },
                Marshal.SizeOf<NativeInput.INPUT>());
        }

        public ushort GetVkCode(string key)
        {
            if (string.IsNullOrEmpty(key)) return 0;

            string upperKey = key.ToUpper();

            if (map.TryGetValue(upperKey, out ushort staticCode))
            {
                return staticCode;
            }

            if (key.Length == 1)
            {
                char character = key[0];
                short scanResult = VkKeyScan(character);

                byte vkCode = (byte)(scanResult & 0xFF);
                if (vkCode != 0xFF)
                {
                    return vkCode;
                }
            }

            return 0; // Unknown key
        }
    }
}
