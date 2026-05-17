using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class Command
    {
        public string Type { get; set; }

        public float DX { get; set; }
        public float DY { get; set; }

        public string Button { get; set; }
    }
}
