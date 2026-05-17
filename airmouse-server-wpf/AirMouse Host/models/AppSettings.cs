using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    public class AppSettings
    {
        public bool UseStaticPin { get; set; } = false;
        public string StaticPin { get; set; } = "";
    }
}
