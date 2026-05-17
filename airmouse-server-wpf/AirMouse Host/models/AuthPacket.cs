using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    internal class AuthPacket
    {
        public string Type { get; set; }
        public string Pin { get; set; }
        public string DeviceName { get; set; }
    }
}
