using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Runtime.InteropServices;

namespace AirMouse_Host.input
{
    internal class MouseHandler
    {
        public void Move(int dx, int dy)
        {
            SendMouse(dx, dy, 0, NativeInput.MOUSEEVENTF_MOVE);
        }

        public void Click(string button, string state)
        {
            if (button == null)
                return;

            string btn = button.ToLowerInvariant();
            string st = (state ?? string.Empty).ToLowerInvariant();

            // Helper to send down/up or click
            void SendPair(uint downFlag, uint upFlag, uint data = 0)
            {
                if (st == "down")
                {
                    SendMouse(0, 0, data, downFlag);
                }
                else if (st == "up")
                {
                    SendMouse(0, 0, data, upFlag);
                }
                else // click or unknown -> perform click
                {
                    SendMouse(0, 0, data, downFlag);
                    SendMouse(0, 0, data, upFlag);
                }
            }

            switch (btn)
            {
                case "left":
                    SendPair(NativeInput.MOUSEEVENTF_LEFTDOWN, NativeInput.MOUSEEVENTF_LEFTUP);
                    break;

                case "right":
                    SendPair(NativeInput.MOUSEEVENTF_RIGHTDOWN, NativeInput.MOUSEEVENTF_RIGHTUP);
                    break;

                case "middle":
                    SendPair(NativeInput.MOUSEEVENTF_MIDDLEDOWN, NativeInput.MOUSEEVENTF_MIDDLEUP);
                    break;

                case "x1":
                    SendPair(NativeInput.MOUSEEVENTF_XDOWN, NativeInput.MOUSEEVENTF_XUP, NativeInput.XBUTTON1);
                    break;

                case "x2":
                    SendPair(NativeInput.MOUSEEVENTF_XDOWN, NativeInput.MOUSEEVENTF_XUP, NativeInput.XBUTTON2);
                    break;
            }
        }

        public void Scroll(int delta)
        {
            SendMouse(0, 0, (uint)delta, NativeInput.MOUSEEVENTF_WHEEL);
        }

        private void SendMouse(int dx, int dy, uint data, uint flags)
        {
            var input = new NativeInput.INPUT
            {
                type = NativeInput.INPUT_MOUSE,
                U = new NativeInput.INPUTUnion
                {
                    mi = new NativeInput.MOUSEINPUT
                    {
                        dx = dx,
                        dy = dy,
                        mouseData = data,
                        dwFlags = flags
                    }
                }
            };

            NativeInput.SendInput(
                1,
                new[] { input },
                Marshal.SizeOf<NativeInput.INPUT>());
        }
    }
}
