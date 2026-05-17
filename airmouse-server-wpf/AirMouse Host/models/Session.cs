using System;
using System.Net.Sockets;

namespace AirMouse_Host.models
{
    public class Session
    {
        public TcpClient Client { get; set; }

        public string DeviceName { get; set; }

        public DateTime ConnectedAt { get; set; }
    }
}