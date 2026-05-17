using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;

namespace AirMouse_Host.input
{
    internal class MouseHandler
    {
        public void Move(int dx, int dy)
        {
            Debug.WriteLine($"Mouse move: {dx}, {dy}");
        }

        public void Click(string button, string state)
        {
            Debug.WriteLine($"Mouse {button}: {state}");
        }

        public void Scroll(int delta)
        {
            Debug.WriteLine($"Mouse scroll: {delta}");
        }
    }
}
