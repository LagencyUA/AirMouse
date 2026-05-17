using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class KeyPayload
    {
        public string Key { get; set; } // e.g. "A", "Enter", "Ctrl"
        public List<string> Keys { get; set; } // for multiple key presses, e.g. ["Ctrl", "C"] for copy
    }
}
