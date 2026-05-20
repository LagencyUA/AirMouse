using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class KeyboardTextPacket
    {
        public string Type { get; set; } = "keyboard_text";
        public string Text { get; set; }
    }
}
