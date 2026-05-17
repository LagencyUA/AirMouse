using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class InputPacket
    {
        public string Type { get; set; }
        public string Action { get; set; }
        public string Payload { get; set; } //json data for the action
    }
}
