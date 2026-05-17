using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class SystemPacket
    {
        public string Type { get; set; } // system
        public string Action { get; set; } // disconnect
        public string Message { get; set; } // optional message for the action
    }
}
