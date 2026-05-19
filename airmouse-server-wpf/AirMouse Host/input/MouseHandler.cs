using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Runtime.InteropServices;
using System.Threading.Tasks;

namespace AirMouse_Host.input
{
    internal class MouseHandler
    {
        //private DateTime? lastMoveTime;
        private double residualX = 0.0;
        private double residualY = 0.0;
        private readonly object smoothingLock = new();

        public int SmoothingIntensity { get; set; } = 25;

        public void Move(int dx, int dy)
        {
            if (dx == 0 && dy == 0)
                return;

            if (SmoothingIntensity > 1)
            {
                lock (smoothingLock)
                {
                    int steps = Math.Max(1, SmoothingIntensity);

                    // include residuals from previous rounding
                    double totalX = dx + residualX;
                    double totalY = dy + residualY;

                    double incX = totalX / steps;
                    double incY = totalY / steps;

                    double accX = 0.0;
                    double accY = 0.0;
                    int prevRoundedX = 0;
                    int prevRoundedY = 0;

                    for (int i = 0; i < steps; i++)
                    {
                        accX += incX;
                        accY += incY;

                        int roundedX = (int)Math.Round(accX);
                        int roundedY = (int)Math.Round(accY);

                        int stepDx = roundedX - prevRoundedX;
                        int stepDy = roundedY - prevRoundedY;

                        if (stepDx != 0 || stepDy != 0)
                            SendMouse(stepDx, stepDy, 0, NativeInput.MOUSEEVENTF_MOVE);

                        prevRoundedX = roundedX;
                        prevRoundedY = roundedY;
                    }

                    // store residual for next packet
                    residualX = totalX - prevRoundedX;
                    residualY = totalY - prevRoundedY;
                }

                return;
            }

            // not applying smoothing -> clear residuals
            residualX = 0.0;
            residualY = 0.0;
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
