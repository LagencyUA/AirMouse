using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class KeyPayload
    {
        public string Key { get; set; } // e.g. "A", "Enter", "Ctrl"
        public List<string> Keys { get; set; } // for multiple key presses, e.g. ["Ctrl", "C"] for copy
        public string State { get; set; } // "down", "up", "press" (only for single Key)
    }
}
