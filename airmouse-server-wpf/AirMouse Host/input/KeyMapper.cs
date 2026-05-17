using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;

namespace AirMouse_Host.input
{
    internal class KeyMapper
    {
        public void Press(string key)
        {
            Debug.WriteLine($"Key press: {key}");
        }

        public void Combo(List<string> keys)
        {
            Debug.WriteLine($"Combo: {string.Join(" + ", keys)}");
        }
    }
}
