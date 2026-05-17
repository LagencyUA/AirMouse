using System;
using System.Collections.Generic;
using System.Text;

namespace AirMouse_Host.models
{
    internal class AuthResponse
    {
        public string Type { get; set; } = "auth_response";

        public bool Success { get; set; }

        public string Message { get; set; }
    }
}
