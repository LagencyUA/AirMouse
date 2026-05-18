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
            uint flag = 0;

            switch (button.ToLower())
            {
                case "left":
                    flag = state == "down"
                        ? NativeInput.MOUSEEVENTF_LEFTDOWN
                        : NativeInput.MOUSEEVENTF_LEFTUP;
                    break;

                case "right":
                    flag = state == "down"
                        ? NativeInput.MOUSEEVENTF_RIGHTDOWN
                        : NativeInput.MOUSEEVENTF_RIGHTUP;
                    break;

                case "middle":
                    flag = state == "down"
                        ? NativeInput.MOUSEEVENTF_MIDDLEDOWN
                        : NativeInput.MOUSEEVENTF_MIDDLEUP;
                    break;

                case "x1":
                    flag = state == "down"
                        ? NativeInput.MOUSEEVENTF_XDOWN
                        : NativeInput.MOUSEEVENTF_XUP;

                    SendMouse(0, 0, NativeInput.XBUTTON1, flag);
                    return;

                case "x2":
                    flag = state == "down"
                        ? NativeInput.MOUSEEVENTF_XDOWN
                        : NativeInput.MOUSEEVENTF_XUP;

                    SendMouse(0, 0, NativeInput.XBUTTON2, flag);
                    return;
            }

            SendMouse(0, 0, 0, flag);
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
