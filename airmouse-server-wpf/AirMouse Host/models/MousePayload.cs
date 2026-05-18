using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class MousePayload
    {
        public int? DX { get; set; }
        public int? DY { get; set; }

        public string Button { get; set; } // "left", "right", "middle"
        public string State { get; set; } // "down", "up"

        public int? Scroll { get; set; } // positive for scroll up, negative for scroll down
    }
}
